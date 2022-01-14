package com.funnydevs.hilt_conductor.processor

import com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint
import com.google.auto.service.AutoService
import com.funnydevs.hilt_conductor.annotations.ControllerScoped
import com.squareup.javapoet.*
import javax.annotation.processing.*
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType


@AutoService(Processor::class)
class FileGenerator : AbstractProcessor() {

//  var injectablesFields = mutableMapOf<String, MutableList<Pair<String, String>>>()
  var whereInjectList = mutableListOf<WhereInject>()
  var whereNamedList = mutableListOf<WhereNamed>()
  var build = false

  override fun getSupportedAnnotationTypes(): MutableSet<String> {
    return mutableSetOf(ControllerScoped::class.java.name, Named::class.java.name, Inject::class.java.name)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latest()
  }

  override fun process(ann: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {

    if (build)
      return false

    try {
      roundEnv.getElementsAnnotatedWith(ConductorEntryPoint::class.java)?.forEach { rootElement ->

        val rootClassType = rootElement as TypeElement
        roundEnv.getElementsAnnotatedWith(Named::class.java)?.forEach { namedElement ->
          var classType = namedElement::class.members.let { members ->
            members.first { it.name == "owner" }.let { it.call(namedElement) }
          } as? TypeElement
          if (classType is TypeElement) {
            var controllerTypeClass = findIfHasControllerParent(classType)

            if (controllerTypeClass!= null &&
              (   classType.toString() == rootClassType.superclass.toString() ||
                  classType.toString() == rootClassType.toString()
              )
            ) {
              var classTypeName = rootClassType.toString()
              val value =
                namedElement.annotationMirrors.first { it.annotationType.asElement().simpleName.toString() == Named::class.simpleName.toString() }
                  .elementValues.values.iterator().next().toString()

              if (whereNamedList.firstOrNull { it.className == classTypeName } == null)
                whereNamedList.add(WhereNamed(classTypeName))

              whereNamedList.first { it.className == classTypeName }
                .fields.add(
                  WhereNamed.Field(
                    value = value,
                    name = namedElement.simpleName.toString()
                      .replace("\$annotations", "")
                      .replace("get", "")
                  )
                )
            }
          }
        }


        roundEnv.getElementsAnnotatedWith(Inject::class.java)?.forEach { injectElement ->
          var classType = injectElement::class.members.let { members ->
            members.first { it.name == "owner" }.let { it.call(injectElement) }
          } as? TypeElement
          if (classType is TypeElement) {
            var controllerTypeClass = findIfHasControllerParent(classType)

            if (controllerTypeClass!= null &&
              (   classType.toString() == rootClassType.superclass.toString() ||
                  classType.toString() == rootClassType.toString()
              )
            ) {
              var classTypeName = rootClassType.toString()
              val fieldClassType = injectElement.asType().toString()
              if (whereInjectList.firstOrNull { it.className == classTypeName } == null)
                whereInjectList.add(WhereInject(className = classTypeName))

              whereInjectList.first { it.className == classTypeName }
                .fields.add(WhereInject.Field(fieldClassType,injectElement.simpleName.toString()))

            }
          }
        }

      }



      generateInterfaces()
      build = true
    } catch (t: Throwable) {
      println(t.message)
    }


    return true
  }


  private fun findIfHasControllerParent(typeElement: TypeElement): TypeElement? {

    try {
      if (typeElement.superclass != null && typeElement.superclass.toString().startsWith("com.bluelinelabs.conductor.Controller")) {
        return (typeElement.superclass as DeclaredType).asElement() as TypeElement
      }
      else
        return findIfHasControllerParent((typeElement.superclass as DeclaredType).asElement() as TypeElement)
    } catch (t: Throwable) {
      print(t.message)
    }

    return null
  }


  private fun generateInterfaces() {
    try {
      for (element in whereInjectList) {
        var packageValue = element.className.substringBeforeLast(".")
        var className = element.className.substringAfterLast(".")

        val hiltInterface: TypeSpec =
          TypeSpec.interfaceBuilder("${className}HiltInterface").addModifiers(Modifier.PUBLIC)
            .addAnnotation(
              AnnotationSpec.builder(Class.forName("dagger.hilt.InstallIn"))
                .addMember("value", "com.funnydevs.hilt_conductor.ControllerComponent.class")
                .build()
            )
            .addAnnotation(Class.forName("dagger.hilt.EntryPoint"))
            .apply {
              for (field in element.fields)
                addMethod(MethodSpec.methodBuilder(
                  "${className}_${
                    field.name.substringAfterLast(".").capitalize()
                  }"
                )
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(
                    ClassName.get(
                      "${field.type.substringBeforeLast(".")}",
                      "${field.type.substringAfterLast(".")}"
                    )
                  )
                  .apply {
                    var namedElement = whereNamedList.firstOrNull { it.className == element.className }
                      ?.fields?.firstOrNull { it.name == field.name.capitalize() }
                    if (namedElement != null){
                      addAnnotation(AnnotationSpec.builder(Class.forName ("javax.inject.Named"))
                        .addMember("value", namedElement.value)
                        .build())
                    }
                  }
                  .build())
            }
            .addModifiers(Modifier.PUBLIC)
            .build()

        JavaFile.builder(packageValue, hiltInterface)
          .build().writeTo(processingEnv.filer)
      }
    } catch (e: Throwable) {
      println(e.message)
      throw e
    }

  }

}

data class WhereInject(
  val className: String,
  val fields: MutableList<Field> = mutableListOf()
) {
  data class Field(val type: String, val name: String)
}

data class WhereNamed(
  val className: String,
  val fields: MutableList<Field> = mutableListOf()
) {
  data class Field(val value: String, val name: String)
}