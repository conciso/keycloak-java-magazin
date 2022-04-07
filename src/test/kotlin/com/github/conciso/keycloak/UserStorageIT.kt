package com.github.conciso.keycloak

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.keycloak.admin.client.Keycloak
import org.keycloak.common.util.MultivaluedHashMap
import org.keycloak.jose.jws.JWSInput
import org.keycloak.representations.AccessToken
import org.keycloak.representations.idm.ComponentRepresentation
import org.keycloak.representations.info.ProviderRepresentation
import org.keycloak.storage.UserStorageProvider
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.model.RequestDefinition
import org.mockserver.verify.VerificationTimes
import org.mockserver.verify.VerificationTimes.once
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import javax.ws.rs.core.Response


@Testcontainers
class UserStorageIT {

    @Test
    @DisplayName("User Storage Provider can be registerd")
    fun canRegisterSPI() {
        val providers = KEYCLOAK_CONTAINER.storageProviderInfo()
        assertThat(providers)
            .containsKey("custom-provider")
    }

    @Test
    @DisplayName("Provides information on server info page")
    fun providesInfo() {
        val providers = KEYCLOAK_CONTAINER.storageProviderInfo()
        assertThat(providers["custom-provider"]?.operationalInfo)
            .containsKey("version")
    }

    @Test
    @DisplayName("Can login user")
    fun canLoginUser() {

        val username = "janedoe"
        val userId = "0dc586be-c5b0-468a-a9a8-4a5f3649c74e"
        val expectedRequest = mockUserLookup(username, userId)

        val accessToken = login("janedoe", "test")

        assertThat(accessToken.subject)
            .isEqualTo("f:$USER_STORAGE_COMPONENT_ID:$userId")
        verify(expectedRequest, once())
    }

    companion object {

        @JvmStatic
        private val NETWORK = Network.newNetwork()

        @Container
        @JvmStatic
        private val KEYCLOAK_CONTAINER = KeycloakContainer("quay.io/keycloak/keycloak:17.0.1")
            .withProviderClassesFrom("target/classes")
            .withProviderLibsFrom(listOf(
                File("target/dependency/kotlin-stdlib.jar"),
            ))
            .withNetwork(NETWORK)

        @Container
        @JvmStatic
        private val MOCK_SERVER_CONTAINER = MockServerContainer("5.13.0")
            .withCommand("-serverPort 80")
            .withExposedPorts(80)
            .withNetwork(NETWORK)
            .withNetworkAliases("mockserver")

        private lateinit var MOCK_SERVER_CLIENT: MockServerClient
        private fun `when`(expectedRequest: RequestDefinition) =
            MOCK_SERVER_CLIENT.`when`(expectedRequest)

        private fun verify(expectedRequest: RequestDefinition, times: VerificationTimes) =
            MOCK_SERVER_CLIENT.verify(expectedRequest, times)

        private fun login(username: String, password: String): AccessToken {
            val response = Keycloak
                .getInstance(
                    KEYCLOAK_CONTAINER.authServerUrl,
                    "master",
                    username,
                    password,
                    "admin-cli"
                )
                .tokenManager()
                .grantToken()
            return JWSInput(response.token).readJsonContent(AccessToken::class.java)
        }

        private fun mockUserLookup(username: String, userId: String): HttpRequest {
            val expectedRequest = request()
                .withPath("/users")
                .withQueryStringParameter("username", username)

            `when`(expectedRequest)
                .respond(
                    HttpResponse.response()
                        .withBody(
                            """
                            {
                              "id": "$userId",
                              "username": "$username"
                            }
                            """.trimIndent()
                        )
                        .withContentType(MediaType.JSON_UTF_8)
                )
            return expectedRequest
        }

        @BeforeAll
        @JvmStatic
        private fun setUp() {
            MOCK_SERVER_CLIENT = MockServerClient(
                MOCK_SERVER_CONTAINER.host,
                MOCK_SERVER_CONTAINER.getMappedPort(80)
            )
            addUserFederation()
        }

        @AfterAll
        @JvmStatic
        private fun tearDown() {
            NETWORK.close()
        }

        private lateinit var USER_STORAGE_COMPONENT_ID: String

        private fun addUserFederation() {
            val userStorageComponent = ComponentRepresentation()
            userStorageComponent.name = "REST"
            userStorageComponent.providerType = UserStorageProvider::class.java.getName()
            userStorageComponent.providerId = "custom-provider"
            val config = MultivaluedHashMap<String, String>()
            config.putSingle("baseUrl", "http://mockserver")
            userStorageComponent.config = config
            val response = KEYCLOAK_CONTAINER
                .keycloakAdminClient
                .realm("master")
                .components()
                .add(userStorageComponent)

            assertThat(response.statusInfo.family)
                .isEqualTo(Response.Status.Family.SUCCESSFUL)

            val location = response.getHeaderString("Location")
            val lastIndex: Int = location.lastIndexOf("/")
            USER_STORAGE_COMPONENT_ID = location.substring(lastIndex + 1)
        }

        private fun KeycloakContainer.storageProviderInfo(): MutableMap<String, ProviderRepresentation> =
            this.keycloakAdminClient
                .serverInfo()
                .info
                .providers["storage"]
                ?.providers
                ?: mutableMapOf()
    }
}
