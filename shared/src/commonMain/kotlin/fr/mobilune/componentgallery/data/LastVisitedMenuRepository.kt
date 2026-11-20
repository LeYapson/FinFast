package fr.mobilune.componentgallery.data

enum class MainMenu {
    Home,
    Card,
}

interface LastVisitedMenuRepository {
    suspend fun getLastVisitedMenu(): MainMenu

    suspend fun saveLastVisitedMenu(menu: MainMenu)
}
