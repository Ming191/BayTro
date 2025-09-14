package com.example.baytro.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
}