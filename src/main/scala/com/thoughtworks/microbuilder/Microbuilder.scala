package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{DependencyVersion, HaxeJavaPlugin}
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object Microbuilder extends AutoPlugin {

  val packageNameValue = "proxy"

  def getModelDir(baseDir: File, subDir: String): File = {
    baseDir / s"src/haxe/${subDir}"
  }

  def getOutputDir(baseDir: File): File = {
    packageNameValue.split('.').foldLeft(baseDir) { (parent, path) =>
      parent / path
    }
  }

  def writeFile(outPutFile: File, content: String) = {
    if (!outPutFile.exists || content != IO.read(outPutFile, scala.io.Codec.UTF8.charSet)) {
      IO.write(outPutFile, content, scala.io.Codec.UTF8.charSet)
    }
    outPutFile
  }

  def getAllModelNamesFrom(modelPath: File, packageName: String): Array[String] = {
    modelPath.list.map("\"" + packageName + "." + _.replaceFirst("[.][^.]+$", "") + "\"")
  }

  def genFileForModel(outputBaseDir: File, outputFileName: String, content: String): File = {
    val packagePath = getOutputDir(outputBaseDir)
    packagePath.mkdirs()
    val outputFile = packagePath / s"${outputFileName}.hx"
    writeFile(outputFile, content)
  }

  object autoImport {
    val jsonStreamDeserializer = taskKey[File]("Generates deserizlier for models.")
    val jsonStreamSerializer = taskKey[File]("Generates serizlier for models.")
    val outgoingProxyFactoryGen = taskKey[File]("Generates outgoing proxy factory")
    val routeConfigurationFactoryGen = taskKey[File]("Generates route configuration factory")

    val className = settingKey[String]("Class name of a specific generating class.")
  }

  import autoImport._

  override def requires = HaxeJavaPlugin

  override def globalSettings = Seq(
    className in jsonStreamDeserializer := "MicrobuilderDeserializer",
    className in jsonStreamSerializer := "MicrobuilderSerializer",
    className in outgoingProxyFactoryGen := "MicrobuilderOutgoingProxyFactory",
    className in routeConfigurationFactoryGen := "MicrobuilderRouteConfigurationFactory"
  )


  val haxelibs = Map(
    "continuation" -> DependencyVersion.SpecificVersion("1.3.2")
  )


  override lazy val projectSettings: Seq[Setting[_]] = (for (c <- AllTargetConfigurations ++ AllTestTargetConfigurations) yield {
    haxeOptions in c ++= haxelibOptions(haxelibs)
  }) ++
    (for (c <- AllTargetConfigurations) yield {
      haxeOptions in c ++= Seq(
        "--macro", "hamu.ExprEvaluator.parseAndEvaluate(\"autoParser.AutoFormatter.BUILDER.defineMacroClass([\\\"com.thoughtworks.microbuilder.core.UriTemplate\\\"],\\\"com.thoughtworks.microbuilder.core.UriTemplateFormatter\\\")\")",
        "--macro", "hamu.ExprEvaluator.parseAndEvaluate(\"autoParser.AutoParser.BUILDER.defineMacroClass([\\\"com.thoughtworks.microbuilder.core.UriTemplate\\\"],\\\"com.thoughtworks.microbuilder.core.UriTemplateParser\\\")\")")
    }) ++
    (for (c <- AllTargetConfigurations ++ AllTestTargetConfigurations) yield {
      haxeOptions in c ++= Seq("-dce", "no")
    }) ++
    (for (c <- Seq(Compile, Test)) yield {
      haxeOptions in c ++= Seq("-D", "scala")
    }) ++ Seq(
    libraryDependencies ++= Seq("com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0",
      "com.qifun" %% "haxe-scala-stm" % "0.1.4" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "microbuilder-play" % "0.1.1",
      "com.thoughtworks.microbuilder" %% "microbuilder-core" % "0.1.1",
      "com.thoughtworks.microbuilder" %% "microbuilder-core" % "0.1.1" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" % "hamu" % "0.2.0" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" % "auto-parser" % "0.2.0" % HaxeJava classifier HaxeJava.name
    ),
    haxelibDependencies ++= haxelibs,
    jsonStreamDeserializer := {
      val modelPath = getModelDir(baseDirectory.value, "model")
      val modelNames = getAllModelNamesFrom(modelPath, "model").mkString(",")
      val classNameValue = (className in jsonStreamDeserializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonDeserializer.generateDeserializer(["com.thoughtworks.microbuilder.core.Failure",${modelNames}]))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    jsonStreamSerializer := {
      val modelPath = getModelDir(baseDirectory.value, "model")
      val modelNames = getAllModelNamesFrom(modelPath, "model").mkString(",")
      val classNameValue = (className in jsonStreamSerializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonSerializer.generateSerializer(["com.thoughtworks.microbuilder.core.Failure",${modelNames}]))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    outgoingProxyFactoryGen := {
      val modelPath = getModelDir(baseDirectory.value, "rpc")
      val modelNames = getAllModelNamesFrom(modelPath, "rpc").mkString(",")
      val classNameValue = (className in outgoingProxyFactoryGen).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using proxy.MicrobuilderDeserializer;
using proxy.MicrobuilderSerializer;
@:nativeGen
@:build(jsonStream.rpc.OutgoingProxyFactory.generateOutgoingProxyFactory([${modelNames}]))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    routeConfigurationFactoryGen := {
      val modelPath = getModelDir(baseDirectory.value, "rpc")
      val modelNames = getAllModelNamesFrom(modelPath, "rpc").mkString(",")
      val classNameValue = (className in routeConfigurationFactoryGen).value
      val content =
        raw"""package $packageNameValue;
@:nativeGen
@:build(com.thoughtworks.microbuilder.core.RouteConfigurationFactory.generateRouteConfigurationFactory([${modelNames}]))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamDeserializer.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamSerializer.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(outgoingProxyFactoryGen.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(routeConfigurationFactoryGen.value)
    }
  )
}
