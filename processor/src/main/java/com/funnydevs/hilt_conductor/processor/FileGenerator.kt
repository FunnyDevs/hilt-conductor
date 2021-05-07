package com.funnydevs.hilt_conductor.processor

import com.google.auto.service.AutoService
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import com.squareup.javapoet.*
import javax.annotation.processing.*
import javax.inject.Inject
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType


@AutoService(Processor::class)
class FileGenerator : AbstractProcessor() {

  var injectablesFields = mutableMapOf<String, MutableList<Pair<String,String>>>()
  var build = false

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ControllerScoped::class.java.name, Inject::class.java.name)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latest()
  }

  override fun process(p0: MutableSet<out TypeElement>?, p1: RoundEnvironment): Boolean {

    if (build)
      return false

    try{
      p1.getElementsAnnotatedWith(Inject::class.java)?.forEach { element ->

        var classType = element::class.members.let { members ->
          members.first { it.name == "owner" }.let { it.call(element) }
        } as TypeElement
        var controllerTypeClass = findIfHasControllerParent(classType)

        if (controllerTypeClass.toString().startsWith("com.bluelinelabs.conductor.Controller")){
          var classTypeName = classType.toString()
          val fieldClassType = element.asType().toString()
          if (injectablesFields[classTypeName] == null)
            injectablesFields[classTypeName] = mutableListOf()

          injectablesFields[classTypeName]?.add(Pair(fieldClassType,element.simpleName.toString()))
        }
      }

      generateInterfaces()
      build = true
    }
    catch (t: Throwable){
      println(t.message)
    }


    return true
  }



  private fun findIfHasControllerParent(typeElement: TypeElement): TypeElement{
    try {
      if (typeElement.superclass != null && !typeElement.superclass.toString().startsWith("java.lang.Object")){
        return findIfHasControllerParent((typeElement.superclass as DeclaredType).asElement() as TypeElement)
      }
    }catch (t: Throwable){
      print(t.message)
    }

    return typeElement
  }


  private fun generateInterfaces() {
    try {
      for (key in injectablesFields.keys) {
        var packageValue = key.substringBeforeLast(".")
        var className = key.substringAfterLast(".")

        val hiltInterface: TypeSpec =
          TypeSpec.interfaceBuilder("${className}HiltInterface").addModifiers(Modifier.PUBLIC)
            .addAnnotation(
              AnnotationSpec.builder(Class.forName ("dagger.hilt.InstallIn"))
                .addMember("value", "com.funnydevs.hilt_conductor.ControllerComponent.class")
                .build()
            )
            .addAnnotation(Class.forName("dagger.hilt.EntryPoint"))
            .apply {
              for (field in injectablesFields[key]!!)
                addMethod( MethodSpec.methodBuilder("${className}_${field.second.substringAfterLast(".").capitalize()}")
                  .addModifiers(Modifier.PUBLIC,Modifier.ABSTRACT).returns(ClassName.get("${field.first.substringBeforeLast(".")}",
                    "${field.first.substringAfterLast(".")}"))
                  .build())
            }
            .addModifiers(Modifier.PUBLIC)
            .build()

        JavaFile.builder(packageValue, hiltInterface)
          .build().writeTo(processingEnv.filer)
      }
    }catch (e: Throwable){
      println(e.message)
      throw e
    }

  }

}