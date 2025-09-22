package com.example.baytro.di

import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.FirebaseAuthRepository
import com.example.baytro.utils.Validator
import com.example.baytro.viewModel.SignInVM
import com.example.baytro.viewModel.SignUpVM
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
}

val authModule = module {
    single<Validator> { Validator() }
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    viewModel { SignUpVM(get()) }
    viewModel { SignInVM(get(), get()) }
}