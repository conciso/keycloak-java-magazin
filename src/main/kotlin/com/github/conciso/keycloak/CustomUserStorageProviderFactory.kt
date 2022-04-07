package com.github.conciso.keycloak

import org.keycloak.component.ComponentModel
import org.keycloak.connections.httpclient.HttpClientProvider
import org.keycloak.models.KeycloakSession
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.provider.ProviderConfigurationBuilder
import org.keycloak.provider.ServerInfoAwareProviderFactory
import org.keycloak.storage.UserStorageProviderFactory

class CustomUserStorageProviderFactory :
    UserStorageProviderFactory<CustomUserStorageProvider>,
    ServerInfoAwareProviderFactory {

    override fun create(keycloakSession: KeycloakSession, componentModel: ComponentModel): CustomUserStorageProvider {
        val httpClientProvider = keycloakSession.getProvider(HttpClientProvider::class.java)
        return CustomUserStorageProvider(keycloakSession, componentModel, httpClientProvider.httpClient)
    }

    override fun getId() = "custom-provider"

    override fun getOperationalInfo(): Map<String, String> {
        val version = this.javaClass.getPackage().implementationVersion ?: "undefined"
        return mapOf("version" to version)
    }

    override fun getConfigProperties() = ProviderConfigurationBuilder.create()
        .property(
            ProviderConfigProperty(
                "baseUrl",  // Name
                "Basis URL",  // Label
                "Die Basis URL des externen Benutzerverzeichnisses",  // Hilfe-Text
                ProviderConfigProperty.STRING_TYPE,  // Typ
                "http://mockserver",  // Standard-Wert
                false // Secret/Passwort-Feld
            )
        )
        .build()
}
