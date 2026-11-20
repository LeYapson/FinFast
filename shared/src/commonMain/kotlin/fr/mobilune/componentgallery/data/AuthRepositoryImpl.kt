package fr.mobilune.componentgallery.data

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val AUTH_BASE_URL = "https://simpleapi.mobilune.fr/auth"
private const val API_BASE_URL = "https://simpleapi.mobilune.fr"

/** 401 (jeton absent/expiré) ou 403 (jeton refusé) : la session n'est plus valable. */
internal fun Int.isSessionRejection(): Boolean = this == 401 || this == 403

class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val authSession: AuthSession,
) : AuthRepository {
    override suspend fun register(userId: String, code: String, displayedName: String) {
        val response = httpClient.post("$AUTH_BASE_URL/register") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("userId", userId)
                    put("code", code)
                    put("displayedName", displayedName)
                }.toString(),
            )
        }

        when (response.status.value) {
            201 -> Unit
            409 -> throw IllegalStateException(
                "Cette carte ou ce nom est déjà associé à un compte. Essaie de te connecter.",
            )
            400 -> throw IllegalStateException("Les informations fournies ne sont pas valides.")
            else -> throw IllegalStateException(
                "La création du compte a échoué (HTTP ${response.status.value}).",
            )
        }

    }

    override suspend fun login(userId: String, code: String) {
        val response = httpClient.post("$AUTH_BASE_URL/login") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("userId", userId)
                    put("code", code)
                }.toString(),
            )
        }

        when (response.status.value) {
            200 -> {
                val token = kotlinx.serialization.json.Json
                    .parseToJsonElement(response.bodyAsText())
                    .jsonObject
                    .getValue("accessToken")
                    .jsonPrimitive
                    .content
                authSession.setAccessToken(token)
                getCurrentUser()
            }
            401 -> throw IllegalStateException("Nom ou code incorrect.")
            else -> throw IllegalStateException(
                "La connexion a échoué (HTTP ${response.status.value}).",
            )
        }
    }

    override suspend fun getCurrentUser(): UserProfile {
        val token = authSession.accessToken
            ?: throw IllegalStateException("Aucune session active.")
        val response = httpClient.get("$AUTH_BASE_URL/me") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
        if (response.status.value != 200) {
            throw IllegalStateException(
                "Impossible de récupérer le profil (HTTP ${response.status.value}).",
            )
        }

        val profile = kotlinx.serialization.json.Json
            .parseToJsonElement(response.bodyAsText())
            .jsonObject
        val userProfile = UserProfile(
            userId = profile.getValue("userId").jsonPrimitive.content,
            displayedName = profile.getValue("displayedName").jsonPrimitive.content,
            balanceCents = profile.getValue("solde_centimes").jsonPrimitive.content.toLong(),
        )
        authSession.setDisplayedName(userProfile.displayedName)
        return userProfile
    }

    override suspend fun logout() {
        val token = authSession.accessToken
            ?: throw IllegalStateException("Aucune session active.")
        val response = httpClient.post("$AUTH_BASE_URL/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        // Jeton déjà expiré côté serveur : l'utilisateur est de toute façon déconnecté.
        if (response.status.value != 204 && !response.status.value.isSessionRejection()) {
            throw IllegalStateException(
                "La déconnexion a échoué (HTTP ${response.status.value}).",
            )
        }
        authSession.clear()
    }

    override suspend fun sendMoney(userId: String, amountCents: Long) {
        postMoney("/money/send", userId, amountCents)
    }

    override suspend fun receiveMoney(userId: String, amountCents: Long) {
        postMoney("/money/receive", userId, amountCents)
    }

    private suspend fun postMoney(path: String, userId: String, amountCents: Long) {
        val token = authSession.accessToken
            ?: throw IllegalStateException("Aucune session active.")
        val response = httpClient.post("$API_BASE_URL$path") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(
                buildJsonObject {
                    put("userId", userId)
                    put("montant_centimes", amountCents)
                }.toString(),
            )
        }
        if (response.status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException(
                "Le transfert a échoué (HTTP ${response.status.value}). Vérifie le montant et la carte, puis réessaie.",
            )
        }
    }
}
