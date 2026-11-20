package fr.mobilune.componentgallery.core.designsystem

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatLastUpdate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRANCE).format(Date(epochMillis))
