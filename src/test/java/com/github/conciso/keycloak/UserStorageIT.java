package com.github.conciso.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
public class UserStorageIT {

    private static final Network NETWORK = Network.newNetwork();

    private static String USER_STORAGE_COMPONENT_ID;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER =
        new KeycloakContainer("quay.io/keycloak/keycloak:17.0.1")
            .withProviderClassesFrom("target/classes")
            .withNetwork(NETWORK);

    @Container
    private static final MockServerContainer MOCK_SERVER_CONTAINER =
        new MockServerContainer("5.13.0")
            .withCommand("-serverPort 80")
            .withExposedPorts(80)
            .withNetwork(NETWORK)
            .withNetworkAliases("mockserver");

    private static MockServerClient mockServerClient;

    @BeforeAll
    static void setUp() {
        mockServerClient = new MockServerClient(
            MOCK_SERVER_CONTAINER.getHost(),
            MOCK_SERVER_CONTAINER.getMappedPort(80));
        addUserFederation();
    }

    @AfterAll
    static void tearDown() {
        NETWORK.close();
    }

    private static void addUserFederation() {
        ComponentRepresentation userStorageComponent = new ComponentRepresentation();
        userStorageComponent.setName("REST");
        userStorageComponent.setProviderType(UserStorageProvider.class.getName());
        userStorageComponent.setProviderId("custom-provider");
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("baseUrl", "http://mockserver");
        userStorageComponent.setConfig(config);
        Response response = KEYCLOAK_CONTAINER.getKeycloakAdminClient()
            .realm("master")
            .components()
            .add(userStorageComponent);
        assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
        String location = response.getHeaderString("Location");
        int lastIndex = location.lastIndexOf("/");
        USER_STORAGE_COMPONENT_ID = location.substring(lastIndex + 1);
    }

    @Test
    @DisplayName("User Storage Provider can be registerd")
    void canRegisterSPI() {
        Map<String, ProviderRepresentation> providers = getStorageProviderInfo();
        assertThat(providers).containsKey("custom-provider");
    }

    @Test
    @DisplayName("Provides information on server info page")
    void providesInfo() {
        Map<String, ProviderRepresentation> providers = getStorageProviderInfo();
        assertThat(providers.get("custom-provider").getOperationalInfo()).containsKey("version");
    }

    private static Map<String, ProviderRepresentation> getStorageProviderInfo() {
        return KEYCLOAK_CONTAINER.getKeycloakAdminClient()
            .serverInfo()
            .getInfo()
            .getProviders()
            .get("storage")
            .getProviders();
    }

    @Test
    @DisplayName("Can login user")
    void canLoginUser() throws JWSInputException {
        mockServerClient
            .when(request()
                .withPath("/users")
                .withQueryStringParameter("username", "janedoe")
            )
            .respond(response()
                .withBody("{\"id\":\"0dc586be-c5b0-468a-a9a8-4a5f3649c74e\", \"username\":\"janedoe\"}")
                .withContentType(MediaType.JSON_UTF_8)
            );

        AccessTokenResponse response = Keycloak
            .getInstance(KEYCLOAK_CONTAINER.getAuthServerUrl(), "master","janedoe", "test", "admin-cli")
            .tokenManager()
            .grantToken();

        AccessToken accessToken = new JWSInput(response.getToken()).readJsonContent(AccessToken.class);
        assertThat(accessToken.getSubject()).isEqualTo("f:" + USER_STORAGE_COMPONENT_ID + ":0dc586be-c5b0-468a-a9a8-4a5f3649c74e");
    }

}
