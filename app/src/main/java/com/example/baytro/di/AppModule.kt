package com.example.baytro.di

import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.meter_reading.MeterReadingRepository
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.request.RequestRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.user.UserRoleCache
import com.example.baytro.service.FptAiService
import com.example.baytro.service.MeterReadingApiService
import com.example.baytro.utils.AvatarCache
import com.example.baytro.utils.cloudFunctions.BuildingCloudFunctions
import com.example.baytro.utils.cloudFunctions.ContractCloudFunctions
import com.example.baytro.utils.cloudFunctions.DashboardCloudFunctions
import com.example.baytro.utils.cloudFunctions.MeterReadingCloudFunctions
import com.example.baytro.utils.cloudFunctions.RequestCloudFunctions
import com.example.baytro.viewModel.Room.*
import com.example.baytro.viewModel.SettingsVM
import com.example.baytro.viewModel.auth.*
import com.example.baytro.viewModel.billing.*
import com.example.baytro.viewModel.building.*
import com.example.baytro.viewModel.contract.*
import com.example.baytro.viewModel.dashboard.*
import com.example.baytro.viewModel.importExcel.ImportBuildingRoomVM
import com.example.baytro.viewModel.meter_reading.*
import com.example.baytro.viewModel.request.*
import com.example.baytro.viewModel.service.*
import com.example.baytro.viewModel.splash.*
import com.example.baytro.viewModel.tenant.TenantListVM
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { dev.gitlive.firebase.Firebase.firestore }
    single<FirebaseStorage> { FirebaseStorage.getInstance() }
    single<FirebaseFunctions> { Firebase.functions }
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json()
            }
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
    single { Gson() }
    single { AvatarCache(get()) }
    single { UserRoleCache(androidContext()) }
}

val dataModule = module {
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
    single<UserRepository> { UserRepository(get()) }
    single<BuildingRepository> { BuildingRepository(get(), get()) }
    single<RoomRepository> { RoomRepository(get()) }
    single<ContractRepository> { ContractRepository(get()) }
    single<MediaRepository> { MediaRepository(get()) }
    single<QrSessionRepository> { QrSessionRepository(get(), get()) }
    single<RequestRepository> { RequestRepository(get()) }
    single<MeterReadingRepository> { MeterReadingRepository(get()) }
    single<BillRepository> { BillRepository(get()) }
}

val cloudFunctionsModule = module {
    single<BuildingCloudFunctions> { BuildingCloudFunctions(get(), get()) }
    single<ContractCloudFunctions> { ContractCloudFunctions(get(), get()) }
    single<DashboardCloudFunctions> { DashboardCloudFunctions(get(), get()) }
    single<MeterReadingCloudFunctions> { MeterReadingCloudFunctions(get()) }
    single<RequestCloudFunctions> { RequestCloudFunctions(get(), get()) }
}

val externalServicesModule = module {
    single<FptAiService> { FptAiService(get(), get()) }
    single<MeterReadingApiService> { MeterReadingApiService(get()) }
}

val authViewModelModule = module {
    viewModelOf(::SignUpVM)
    viewModelOf(::SignInVM)
    viewModelOf(::ForgotPasswordVM)
    viewModelOf(::PersonalInformationVM)
    viewModelOf(::EditPersonalInformationVM)
    viewModelOf(::ChangePasswordVM)
}

val splashViewModelModule = module {
    single { IdCardDataViewModel() }
    viewModelOf(::SplashScreenVM)
    viewModelOf(::UploadIdCardVM)
    viewModelOf(::NewLandlordUserVM)
    viewModelOf(::NewTenantUserVM)
}

val buildingViewModelModule = module {
    viewModelOf(::BuildingListVM)
    viewModelOf(::AddBuildingVM)
    viewModelOf(::EditBuildingVM)
}

val roomViewModelModule = module {
    viewModelOf(::RoomListVM)
    viewModelOf(::RoomDetailsVM)
    viewModelOf(::AddRoomVM)
    viewModelOf(::EditRoomVM)
}

val contractViewModelModule = module {
    viewModelOf(::ContractListVM)
    viewModelOf(::ContractDetailsVM)
    viewModelOf(::AddContractVM)
    viewModelOf(::EditContractVM)
    viewModelOf(::TenantJoinVM)
}

val serviceViewModelModule = module {
    viewModelOf(::ServiceListVM)
    viewModelOf(::AddServiceVM)
    viewModelOf(::EditServiceVM)
}

val requestViewModelModule = module {
    viewModelOf(::RequestListVM)
    viewModelOf(::AddRequestVM)
    viewModelOf(::UpdateRequestVM)
    viewModelOf(::AssignRequestVM)
}

val billingViewModelModule = module {
    viewModelOf(::LandlordBillsViewModel)
    viewModelOf(::TenantBillViewModel)
    viewModelOf(::BillDetailsViewModel)
}

val meterReadingViewModelModule = module {
    viewModelOf(::MeterReadingVM)
    viewModelOf(::PendingMeterReadingsVM)
    viewModelOf(::MeterReadingHistoryVM)
}

val dashboardViewModelModule = module {
    viewModelOf(::LandlordDashboardVM)
    viewModelOf(::TenantDashboardVM)
}

val tenantViewModelModule = module {
    viewModelOf(::TenantListVM)
}

val utilityViewModelModule = module {
    viewModelOf(::ImportBuildingRoomVM)
    viewModelOf(::SettingsVM)
}