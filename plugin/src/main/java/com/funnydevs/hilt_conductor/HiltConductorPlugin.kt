package com.funnydevs.hilt_conductor.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project


class HiltConductorPlugin : Plugin<Project> {
  override fun apply(project: Project) {

    val isAndroid =
      project.plugins.hasPlugin(AppPlugin::class.java) || project.plugins.hasPlugin(
        LibraryPlugin::class.java
      )
    if (!isAndroid)
      throw GradleException("'com.android.application' or 'com.android.library' plugin required.")

    val android = project.extensions.findByName("android") as BaseExtension

    android.registerTransform(HiltConductorTransformer(project))
  }

}

