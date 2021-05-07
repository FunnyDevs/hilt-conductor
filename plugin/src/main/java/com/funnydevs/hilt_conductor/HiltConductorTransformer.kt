package com.funnydevs.hilt_conductor.plugin

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.android.build.gradle.BaseExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.gradle.api.Project
import java.util.*


class HiltConductorTransformer(private val project: Project) : Transform() {


  override fun getName(): String = "ConductorHilt"

  override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
    mutableSetOf(DefaultContentType.CLASSES)

  override fun isIncremental(): Boolean = true

  override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
    EnumSet.of(Scope.PROJECT)

  override fun getReferencedScopes(): MutableSet<in QualifiedContent.Scope> =
    EnumSet.of(Scope.EXTERNAL_LIBRARIES, Scope.SUB_PROJECTS, Scope.TESTED_CODE)


  override fun transform(transformInvocation: TransformInvocation) {
    super.transform(transformInvocation)

    /*****************Output directory where files will be saved****/
    val outputDir = transformInvocation.outputProvider.getContentLocation(
      name,
      outputTypes,
      scopes,
      Format.DIRECTORY
    )

    /****************************************************************/

    val ctClasses = getClasses(transformInvocation)
    val controllerClassFilter =
      ctClasses.first().classPool.get("com.bluelinelabs.conductor.Controller")


    ctClasses
      .filter { it.subclassOf(controllerClassFilter) && it.hasAnnotation("com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint") }
      .forEach { controllerClass ->

        controllerClass.addField(
          CtField.make(
              "com.funnydevs.hilt_conductor.ConductorComponentLifecycleHandler handler; ",
            controllerClass
          )
        )

        val injectableFields = StringBuilder()
        for (field in controllerClass.fields){
          if (field.hasAnnotation("javax.inject.Inject"))
            injectableFields
              .append("${field.fieldInfo.name} = hiltInterface")
              .append(".${controllerClass.simpleName}_${field.fieldInfo.name.capitalize()}();\n")
        }



        var hiltInterface = "${controllerClass.packageName}.${controllerClass.simpleName}HiltInterface"

        controllerClass.declaredMethods.firstOrNull() { it.name == "onCreateView" }?.apply {
          this.insertAt(0,
            "com.funnydevs.hilt_conductor.ConductorInterface conductorInterface = " +
            "dagger.hilt.android.EntryPointAccessors.fromApplication(getActivity()," +
            "com.funnydevs.hilt_conductor.ConductorInterface.class);\n" +
            "this.handler = conductorInterface.Conductor_LifeCycleHandler();\n" +
            "handler.inject(getActivity());\n"+
            "$hiltInterface hiltInterface = dagger.hilt.EntryPoints.get(handler,${hiltInterface}.class);\n"
            +injectableFields)
        }

        var onDestroyViewMethod = controllerClass.declaredMethods.firstOrNull { it.name == "onDestroyView" }
        if (onDestroyViewMethod == null){
          onDestroyViewMethod = CtMethod.make("protected void onDestroyView(android.view.View view) {super.onDestroyView(view);}"
            ,controllerClass)

          controllerClass.addMethod(onDestroyViewMethod)
        }

        onDestroyViewMethod?.insertAfter("handler.destroy();")
        


      }



    saveJarFiles(transformInvocation)
    ctClasses.forEach { it.writeFile(outputDir.canonicalPath) }
  }




  private fun getClasses(transformInvocation: TransformInvocation): List<CtClass> {
    val classPool = createClassPool(transformInvocation)
    val isIncremental = transformInvocation.isIncremental
    val classNames =
      if (isIncremental) collectClassNamesForIncrementalBuild(transformInvocation)
      else collectClassNamesForFullBuild(transformInvocation)
    val ctClasses = classNames.map { className -> classPool.get(className) }
    return ctClasses
  }


  private fun createClassPool(invocation: TransformInvocation): ClassPool {
    val classPool = ClassPool(null)
    classPool.appendSystemPath()
    project.extensions.findByType(BaseExtension::class.java)?.bootClasspath?.forEach {
      System.out.println("bootclasspath ${it.absolutePath}")
      classPool.appendClassPath(it.absolutePath)
    }

    invocation.inputs.forEach { input ->
      input.directoryInputs.forEach { classPool.appendClassPath(it.file.absolutePath) }
      input.jarInputs.forEach { classPool.appendClassPath(it.file.absolutePath) }
    }
    invocation.referencedInputs.forEach { input ->
      input.directoryInputs.forEach { classPool.appendClassPath(it.file.absolutePath) }
      input.jarInputs.forEach { classPool.appendClassPath(it.file.absolutePath) }
    }

    return classPool
  }

  private fun collectClassNamesForFullBuild(invocation: TransformInvocation): List<String> =
    invocation.inputs
      .flatMap { it.directoryInputs }
      .flatMap {
        it.file.walkTopDown()
          .filter { file -> file.isFile }
          .map { file -> file.relativeTo(it.file) }
          .toList()
      }
      .map { it.path }
      .filter { it.endsWith(SdkConstants.DOT_CLASS) }
      .map { pathToClassName(it) }

  private fun collectClassNamesForIncrementalBuild(invocation: TransformInvocation): List<String> =
    invocation.inputs
      .flatMap { it.directoryInputs }
      .flatMap {
        it.changedFiles
          .filter { (_, status) -> status != Status.NOTCHANGED && status != Status.REMOVED }
          .map { (file, _) -> file.relativeTo(it.file) }
      }
      .map { it.path }
      .filter { it.endsWith(SdkConstants.DOT_CLASS) }
      .map { pathToClassName(it) }

  private fun pathToClassName(path: String): String {
    return path.substring(0, path.length - SdkConstants.DOT_CLASS.length)
      .replace("/", ".")
      .replace("\\", ".")
  }


  private fun saveJarFiles(transformInvocation: TransformInvocation) {
    transformInvocation.inputs.forEach { transformInput ->
      // Ensure JARs are copied as well:
      transformInput.jarInputs.forEach {
        it.file.copyTo(
          transformInvocation.outputProvider.getContentLocation(
            it.name,
            inputTypes,
            scopes,
            Format.JAR
          ),
          overwrite = true
        )
      }
    }
  }
}