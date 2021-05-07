package com.funnydevs.hilt_conductor.demo

import com.funnydevs.hilt_conductor.ControllerComponent
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn

@InstallIn(ControllerComponent::class)
@Module
object HiltModule {

    @Provides
    @ControllerScoped
    fun text(): String = "Hello World"
}