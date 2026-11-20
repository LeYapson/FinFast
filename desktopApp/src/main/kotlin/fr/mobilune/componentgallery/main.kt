package fr.mobilune.componentgallery

import fr.mobilune.componentgallery.data.PREFERENCES_FILE_NAME
import fr.mobilune.componentgallery.data.db.DATABASE_FILE_NAME
import java.io.File
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ComponentGallery",
    ) {
        App(
            onExit = { exitApplication() },
            dataStoreFilePath = File(
                System.getProperty("user.home"),
                ".componentgallery/$PREFERENCES_FILE_NAME",
            ).absolutePath,
            databaseFilePath = File(
                System.getProperty("user.home"),
                ".componentgallery/$DATABASE_FILE_NAME",
            ).absolutePath,
        )
    }
}
