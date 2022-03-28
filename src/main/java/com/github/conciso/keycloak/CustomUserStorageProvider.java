package com.github.conciso.keycloak;

import com.github.conciso.keycloak.api.UserRepresentation;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

import java.io.IOException;

public class CustomUserStorageProvider
    implements UserStorageProvider,
    UserLookupProvider, CredentialInputValidator {

    private final KeycloakSession session;
    private final CloseableHttpClient client;
    private final ComponentModel componentModel;

    public CustomUserStorageProvider(KeycloakSession session, ComponentModel componentModel, CloseableHttpClient client) {
        this.session = session;
        this.client = client;
        this.componentModel = componentModel;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        return null;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        return getUserById(realmModel, id);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        try {
            SimpleHttp.Response response = SimpleHttp.doGet("http://mockserver/users", client)
                .param("username", username)
                .acceptJson()
                .asResponse();

            if (response.getStatus() != 200) return null;
            UserRepresentation user = response.asJson(UserRepresentation.class);

            InMemoryUserAdapter keycloakUser = new InMemoryUserAdapter(session, realm, createStorageId(user));
            keycloakUser.setUsername(username);
            keycloakUser.setEnabled(true);
            keycloakUser.addDefaults();
            return keycloakUser;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String createStorageId(UserRepresentation user) {
        return new StorageId(componentModel.getId(), user.getId()).getId();
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        return getUserByUsername(realmModel, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        return getUserByEmail(realmModel, email);
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return PasswordCredentialModel.TYPE.equalsIgnoreCase(type);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        return "test".equals(credentialInput.getChallengeResponse());
    }
}
