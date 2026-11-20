package fr.mobilune.componentgallery.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Profil de l'utilisateur connecté (`GET /auth/me`). */
data class CurrentUserDto(
    val userId: String,
    val displayedName: String,
    val balanceCents: Long,
)

/** Authentification et transferts d'argent : les routes `/auth/...` et `/money/...` du serveur. */
interface AuthApi {
    /** `POST /auth/register`. */
    suspend fun register(userId: String, code: String, displayedName: String)

    /** `POST /auth/login` : renvoie le jeton d'accès. Le mémoriser est le travail de l'appelant. */
    suspend fun login(userId: String, code: String): String

    /** `GET /auth/me`. */
    suspend fun currentUser(): CurrentUserDto

    /** `POST /auth/logout` : un jeton déjà expiré côté serveur n'est pas une erreur. */
    suspend fun logout()

    /** `POST /money/send`. */
    suspend fun sendMoney(userId: String, amountCents: Long)

    /** `POST /money/receive`. */
    suspend fun receiveMoney(userId: String, amountCents: Long)
}

class AuthApiImpl(
    private val httpClient: HttpClient,
    private val authSession: AuthSession,
) : AuthApi {
    override suspend fun register(userId: String, code: String, displayedName: String) {
        val response = httpClient.post("$API_BASE_URL/auth/register") {
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

    override suspend fun login(userId: String, code: String): String {
        val response = httpClient.post("$API_BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("userId", userId)
                    put("code", code)
                }.toString(),
            )
        }

        return when (response.status.value) {
            200 -> Json.parseToJsonElement(response.bodyAsText())
                .jsonObject
                .getValue("accessToken")
                .jsonPrimitive
                .content
            401 -> throw IllegalStateException("Nom ou code incorrect.")
            else -> throw IllegalStateException(
                "La connexion a échoué (HTTP ${response.status.value}).",
            )
        }
    }

    override suspend fun currentUser(): CurrentUserDto {
        val token = requireAccessToken()
        val response = httpClient.get("$API_BASE_URL/auth/me") {
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        response.rejectExpiredSession()
        if (response.status.value != 200) {
            throw IllegalStateException(
                "Impossible de récupérer le profil (HTTP ${response.status.value}).",
            )
        }

        val profile = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return CurrentUserDto(
            userId = profile.getValue("userId").jsonPrimitive.content,
            displayedName = profile.getValue("displayedName").jsonPrimitive.content,
            balanceCents = profile.getValue("solde_centimes").jsonPrimitive.content.toLong(),
        )
    }

    override suspend fun logout() {
        val token = requireAccessToken()
        val response = httpClient.post("$API_BASE_URL/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        // Jeton déjà expiré côté serveur : l'utilisateur est de toute façon déconnecté.
        if (response.status.value != 204 && !response.status.value.isSessionRejection()) {
            throw IllegalStateException(
                "La déconnexion a échoué (HTTP ${response.status.value}).",
            )
        }
    }

    override suspend fun sendMoney(userId: String, amountCents: Long) {
        postMoney("/money/send", userId, amountCents)
    }

    override suspend fun receiveMoney(userId: String, amountCents: Long) {
        postMoney("/money/receive", userId, amountCents)
    }

    private suspend fun postMoney(path: String, userId: String, amountCents: Long) {
        val token = requireAccessToken()
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
        response.rejectExpiredSession()
        if (response.status.value !in 200..299) {
            throw IllegalStateException(
                "Le transfert a échoué (HTTP ${response.status.value}). Vérifie le montant et la carte, puis réessaie.",
            )
        }
    }

    private fun requireAccessToken(): String =
        authSession.accessToken ?: throw IllegalStateException("Aucune session active.")

    /** 401/403 : le serveur ne reconnaît plus la session ; l'UI est renvoyée vers la connexion. */
    private suspend fun HttpResponse.rejectExpiredSession() {
        if (status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
    }
}
