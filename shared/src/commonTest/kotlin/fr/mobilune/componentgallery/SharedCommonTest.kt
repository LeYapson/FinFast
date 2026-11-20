package fr.mobilune.componentgallery

import fr.mobilune.componentgallery.core.designsystem.components.TransactionUiModel
import fr.mobilune.componentgallery.data.UserProfile
import fr.mobilune.componentgallery.viewmodel.CarteUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun dashboardTotalsSeparateNegativeExpensesFromPositiveIncome() {
        val dashboard = CarteUiState.Dashboard(
            profile = UserProfile("user", "User", 12_500),
            transactions = listOf(
                TransactionUiModel("1", "Store", "date", "-20", amountCents = -2_000),
                TransactionUiModel("2", "Store", "date", "-30", amountCents = -3_000),
                TransactionUiModel("3", "Transfer", "date", "+45", true, 4_500),
                TransactionUiModel("4", "Zero", "date", "0", amountCents = 0),
            ),
        )

        assertEquals(-5_000, dashboard.expensesCents)
        assertEquals(4_500, dashboard.incomeCents)
    }

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }
}