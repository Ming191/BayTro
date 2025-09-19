package com.example.baytro.di

import com.example.baytro.data.AuthRepository
import com.example.baytro.viewModel.SignInVM
import com.example.baytro.viewModel.SignUpVM
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
}

val authModule = module {
    single { AuthRepository(get()) }
    viewModel { SignUpVM(get()) }
    viewModel { SignInVM(get()) }
}