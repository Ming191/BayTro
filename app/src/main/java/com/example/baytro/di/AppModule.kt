package com.example.baytro.di

import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.user.UserRepository
import com.example.baytro.service.FptAiService
import com.example.baytro.viewModel.AddBuildingVM
import com.example.baytro.viewModel.BuildingListVM
import com.example.baytro.viewModel.Room.AddRoomVM
import com.example.baytro.viewModel.Room.EditRoomVM
import com.example.baytro.viewModel.Room.RoomDetailsVM
import com.example.baytro.viewModel.Room.RoomListVM
import com.example.baytro.viewModel.EditBuildingVM
import com.example.baytro.viewModel.auth.SignInVM
import com.example.baytro.viewModel.auth.SignUpVM
import com.example.baytro.viewModel.contract.AddContractVM
import com.example.baytro.viewModel.contract.ContractDetailsVM
import com.example.baytro.viewModel.contract.ContractListVM
import com.example.baytro.viewModel.contract.TenantJoinVM
import com.example.baytro.viewModel.splash.IdCardDataViewModel
import com.example.baytro.viewModel.splash.NewLandlordUserVM
import com.example.baytro.viewModel.splash.NewTenantUserVM
import com.example.baytro.viewModel.splash.SplashScreenVM
import com.example.baytro.viewModel.splash.UploadIdCardVM
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { dev.gitlive.firebase.Firebase.firestore }
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
    single<FirebaseFunctions> { com.google.firebase.Firebase.functions }
}

val authModule = module {
    single<UserRepository> { UserRepository(get()) }
    single<BuildingRepository> { BuildingRepository(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
    single<MediaRepository> { MediaRepository(get()) }
    single<RoomRepository> { RoomRepository(get()) }
    single<FptAiService> { FptAiService(get(), get()) }
    single<ContractRepository> { ContractRepository(get()) }
    single<QrSessionRepository> { QrSessionRepository(get(),get()) }
    single<RoomRepository> { RoomRepository(get()) }
    single { IdCardDataViewModel() }

    viewModel {
        SplashScreenVM(get()
        )
    }
    viewModel {
        NewLandlordUserVM(
            androidContext(),
            get(),
            get(),
            get()
        )
    }
    viewModel {
        SignUpVM(get()
        )
    }
    viewModel {
        SignInVM(
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModel {
        AddBuildingVM(
            androidContext(),
            get(),
            get(),
            get()
        )
    }
    viewModel {
        BuildingListVM(
            get(),
            get()
        )
    }
    viewModel {
        AddContractVM(
            androidContext(),
            get(), get(),
            get(),
            get(),
            get()
        )
    }
    viewModel {
        UploadIdCardVM(
            androidContext(),
            get(),
            get(),
            get()
        )
    }
    viewModel {
        NewTenantUserVM(
            androidContext(),
            get(),
            get(),
            get()
        )
    }

    viewModel {
        ContractDetailsVM(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    viewModel {
        TenantJoinVM(
            get(),
            get(),
            get()
        )
    }

    viewModel {
        ContractListVM(
            get(),
            get(),
            get()
        )
    }
    viewModel { BuildingListVM(get(), get()) }
    viewModel { AddRoomVM(get(), get()) }
    viewModel { RoomListVM(get(), get(), get()) }
    viewModel { RoomDetailsVM(get(), get()) }
    viewModel { EditRoomVM(get(), get()) }
    viewModel { EditBuildingVM(androidContext(), get(), get(), get()) }
}
