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
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic


@AutoService(Processor::class)
class FileGenerator : AbstractProcessor() {

//  val injectablesFields = mutableMapOf<String, MutableList<Pair<String, String>>>()
  private val whereInjectList = mutableListOf<WhereInject>()
  private val whereNamedList = mutableListOf<WhereNamed>()
  private var build = false

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
          val classType = namedElement::class.members.first { it.name == "owner" }.call(namedElement) as? TypeElement
          if (classType is TypeElement) {
            val controllerTypeClass = findIfHasControllerParent(classType)

            if (controllerTypeClass!= null &&
              (   isSuperclassOfType(classType, rootClassType) ||
                  classType.toString() == rootClassType.toString()
              )
            ) {
              val classTypeName = rootClassType.toString()
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
          val classType = injectElement::class.members.first { it.name == "owner" }.call(injectElement) as? TypeElement
          if (classType is TypeElement) {
            val controllerTypeClass = findIfHasControllerParent(classType)

            if (controllerTypeClass!= null &&
              (   isSuperclassOfType(classType, rootClassType) ||
                  classType.toString() == rootClassType.toString()
              )
            ) {
              val classTypeName = rootClassType.toString()
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
      generateInjectors()

      build = true
    } catch (t: Throwable) {
      println(t.message)
    }


    return true
  }

  private fun isSuperclassOfType(classType: TypeElement, rootClassType: TypeElement): Boolean {
    var currentType: TypeMirror? = rootClassType.superclass

    while (currentType != null && currentType.kind != TypeKind.NONE) {
      if (currentType.toString() == classType.toString()) {
        return true
      }
      val currentElement = (currentType as DeclaredType).asElement() as TypeElement
      currentType = currentElement.superclass
    }

    return false
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
        val packageValue = element.className.substringBeforeLast(".")
        val className = element.className.substringAfterLast(".")

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
                      field.type.substringBeforeLast("."),
                      field.type.substringAfterLast(".")
                    )
                  )
                  .apply {
                    val namedElement = whereNamedList.firstOrNull { it.className == element.className }
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

  private fun generateInjectors() {
    whereInjectList.forEach { whereInject ->
      println("whereInject = $whereInject")
      val className = whereInject.className.substringAfterLast(".")
      val packageValue = whereInject.className.substringBeforeLast(".")

      val classBuilder = TypeSpec.classBuilder("${className}_Injector")
        .addModifiers(Modifier.PUBLIC)

      val injectMethodBuilder = MethodSpec.methodBuilder("inject")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(Void.TYPE)
        .addParameter(ClassName.get(packageValue.substringBeforeLast("."), "ConductorComponentLifecycleHandler"), "handler")
        .addParameter(ClassName.bestGuess(whereInject.className), "controller")

      injectMethodBuilder.addStatement("\$T entryPoint = \$T.get(handler, \$T.class)",
        ClassName.get("java.lang", "Object"), // For 'Object'
        ClassName.get("dagger.hilt", "EntryPoints"), // For 'EntryPoints'
        ClassName.get(packageValue, "${className}HiltInterface"))


      whereInject.fields.forEach { field ->
        val methodName = "${className}_${field.name.capitalize()}"
        injectMethodBuilder.addStatement("controller.\$N = ((\$T)entryPoint).$methodName()",
          field.name,
          ClassName.get(packageValue, "${className}HiltInterface"))
      }


      classBuilder.addMethod(injectMethodBuilder.build())

      val javaFile = JavaFile.builder(packageValue, classBuilder.build())
      try {
        javaFile.build().writeTo(processingEnv.filer)
      } catch (e: Exception) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message)
      }
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