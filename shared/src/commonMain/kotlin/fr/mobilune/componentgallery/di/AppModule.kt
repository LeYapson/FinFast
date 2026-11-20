package fr.mobilune.componentgallery.di

import fr.mobilune.componentgallery.data.LastVisitedMenuRepository
import fr.mobilune.componentgallery.data.LastVisitedMenuRepositoryImpl
import fr.mobilune.componentgallery.data.VirtualCardRepository
import fr.mobilune.componentgallery.data.VirtualCardRepositoryImpl
import fr.mobilune.componentgallery.data.TransactionRepository
import fr.mobilune.componentgallery.data.TransactionRepositoryImpl
import fr.mobilune.componentgallery.data.AuthRepository
import fr.mobilune.componentgallery.data.AuthRepositoryImpl
import fr.mobilune.componentgallery.core.network.AuthSession
import fr.mobilune.componentgallery.data.RegisteredAccountRepository
import fr.mobilune.componentgallery.data.RegisteredAccountRepositoryImpl
import fr.mobilune.componentgallery.createAccessTokenStore
import fr.mobilune.componentgallery.createAppDatabase
import fr.mobilune.componentgallery.data.db.AppDatabase
import fr.mobilune.componentgallery.getPreferencesDataStore
import fr.mobilune.componentgallery.viewmodel.AuthViewModel
import fr.mobilune.componentgallery.data.AccessibilityRepository
import fr.mobilune.componentgallery.data.AccessibilityRepositoryImpl
import fr.mobilune.componentgallery.viewmodel.CarteViewModel
import fr.mobilune.componentgallery.viewmodel.SettingsViewModel
import fr.mobilune.componentgallery.viewmodel.PushViewModel
import fr.mobilune.componentgallery.pushPlatformName
import fr.mobilune.componentgallery.data.PushRepository
import fr.mobilune.componentgallery.data.PushRepositoryImpl
import fr.mobilune.componentgallery.fetchDevicePushToken
import fr.mobilune.componentgallery.viewmodel.ProjectsViewModel
import fr.mobilune.componentgallery.abdelrun.AbdelRunAudio
import fr.mobilune.componentgallery.abdelrun.AbdelRunRepository
import fr.mobilune.componentgallery.abdelrun.AbdelRunRepositoryImpl
import fr.mobilune.componentgallery.projects.ProjectRepository
import fr.mobilune.componentgallery.projects.ProjectRepositoryImpl
import fr.mobilune.componentgallery.core.network.AuthApi
import fr.mobilune.componentgallery.core.network.AuthApiImpl
import fr.mobilune.componentgallery.core.network.PushApi
import fr.mobilune.componentgallery.core.network.PushApiImpl
import fr.mobilune.componentgallery.core.network.TransactionApi
import fr.mobilune.componentgallery.core.network.TransactionApiImpl
import fr.mobilune.componentgallery.core.network.createHttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun appModule(dataStoreFilePath: String, databaseFilePath: String) = module {
    single { getPreferencesDataStore(dataStoreFilePath) }
    single { createAppDatabase(databaseFilePath) }
    single { get<AppDatabase>().transactionDao() }
    single { LastVisitedMenuRepositoryImpl(get()) } bind LastVisitedMenuRepository::class
    single { RegisteredAccountRepositoryImpl(get()) } bind RegisteredAccountRepository::class
    single { VirtualCardRepositoryImpl(get()) } bind VirtualCardRepository::class
    single { AuthSession(createAccessTokenStore(get())) }
    single { createHttpClient() }
    single { AuthApiImpl(get(), get()) } bind AuthApi::class
    single { TransactionApiImpl(get(), get()) } bind TransactionApi::class
    single<Clock> { Clock.System }
    single { TransactionRepositoryImpl(get(), get(), get(), get()) } bind TransactionRepository::class
    single { AuthRepositoryImpl(get(), get()) } bind AuthRepository::class
    viewModel { AuthViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { CarteViewModel(get(), get(), get()) }
    single { AccessibilityRepositoryImpl(get()) } bind AccessibilityRepository::class
    viewModel { SettingsViewModel(get()) }
    single { PushApiImpl(get(), get()) } bind PushApi::class
    single { PushRepositoryImpl(get(), get(), ::fetchDevicePushToken, pushPlatformName()) } bind PushRepository::class
    viewModel {
        PushViewModel(
            pushRepository = get(),
            getDeviceToken = ::fetchDevicePushToken,
            getOwnUserId = { get<AuthRepository>().getCurrentUser().userId },
        )
    }
    single { ProjectRepositoryImpl(get(), { get<Clock>().now().toEpochMilliseconds() }) } bind ProjectRepository::class
    single { AbdelRunRepositoryImpl(get()) } bind AbdelRunRepository::class
    single { AbdelRunAudio() }
    viewModel { ProjectsViewModel(get(), get(), get(), { get<Clock>().now().toEpochMilliseconds() }) }
}
