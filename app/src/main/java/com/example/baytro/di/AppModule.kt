package com.example.baytro.di

import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.data.UserRepository
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.service.ServiceRepository
import com.example.baytro.viewModel.SignInVM
import com.example.baytro.viewModel.SignUpVM
import com.example.baytro.viewModel.service.ServiceListVM
import com.example.baytro.viewModel.service.AddServiceVM
import com.google.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { Firebase.firestore }
}

val authModule = module {
    single<UserRepository> { UserRepository(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    viewModel { SignUpVM(get(), get()) }
    viewModel { SignInVM(get(), get()) }
}

val serviceModule = module {
    single<BuildingRepository> { BuildingRepository(get()) }
    single<ServiceRepository> { ServiceRepository(get()) }
    viewModel { ServiceListVM(get(), get(), get()) }
    viewModel { AddServiceVM(get(), get(), get(), get()) }
}