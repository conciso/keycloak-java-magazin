package com.github.conciso.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.Map;

public class CustomUserStorageProviderFactory implements
    UserStorageProviderFactory<CustomUserStorageProvider>,
    ServerInfoAwareProviderFactory {

    @Override
    public CustomUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        HttpClientProvider httpClientProvider = keycloakSession.getProvider(HttpClientProvider.class);
        return new CustomUserStorageProvider(keycloakSession, componentModel, httpClientProvider.getHttpClient());
    }

    @Override
    public String getId() {
        return "custom-provider";
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "undefined";
        }
        return Map.of("version", version);
    }

}
