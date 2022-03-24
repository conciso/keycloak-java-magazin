package com.github.conciso.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.assertj.core.internal.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.info.ProviderRepresentation;
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
        Map<String, ProviderRepresentation> providers = CONTAINER.getKeycloakAdminClient()
            .serverInfo()
            .getInfo()
            .getProviders()
            .get("storage")
            .getProviders();
        assertThat(providers).containsKey("custom-provider");
    }

}
