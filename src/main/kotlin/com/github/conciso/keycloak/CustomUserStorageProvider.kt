package com.github.conciso.keycloak

import com.github.conciso.keycloak.api.UserRepresentation
import org.apache.http.impl.client.CloseableHttpClient
import org.keycloak.broker.provider.util.SimpleHttp
import org.keycloak.component.ComponentModel
import org.keycloak.credential.CredentialInput
import org.keycloak.credential.CredentialInputValidator
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.models.credential.PasswordCredentialModel
import org.keycloak.storage.StorageId
import org.keycloak.storage.UserStorageProvider
import org.keycloak.storage.adapter.InMemoryUserAdapter
import org.keycloak.storage.user.UserLookupProvider

class CustomUserStorageProvider(
    private val session: KeycloakSession,
    private val componentModel: ComponentModel,
    private val client: CloseableHttpClient
) : UserStorageProvider, UserLookupProvider, CredentialInputValidator {

    override fun close() {}

    override fun getUserById(realm: RealmModel, id: String): UserModel? = null

    override fun getUserById(id: String, realmModel: RealmModel) = getUserById(realmModel, id)

    override fun getUserByUsername(realm: RealmModel, username: String): UserModel? {
        val baseUrl = componentModel.config.getFirst("baseUrl")
        val response = SimpleHttp.doGet("$baseUrl/users", client)
            .param("username", username)
            .acceptJson()
            .asResponse()
        if (response.status != 200) return null
        val user: UserRepresentation =
            response.asJson(UserRepresentation::class.java)
        val keycloakUser = InMemoryUserAdapter(session, realm, createStorageId(user))
        keycloakUser.username = username
        keycloakUser.isEnabled = true
        keycloakUser.addDefaults()
        return keycloakUser
    }

    private fun createStorageId(user: UserRepresentation) = StorageId(componentModel.id, user.id).id

    override fun getUserByUsername(username: String, realmModel: RealmModel) = getUserByUsername(realmModel, username)

    override fun getUserByEmail(realm: RealmModel, email: String): UserModel? = null

    override fun getUserByEmail(email: String, realmModel: RealmModel) = getUserByEmail(realmModel, email)

    override fun supportsCredentialType(type: String) = PasswordCredentialModel.TYPE == type

    override fun isConfiguredFor(realmModel: RealmModel, userModel: UserModel, credentialType: String) =
        supportsCredentialType(credentialType)

    override fun isValid(realmModel: RealmModel, userModel: UserModel, credentialInput: CredentialInput) =
        "test" == credentialInput.challengeResponse

}

