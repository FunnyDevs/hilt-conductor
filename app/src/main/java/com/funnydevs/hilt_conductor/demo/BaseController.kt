package com.funnydevs.hilt_conductor.demo

import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint
import javax.inject.Inject
import javax.inject.Named

abstract class BaseController(args: Bundle?): BaseBaseController(args) {

  @Inject
  @Named("first")
  lateinit var testo: String
}