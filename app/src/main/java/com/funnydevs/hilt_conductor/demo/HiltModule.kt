package com.funnydevs.hilt_conductor.demo

import com.funnydevs.hilt_conductor.ControllerComponent
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import javax.inject.Named

@InstallIn(ControllerComponent::class)
@Module
object HiltModule {

  @Provides
  @ControllerScoped
  @Named("first")
  fun textOne(): String = "Hello World"

  @Provides
  @ControllerScoped
  @Named("second")
  fun textTwo(): String = "Hello Moon"
}