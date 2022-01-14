package com.funnydevs.hilt_conductor.demo

import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import javax.inject.Inject
import javax.inject.Named

abstract class BaseController(args: Bundle?) : Controller(args) {

  @Inject
  @Named("second")
  lateinit var testo: String
}