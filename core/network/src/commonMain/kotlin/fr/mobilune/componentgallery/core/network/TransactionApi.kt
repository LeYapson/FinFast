package fr.mobilune.componentgallery.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Une transaction telle que la renvoie `GET /transactions`. [id] peut être absent côté serveur. */
data class TransactionDto(
    val id: String?,
    val merchant: String,
    val isoDate: String,
    val amountCents: Long,
)

interface TransactionApi {
    /** `GET /transactions` : les transactions de l'utilisateur connecté, dans l'ordre du serveur. */
    suspend fun fetchTransactions(): List<TransactionDto>
}

class TransactionApiImpl(
    private val httpClient: HttpClient,
    private val authSession: AuthSession,
) : TransactionApi {
    override suspend fun fetchTransactions(): List<TransactionDto> {
        val response = httpClient.get("$API_BASE_URL/transactions") {
            authSession.accessToken?.let(::bearerAuth)
        }
        if (response.status.value.isSessionRejection()) {
            authSession.expire()
            throw SessionExpiredException()
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Erreur HTTP ${response.status.value} lors du chargement des transactions.")
        }

        return Json.parseToJsonElement(response.bodyAsText())
            .jsonArray
            .map { element ->
                val transaction = element.jsonObject
                TransactionDto(
                    id = transaction["id"]?.jsonPrimitive?.content?.takeUnless { it == "null" },
                    merchant = transaction.getValue("marchand").jsonPrimitive.content,
                    isoDate = transaction.getValue("date").jsonPrimitive.content,
                    amountCents = transaction.getValue("montant_centimes").jsonPrimitive.content.toLong(),
                )
            }
    }
}
