package fr.mobilune.componentgallery.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

/** Adresse du serveur : toutes les APIs de ce module partent de là. */
internal const val API_BASE_URL = "https://simpleapi.mobilune.fr"

private const val REQUEST_TIMEOUT_MILLIS = 15_000L

/** Le client Ktor de l'application : un seul pour toutes les APIs, avec des délais d'attente communs. */
fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}
