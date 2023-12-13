package com.funnydevs.hilt_conductor.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.api.variant.AndroidComponentsExtension
import com.funnydevs.hilt_conductor.visitors.ConductorEntryPointClassVisitor

class HiltConductorPlugin : Plugin<Project> {
  override fun apply(project: Project) {

    val isAndroid =
      project.plugins.hasPlugin(AppPlugin::class.java) || project.plugins.hasPlugin(
        LibraryPlugin::class.java
      )
    if (!isAndroid)
      throw GradleException("'com.android.application' or 'com.android.library' plugin required.")

    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

    androidComponents.onVariants { variant ->
      variant.instrumentation.apply {

        transformClassesWith(
          classVisitorFactoryImplClass = ConductorEntryPointClassVisitor.Factory::class.java,
          scope = InstrumentationScope.ALL,
          instrumentationParamsConfig = {}
        )
        setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES)
      }
    }

  }
}

