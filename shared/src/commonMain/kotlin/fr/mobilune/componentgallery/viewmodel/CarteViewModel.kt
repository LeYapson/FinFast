package fr.mobilune.componentgallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mobilune.componentgallery.components.TransactionUiModel
import fr.mobilune.componentgallery.data.AuthRepository
import fr.mobilune.componentgallery.data.AuthSession
import fr.mobilune.componentgallery.data.TransactionRepository
import fr.mobilune.componentgallery.data.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CarteUiState {
    data object Loading : CarteUiState
    data class Dashboard(
        // Absent tant que le profil n'a pas pu être chargé (ex. hors ligne).
        val profile: UserProfile?,
        val transactions: List<TransactionUiModel>,
        val lastUpdatedMillis: Long? = null,
        val isRefreshing: Boolean = false,
        val refreshErrorMessage: String? = null,
    ) : CarteUiState {
        val expensesCents: Long = transactions
            .asSequence()
            .map { it.amountCents }
            .filter { it < 0 }
            .sum()
        val incomeCents: Long = transactions
            .asSequence()
            .map { it.amountCents }
            .filter { it > 0 }
            .sum()
    }
    data class Error(val message: String) : CarteUiState
}

private data class RefreshState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class CarteViewModel(
    private val repository: TransactionRepository,
    private val authRepository: AuthRepository,
    authSession: AuthSession,
) : ViewModel() {
    private val profile = MutableStateFlow<UserProfile?>(null)
    private val refreshState = MutableStateFlow(RefreshState())

    // Les transactions viennent de la base : le cache s'affiche tout de suite, puis se met à jour.
    val uiState: StateFlow<CarteUiState> = combine(
        repository.transactions,
        repository.lastUpdatedMillis,
        profile,
        refreshState,
    ) { transactions, lastUpdated, profile, refresh ->
        when {
            lastUpdated != null || transactions.isNotEmpty() -> CarteUiState.Dashboard(
                profile = profile,
                transactions = transactions,
                lastUpdatedMillis = lastUpdated,
                isRefreshing = refresh.isRefreshing,
                refreshErrorMessage = refresh.errorMessage,
            )

            refresh.errorMessage != null -> CarteUiState.Error(refresh.errorMessage)
            else -> CarteUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CarteUiState.Loading)

    init {
        // Session terminée : ne pas montrer le profil de l'ancien compte au prochain login.
        viewModelScope.launch {
            authSession.expirations.collect {
                profile.value = null
                refreshState.value = RefreshState()
            }
        }
        loadDashboard()
    }

    fun loadDashboard() {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            launch {
                try {
                    profile.value = authRepository.getCurrentUser()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Hors ligne : on garde le profil précédent, s'il y en a un.
                }
            }
            val errorMessage = try {
                repository.refresh()
                null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                exception.message ?: "Impossible de charger les données du compte."
            }
            refreshState.update { RefreshState(isRefreshing = false, errorMessage = errorMessage) }
        }
    }
}
