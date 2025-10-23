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
import com.example.baytro.utils.cloudFunctions.BuildingCloudFunctions
import com.example.baytro.utils.AvatarCache
import com.example.baytro.service.FptAiService
import com.example.baytro.service.MeterReadingApiService
import com.example.baytro.utils.cloudFunctions.DashboardCloudFunctions
import com.example.baytro.utils.cloudFunctions.MeterReadingCloudFunctions
import com.example.baytro.viewModel.AddBuildingVM
import com.example.baytro.viewModel.BuildingListVM
import com.example.baytro.viewModel.EditBuildingVM
import com.example.baytro.viewModel.Room.AddRoomVM
import com.example.baytro.viewModel.Room.EditRoomVM
import com.example.baytro.viewModel.Room.RoomDetailsVM
import com.example.baytro.viewModel.Room.RoomListVM
import com.example.baytro.viewModel.auth.ChangePasswordVM
import com.example.baytro.viewModel.auth.EditPersonalInformationVM
import com.example.baytro.viewModel.auth.ForgotPasswordVM
import com.example.baytro.viewModel.auth.PersonalInformationVM
import com.example.baytro.viewModel.auth.SignInVM
import com.example.baytro.viewModel.auth.SignUpVM
import com.example.baytro.viewModel.billing.LandlordBillsViewModel
import com.example.baytro.viewModel.billing.TenantBillViewModel
import com.example.baytro.viewModel.contract.AddContractVM
import com.example.baytro.viewModel.contract.ContractDetailsVM
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.EditContractVM
import com.example.baytro.viewModel.dashboard.LandlordDashboardVM
import com.example.baytro.viewModel.contract.TenantJoinVM
import com.example.baytro.viewModel.dashboard.MeterReadingVM
import com.example.baytro.viewModel.dashboard.TenantDashboardVM
import com.example.baytro.viewModel.importExcel.ImportBuildingRoomVM
import com.example.baytro.viewModel.meter_reading.MeterReadingHistoryVM
import com.example.baytro.viewModel.meter_reading.PendingMeterReadingsVM
import com.example.baytro.viewModel.request.AddRequestVM
import com.example.baytro.viewModel.request.AssignRequestVM
import com.example.baytro.viewModel.request.RequestListVM
import com.example.baytro.viewModel.request.UpdateRequestVM
import com.example.baytro.viewModel.service.AddServiceVM
import com.example.baytro.viewModel.service.EditServiceVM
import com.example.baytro.viewModel.service.ServiceListVM
import com.example.baytro.viewModel.SettingsVM
import com.example.baytro.viewModel.billing.BillDetailsViewModel
import com.example.baytro.viewModel.splash.IdCardDataViewModel
import com.example.baytro.viewModel.splash.NewLandlordUserVM
import com.example.baytro.viewModel.splash.NewTenantUserVM
import com.example.baytro.viewModel.splash.SplashScreenVM
import com.example.baytro.viewModel.splash.UploadIdCardVM
import com.example.baytro.viewModel.tenant.TenantListVM
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.google.firebase.functions.functions
import com.google.firebase.storage.FirebaseStorage
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

val appModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> {
        dev.gitlive.firebase.Firebase.firestore
    }
    single<FirebaseStorage> { FirebaseStorage.getInstance() }
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
    single<FirebaseFunctions> { Firebase.functions }
    single { AvatarCache(get()) }
    single<BuildingCloudFunctions> { BuildingCloudFunctions(get(), get()) }

}

val authModule = module {
    single<UserRepository> { UserRepository(get()) }
    single<BuildingRepository> { BuildingRepository(get(), get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
    single<MediaRepository> { MediaRepository(get()) }
    single<RoomRepository> { RoomRepository(get()) }
    single<FptAiService> { FptAiService(get(), get()) }
    single<MeterReadingApiService> { MeterReadingApiService(get()) }
    single<ContractRepository> { ContractRepository(get()) }
    single<QrSessionRepository> { QrSessionRepository(get(),get()) }
    single<RequestRepository> { RequestRepository(get()) }
    single<DashboardCloudFunctions> { DashboardCloudFunctions(get(), get()) }
    single<MeterReadingRepository> { MeterReadingRepository(get()) }
    single<MeterReadingCloudFunctions> { MeterReadingCloudFunctions(get()) }
    single<BillRepository> { BillRepository(get()) }
    single { IdCardDataViewModel() }
    single { UserRoleCache(androidContext()) }
    viewModelOf(::SplashScreenVM)
    viewModelOf(::NewLandlordUserVM)
    viewModelOf(::SignUpVM)
    viewModelOf(::SignInVM)
    viewModelOf(::ForgotPasswordVM)
    viewModelOf(::AddBuildingVM)
    viewModelOf(::BuildingListVM)
    viewModelOf(::AddContractVM)
    viewModelOf(::UploadIdCardVM)
    viewModelOf(::NewTenantUserVM)
    viewModelOf(::ContractDetailsVM)
    viewModelOf(::TenantJoinVM)
    viewModelOf(::ContractListVM)
    viewModelOf(::EditContractVM)
    viewModelOf(::RequestListVM)
    viewModelOf(::AddRoomVM)
    viewModelOf(::RoomListVM)
    viewModelOf(::RoomDetailsVM)
    viewModelOf(::EditRoomVM)
    viewModelOf(::EditBuildingVM)
    viewModelOf(::TenantDashboardVM)
    viewModelOf(::MeterReadingVM)
    viewModelOf(::PendingMeterReadingsVM)
    viewModelOf(::MeterReadingHistoryVM)
    viewModelOf(::AddRequestVM)
    viewModelOf(::UpdateRequestVM)
    viewModelOf(::AssignRequestVM)
    viewModelOf(::ImportBuildingRoomVM)
    viewModelOf(::LandlordBillsViewModel)
    viewModelOf(::TenantBillViewModel)
    viewModelOf(::SettingsVM)
    viewModelOf(::BillDetailsViewModel)
    viewModelOf(::LandlordDashboardVM)
}
val serviceModule = module {
    viewModelOf(::EditServiceVM)
    viewModelOf(::ServiceListVM)
    viewModelOf(::AddServiceVM)
    viewModelOf(::TenantListVM)
    viewModelOf(::PersonalInformationVM)
    viewModelOf(::EditPersonalInformationVM)
    viewModelOf(::ChangePasswordVM)
}
