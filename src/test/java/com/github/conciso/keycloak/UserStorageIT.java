package com.github.conciso.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.info.ProviderRepresentation;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class UserStorageIT {

    @Container
    private static final KeycloakContainer CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:17.0.1")
        .withProviderClassesFrom("target/classes");

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
        return CONTAINER.getKeycloakAdminClient()
            .serverInfo()
            .getInfo()
            .getProviders()
            .get("storage")
            .getProviders();
    }

}
