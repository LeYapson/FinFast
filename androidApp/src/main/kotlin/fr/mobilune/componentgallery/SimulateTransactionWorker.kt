package fr.mobilune.componentgallery

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/** Déclenche `POST /push/simulate-transaction` en différé, y compris app fermée (programmé par `MainActivity`). */
class SimulateTransactionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val result = simulateTransactionInBackground(applicationContext)
        Log.d("PUSH", "dépense simulée en arrière-plan : n° ${result.transactionId}, appareils notifiés = ${result.sent}")
        Result.success()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Log.e("PUSH", "échec de la dépense simulée en arrière-plan : ${exception.message}", exception)
        Result.failure()
    }
}
