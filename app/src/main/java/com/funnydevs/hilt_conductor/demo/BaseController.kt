package com.funnydevs.hilt_conductor.demo

import android.os.Bundle
import javax.inject.Inject
import javax.inject.Named

abstract class BaseController(args: Bundle?): BaseBaseController(args) {

  @Inject
  @Named("first")
  lateinit var text: String
}