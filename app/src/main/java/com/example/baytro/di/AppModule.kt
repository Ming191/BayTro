package com.example.baytro.di

import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.RoomRepository
import com.example.baytro.data.UserRepository
import com.example.baytro.viewModel.AddBuildingVM
import com.example.baytro.viewModel.BuildingListVM
import com.example.baytro.viewModel.Room.AddRoomVM
import com.example.baytro.viewModel.auth.SignInVM
import com.example.baytro.viewModel.auth.SignUpVM
import com.example.baytro.viewModel.splash.NewLandlordUserVM
import com.example.baytro.viewModel.splash.SplashScreenVM
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { Firebase.firestore }
    single<FirebaseStorage> { FirebaseStorage.getInstance() }
}

val authModule = module {
    single<UserRepository> { UserRepository(get()) }
    single<BuildingRepository> { BuildingRepository(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    single<MediaRepository> { MediaRepository(get()) }
    single<RoomRepository> { RoomRepository(get()) }

    viewModel { SplashScreenVM(get()) }
    viewModel { NewLandlordUserVM(androidContext(), get(), get(), get()) }
    viewModel { SignUpVM(get()) }
    viewModel { SignInVM(get(), get()) }
    viewModel { AddBuildingVM(get(), get()) }
    viewModel { BuildingListVM(get(), get()) }
    viewModel { AddRoomVM(get(), get()) }
}
