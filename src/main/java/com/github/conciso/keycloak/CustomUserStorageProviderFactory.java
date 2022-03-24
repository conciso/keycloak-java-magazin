package com.github.conciso.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class CustomUserStorageProviderFactory
    implements UserStorageProviderFactory<CustomUserStorageProvider> {

    @Override
    public CustomUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new CustomUserStorageProvider();
    }

    @Override
    public String getId() {
        return "custom-provider";
    }
}
