package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{DependencyVersion, HaxeJavaPlugin}
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

import scala.util.parsing.json.JSONArray

object Microbuilder extends AutoPlugin {

  val packageNameValue = "proxy"

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

  def genFileForModel(outputBaseDir: File, outputFileName: String, content: String): File = {
    val packagePath = getOutputDir(outputBaseDir)
    packagePath.mkdirs()
    val outputFile = packagePath / s"${outputFileName}.hx"
    writeFile(outputFile, content)
  }

  object autoImport {

    val jsonStreamServiceModules = settingKey[Seq[String]]("Haxe modules that contains data structures for json-stream.")
    val jsonStreamModelModules = settingKey[Seq[String]]("Haxe modules that contains data structures for json-stream.")

    val jsonStreamDeserializer = taskKey[File]("Generates deserizlier for models.")
    val jsonStreamSerializer = taskKey[File]("Generates serizlier for models.")
    val incomingProxyFactory = taskKey[File]("Generates incoming proxy factory")
    val outgoingProxyFactory = taskKey[File]("Generates outgoing proxy factory")
    val routeConfigurationFactory = taskKey[File]("Generates route configuration factory")

    val className = settingKey[String]("Class name of a specific generating class.")
  }

  import autoImport._

  override def requires = HaxeJavaPlugin

  override def globalSettings = Seq(
    className in jsonStreamDeserializer := "MicrobuilderDeserializer",
    className in jsonStreamSerializer := "MicrobuilderSerializer",
    className in incomingProxyFactory := "MicrobuilderIncomingProxyFactory",
    className in outgoingProxyFactory := "MicrobuilderOutgoingProxyFactory",
    className in routeConfigurationFactory := "MicrobuilderRouteConfigurationFactory"
  )


  val haxelibs = Map(
    "continuation" -> DependencyVersion.SpecificVersion("1.3.2")
  )


  private val HaxeFileRegex = """(.+)\.hx""".r

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
    jsonStreamModelModules := {
      for  {
        haxeSourceDirectory <- (sourceDirectories in Haxe).value
        modelDirectoy = (haxeSourceDirectory / "model")
        if modelDirectoy.exists
        HaxeFileRegex(moduleName) <- modelDirectoy.list
      } yield raw"""model.$moduleName"""
    },
    jsonStreamServiceModules := {
      for  {
        haxeSourceDirectory <- (sourceDirectories in Haxe).value
        modelDirectoy = (haxeSourceDirectory / "rpc")
        if modelDirectoy.exists
        HaxeFileRegex(moduleName) <- modelDirectoy.list
      } yield raw"""rpc.$moduleName"""
    },
    jsonStreamDeserializer := {
      val modelJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamModelModules.value.toList)
      val classNameValue = (className in jsonStreamDeserializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonDeserializer.generateDeserializer($modelJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    jsonStreamSerializer := {
      val modelJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamModelModules.value.toList)
      val classNameValue = (className in jsonStreamSerializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonSerializer.generateSerializer($modelJson))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    outgoingProxyFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val classNameValue = (className in outgoingProxyFactory).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using proxy.MicrobuilderDeserializer;
using proxy.MicrobuilderSerializer;
@:nativeGen
@:build(jsonStream.rpc.OutgoingProxyFactory.generateOutgoingProxyFactory($rpcJson))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    incomingProxyFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val classNameValue = (className in incomingProxyFactory).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using proxy.MicrobuilderDeserializer;
using proxy.MicrobuilderSerializer;
@:nativeGen
@:build(jsonStream.rpc.IncomingProxyFactory.generateIncomingProxyFactory($rpcJson))
class $classNameValue {}
"""

      genFileForModel((sourceManaged in Haxe).value, classNameValue, content)
    },
    routeConfigurationFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val classNameValue = (className in routeConfigurationFactory).value
      val content =
        raw"""package $packageNameValue;
@:nativeGen
@:build(com.thoughtworks.microbuilder.core.RouteConfigurationFactory.generateRouteConfigurationFactory($rpcJson))
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
      Seq(outgoingProxyFactory.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(incomingProxyFactory.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(routeConfigurationFactory.value)
    }
  )
}
