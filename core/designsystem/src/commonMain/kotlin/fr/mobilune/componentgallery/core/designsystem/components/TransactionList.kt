package fr.mobilune.componentgallery.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mobilune.componentgallery.core.designsystem.ThemeConstants
import fr.mobilune.componentgallery.core.designsystem.formatLastUpdate
import fr.mobilune.componentgallery.core.designsystem.heading
import fr.mobilune.componentgallery.core.designsystem.politeLiveRegion

data class TransactionUiModel(
    val id: String,
    val store: String,
    val date: String,
    val price: String,
    val isIncome: Boolean = false,
    val amountCents: Long = 0,
)

@Composable
fun TransactionList(
    transactions: List<TransactionUiModel>,
    modifier: Modifier = Modifier,
    lastUpdatedMillis: Long? = null,
    isRefreshing: Boolean = false,
    refreshErrorMessage: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = "Transactions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeConstants.onClassicBackground,
            modifier = Modifier
                .padding(bottom = if (lastUpdatedMillis != null || isRefreshing) 4.dp else 20.dp)
                .heading(),
        )
        if (lastUpdatedMillis != null || isRefreshing) {
            Text(
                text = if (isRefreshing) {
                    "Actualisation…"
                } else {
                    "Dernière mise à jour : ${formatLastUpdate(lastUpdatedMillis!!)}"
                },
                fontSize = 13.sp,
                color = ThemeConstants.secondaryText,
                modifier = Modifier
                    .padding(bottom = if (refreshErrorMessage != null) 4.dp else 16.dp)
                    // « Actualisation… » puis l'heure de mise à jour sont annoncés quand ils changent.
                    .politeLiveRegion(),
            )
        }
        if (refreshErrorMessage != null) {
            Text(
                text = "Mise à jour impossible, données enregistrées affichées.",
                fontSize = 13.sp,
                color = ThemeConstants.error,
                modifier = Modifier.padding(bottom = 16.dp).politeLiveRegion(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(ThemeConstants.borderWidthDp.dp, ThemeConstants.border, RoundedCornerShape(28.dp))
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Cette semaine",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeConstants.onClassicBackground,
                modifier = Modifier.padding(bottom = 8.dp).heading(),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune transaction cette semaine.",
                            color = ThemeConstants.onClassicBackground,
                        )
                    }
                } else {
                    items(
                        items = transactions,
                        key = { transaction -> transaction.id },
                    ) { transaction ->
                        TransactionCard(
                            modifier = Modifier.fillMaxWidth(),
                            store = transaction.store,
                            date = transaction.date,
                            price = transaction.price,
                            isIncome = transaction.isIncome,
                        )
                    }
                }
            }
        }
    }
}
