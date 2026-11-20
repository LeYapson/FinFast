package fr.mobilune.componentgallery.data

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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val PUSH_BASE_URL = "https://simpleapi.mobilune.fr/push"

/** Réponse `202` de `POST /push/test-token` : nombre de notifications envoyées / en échec. */
data class PushSendResult(val sent: Int, val failed: Int)

/** Un appareil enregistré pour l'utilisateur connecté (`GET /push/devices`). */
data class PushDevice(val token: String, val platform: String, val createdAt: String)

/** Appels HTTP du serveur de notifications. Tous demandent une session active (Bearer). */
interface PushApi {
    /** `POST /push/devices` : enregistre le jeton FCM pour l'utilisateur connecté (un jeton déjà connu change de propriétaire). */
    suspend fun registerDevice(token: String, platform: String)

    /** `GET /push/devices` : appareils enregistrés par l'utilisateur connecté. */
    suspend fun listDevices(): List<PushDevice>

    /** `POST /push/test-token` : envoie une notification de test à cet appareil. */
    suspend fun sendTestPush(deviceToken: String): PushSendResult

    /** `POST /push/send` : notifie tous les appareils de l'utilisateur [userId] (soi-même ou un autre, par exemple son binôme). */
    suspend fun sendPush(userId: String, title: String, body: String, data: Map<String, String>): PushSendResult
}

class PushApiImpl(
    private val httpClient: HttpClient,
    private val authSession: AuthSession,
) : PushApi {
    override suspend fun registerDevice(token: String, platform: String) {
        val response = httpClient.post("$PUSH_BASE_URL/devices") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearer()
            setBody(
                buildJsonObject {
                    put("token", token)
                    put("platform", platform)
                }.toString(),
            )
        }
        response.rejectExpiredSession()
        when (val status = response.status.value) {
            in 200..299 -> Unit
            400 -> throw IllegalStateException("Le serveur a refusé l'enregistrement de l'appareil (données invalides).")
            else -> throw IllegalStateException("L'enregistrement de l'appareil a échoué (HTTP $status).")
        }
    }

    override suspend fun listDevices(): List<PushDevice> {
        val response = httpClient.get("$PUSH_BASE_URL/devices") {
            accept(ContentType.Application.Json)
            bearer()
        }
        response.rejectExpiredSession()
        val status = response.status.value
        if (status != 200) {
            throw IllegalStateException("Impossible de récupérer les appareils enregistrés (HTTP $status).")
        }
        return Json.parseToJsonElement(response.bodyAsText()).jsonArray.map { element ->
            val device = element.jsonObject
            PushDevice(
                token = device["token"]?.jsonPrimitive?.content.orEmpty(),
                platform = device["platform"]?.jsonPrimitive?.content.orEmpty(),
                createdAt = device["createdAt"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
    }

    override suspend fun sendTestPush(deviceToken: String): PushSendResult {
        val response = httpClient.post("$PUSH_BASE_URL/test-token") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearer()
            setBody(buildJsonObject { put("token", deviceToken) }.toString())
        }
        response.rejectExpiredSession()
        return response.toSendResult(badRequest = "Le jeton de notification de cet appareil est invalide.")
    }

    override suspend fun sendPush(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): PushSendResult {
        val response = httpClient.post("$PUSH_BASE_URL/send") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearer()
            setBody(
                buildJsonObject {
                    put("userId", userId)
                    put("title", title)
                    put("body", body)
                    put("data", buildJsonObject { data.forEach { (key, value) -> put(key, value) } })
                }.toString(),
            )
        }
        response.rejectExpiredSession()
        return response.toSendResult(badRequest = "Requête refusée : vérifie le destinataire, le titre et le message.")
    }

    /** 2xx : lit `sent` / `failed` ; sinon traduit l'erreur du serveur en message lisible. */
    private suspend fun HttpResponse.toSendResult(badRequest: String): PushSendResult =
        when (val code = status.value) {
            in 200..299 -> {
                val json = runCatching { Json.parseToJsonElement(bodyAsText()).jsonObject }.getOrNull()
                PushSendResult(
                    sent = json?.get("sent")?.jsonPrimitive?.intOrNull ?: 0,
                    failed = json?.get("failed")?.jsonPrimitive?.intOrNull ?: 0,
                )
            }
            400 -> throw IllegalStateException(badRequest)
            404 -> throw IllegalStateException("Utilisateur introuvable.")
            429 -> throw IllegalStateException("Trop d'envois (30 par minute maximum). Réessaie dans un instant.")
            502 -> throw IllegalStateException("Firebase a refusé les identifiants du serveur.")
            503 -> throw IllegalStateException("L'envoi de notifications n'est pas configuré sur le serveur.")
            else -> throw IllegalStateException("L'envoi a échoué (HTTP $code).")
        }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer() {
        val accessToken = authSession.accessToken
            ?: throw IllegalStateException("Aucune session active.")
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    /** 401/403 : le serveur ne reconnaît plus la session ; l'UI est renvoyée vers la connexion. */
    private suspend fun HttpResponse.rejectExpiredSession() {
        if (status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
    }
}
