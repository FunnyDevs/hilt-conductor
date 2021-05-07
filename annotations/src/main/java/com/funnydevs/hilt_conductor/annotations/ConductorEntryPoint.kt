package com.funnydevs.hilt_conductor.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class ConductorEntryPoint(
  val value: KClass<*> = Void::class
)