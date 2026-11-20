package fr.mobilune.componentgallery.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import fr.mobilune.componentgallery.ThemeConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Couleurs lues à chaque composition : elles suivent l'option « contraste élevé ».
private val CardText: Color get() = ThemeConstants.onClassicBackground
private val ExpenseBackground: Color get() = ThemeConstants.outcomeBackground
private val ExpenseIcon: Color get() = ThemeConstants.outcomeBackgroundStrong
private val IncomeBackground: Color get() = ThemeConstants.incomeBackground
private val IncomeIcon: Color get() = ThemeConstants.incomeBackgroundStrong
private val StoreIcon = ImageVector.Builder(
    name = "Store",
    defaultWidth = 32.dp,
    defaultHeight = 32.dp,
    viewportWidth = 32f,
    viewportHeight = 32f,
).apply {
    addPath(
        pathData = PathParser().parsePathString(
            "M29.332 10.6666C29.3321 10.4247 29.2662 10.1872 29.1413 9.97996L25.5293 3.95996C25.2913 3.56641 24.9561 3.24072 24.5559 3.01422C24.1556 2.78771 23.7038 2.66801 23.244 2.66663H8.75328C7.82262 2.66663 6.94662 3.16263 6.46795 3.96129L2.85595 9.97996C2.73108 10.1872 2.66516 10.4247 2.66528 10.6666C2.66528 12.0066 3.17995 13.2186 3.99862 14.1573V26.6666C3.99862 27.0202 4.13909 27.3594 4.38914 27.6094C4.63919 27.8595 4.97833 28 5.33195 28H15.9986C16.3522 28 16.6914 27.8595 16.9414 27.6094C17.1915 27.3594 17.332 27.0202 17.332 26.6666V20H22.6653V26.6666C22.6653 27.0202 22.8058 27.3594 23.0558 27.6094C23.3059 27.8595 23.645 28 23.9986 28H26.6653C27.0189 28 27.358 27.8595 27.6081 27.6094C27.8581 27.3594 27.9986 27.0202 27.9986 26.6666V14.1573C28.8173 13.2186 29.332 12.0066 29.332 10.6666V10.6666ZM26.6439 11.0013C26.5619 11.6452 26.2481 12.2371 25.7612 12.6663C25.2743 13.0955 24.6477 13.3327 23.9986 13.3333C22.528 13.3333 21.332 12.1373 21.332 10.6666C21.332 10.576 21.2986 10.496 21.28 10.4106L21.3066 10.4053L20.292 5.33329H23.244L26.6439 11.0013V11.0013ZM13.3413 10.7533L14.424 5.33329H17.572L18.656 10.7533C18.6093 12.184 17.4386 13.3333 15.9986 13.3333C14.5586 13.3333 13.3879 12.184 13.3413 10.7533V10.7533ZM8.75328 5.33329H11.7053L10.692 10.4053L10.7186 10.4106C10.6986 10.496 10.6653 10.576 10.6653 10.6666C10.6653 12.1373 9.46928 13.3333 7.99862 13.3333C7.34954 13.3327 6.72293 13.0955 6.23603 12.6663C5.74913 12.2371 5.43531 11.6452 5.35328 11.0013L8.75328 5.33329ZM13.332 21.3333H7.99862V17.3333H13.332V21.3333Z",
        ).toNodes(),
        fill = SolidColor(ExpenseIcon),
    )
}.build()

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    store: String = "H&M",
    date: String = "10 jan 2022",
    price: String = "-29",
    isIncome: Boolean = false,
) {
    // Une transaction = un seul élément lu : « Julien, 29 oct. 2026, entrée de 10,00 € ».
    val spokenAmount = price.trimStart('+', '-', '−')
    val spokenDescription = "$store, $date, ${if (isIncome) "entrée" else "dépense"} de $spokenAmount"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = spokenDescription },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (isIncome) IncomeBackground else ExpenseBackground,
                    shape = RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = StoreIcon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(if (isIncome) IncomeIcon else ExpenseIcon),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store,
                color = CardText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = date,
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Text(
            text = price,
            color = CardText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
