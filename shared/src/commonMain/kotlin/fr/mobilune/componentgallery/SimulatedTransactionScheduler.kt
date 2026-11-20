package fr.mobilune.componentgallery

/**
 * Programme une dépense simulée (`POST /push/simulate-transaction`) qui part **même si l'app est fermée**
 * entre-temps : c'est ce qui permet de tester la notification « app fermée » sans passer par le Swagger.
 * Fourni par la plateforme (WorkManager sur Android) et passé à [App].
 */
interface SimulatedTransactionScheduler {
    val isSupported: Boolean

    /** Lance la simulation dans [delaySeconds] secondes, en arrière-plan. */
    fun scheduleIn(delaySeconds: Int)

    companion object {
        /** Plateforme sans programmation en arrière-plan (ex. desktop). */
        val Unsupported: SimulatedTransactionScheduler = object : SimulatedTransactionScheduler {
            override val isSupported = false
            override fun scheduleIn(delaySeconds: Int) = Unit
        }
    }
}

/** Délai proposé avant l'envoi : le temps de fermer l'app ou de la mettre en arrière-plan. */
const val SIMULATED_TRANSACTION_DELAY_SECONDS = 30
