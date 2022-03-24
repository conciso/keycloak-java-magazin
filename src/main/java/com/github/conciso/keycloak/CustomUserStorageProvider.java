package com.github.conciso.keycloak;

import org.keycloak.storage.UserStorageProvider;

public class CustomUserStorageProvider implements UserStorageProvider {

    @Override
    public void close() {
    }

}
