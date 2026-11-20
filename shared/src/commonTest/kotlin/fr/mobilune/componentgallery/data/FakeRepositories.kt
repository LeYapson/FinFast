package fr.mobilune.componentgallery.data

class FakeLastVisitedMenuRepository(var menu: MainMenu = MainMenu.Home) : LastVisitedMenuRepository {
    override suspend fun getLastVisitedMenu(): MainMenu = menu

    override suspend fun saveLastVisitedMenu(menu: MainMenu) {
        this.menu = menu
    }
}

class FakeVirtualCardRepository(
    var cardId: String? = null,
    var enabled: Boolean = false,
) : VirtualCardRepository {
    override suspend fun getCardId(): String? = cardId

    override suspend fun isCardEnabled(): Boolean = enabled

    override suspend fun saveCardId(cardId: String) {
        this.cardId = cardId
    }

    override suspend fun setCardEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

class FakeRegisteredAccountRepository : RegisteredAccountRepository {
    val registeredUserIds = mutableSetOf<String>()

    override suspend fun isRegistered(userId: String): Boolean = userId in registeredUserIds

    override suspend fun markRegistered(userId: String) {
        registeredUserIds += userId
    }
}
