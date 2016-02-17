package com.thoughtworks.microbuilder.sbtMicrobuilder

import java.net.URLClassLoader

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{BaseHaxePlugin, DependencyVersion}
import sbt.Keys._
import sbt._

import scala.util.parsing.json.JSONArray

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object MicrobuilderCommon extends AutoPlugin {

  override def requires = BaseHaxePlugin

  private def writeFile(outPutFile: File, content: String) = {
    if (!outPutFile.exists || content != IO.read(outPutFile, scala.io.Codec.UTF8.charSet)) {
      IO.write(outPutFile, content, scala.io.Codec.UTF8.charSet)
    }
    outPutFile
  }

  private def genFileForModel(outputBaseDir: File, packageName: String, outputFileName: String, content: String): File = {
    val relativeDirectory = packageName.replace('.', '/')
    val packagePath = outputBaseDir / relativeDirectory
    packagePath.mkdirs()
    val outputFile = packagePath / s"${outputFileName}.hx"
    writeFile(outputFile, content)
  }

  object autoImport {

    val jsonStreamServiceModules = taskKey[Seq[String]]("Haxe modules that contains service definitions for json-stream.")
    val jsonStreamModelModules = taskKey[Seq[String]]("Haxe modules that contains data structures for json-stream.")

    val makeSwaggerSchemaJson = taskKey[File]("Generates swaggerSchema.json.")

    val jsonStreamSwaggerExporter = taskKey[File]("Generates swagger exporter for json-stream.")
    val jsonStreamDeserializer = taskKey[File]("Generates deserizlier for models.")
    val jsonStreamSerializer = taskKey[File]("Generates serizlier for models.")
    val incomingProxyFactory = taskKey[File]("Generates incoming proxy factory")
    val outgoingProxyFactory = taskKey[File]("Generates outgoing proxy factory")
    val routeConfigurationFactory = taskKey[File]("Generates route configuration factory")

    val packageName = settingKey[String]("Package name of a specific generating class.")
    val className = settingKey[String]("Class name of a specific generating class.")

    def microbuilderHaxeDependencies(configuration: Configuration) = {
      MicrobuilderSettings.haxeDependencies(configuration)
    }

  }

  import autoImport._

  override def globalSettings = Seq(
    className in jsonStreamDeserializer := "MicrobuilderDeserializer",
    className in jsonStreamSerializer := "MicrobuilderSerializer",
    className in jsonStreamSwaggerExporter := "JsonStreamSwaggerExporter",
    className in incomingProxyFactory := "MicrobuilderIncomingProxyFactory",
    className in outgoingProxyFactory := "MicrobuilderOutgoingProxyFactory",
    className in routeConfigurationFactory := "MicrobuilderRouteConfigurationFactory"
  )

  val haxelibs = Map(
    "microbuilder-HUGS" -> DependencyVersion.SpecificVersion("2.0.1"),
    "continuation" -> DependencyVersion.SpecificVersion("1.3.2")
  )

  private val HaxeFileRegex = """(.+)\.hx""".r
  private val DashedCharacterRegex = """-([a-z])""".r

  override lazy val projectSettings: Seq[Setting[_]] = (for (c <- AllTargetConfigurations ++ AllTestTargetConfigurations) yield {
    haxeOptions in c ++= haxelibOptions(haxelibs)
  }) ++
    (for (c <- Seq(Compile, Test)) yield {
      haxeOptions in c ++= Seq("-D", "scala")
    }) ++ Seq(
    packageName := {
      DashedCharacterRegex.replaceAllIn(s"${organization.value}.${name.value}", { matched =>
        matched.group(1).toUpperCase
      })
    },
    packageName in jsonStreamServiceModules := s"${packageName.value}.rpc",
    packageName in jsonStreamModelModules := s"${packageName.value}.model",
    packageName in jsonStreamSwaggerExporter := s"${packageName.value}.swagger",
    packageName in jsonStreamDeserializer := s"${packageName.value}.proxy",
    packageName in jsonStreamSerializer := s"${packageName.value}.proxy",
    packageName in incomingProxyFactory := s"${packageName.value}.proxy",
    packageName in outgoingProxyFactory := s"${packageName.value}.proxy",
    packageName in routeConfigurationFactory := s"${packageName.value}.proxy",
    haxelibDependencies ++= haxelibs,
    haxelibDependencies += "microbuilder-core" -> DependencyVersion.SpecificVersion(DependencyVersions.MicrobuilderCore),
    target in makeSwaggerSchemaJson := crossTarget.value / "swagger" / "swaggerSchema.json",
    makeSwaggerSchemaJson := {
      val outputFile = (target in makeSwaggerSchemaJson).value
      outputFile.getParentFile.mkdirs()

      val urls = (for {
        path <- (fullClasspath in Compile).value
      } yield path.data.toURI.toURL) (collection.breakOut(Array.canBuildFrom))

      val packageNameValue = (packageName in jsonStreamSwaggerExporter).value
      val classNameValue = (className in jsonStreamSwaggerExporter).value

      IO.write(
        outputFile,
        new URLClassLoader(urls).loadClass(s"$packageNameValue.$classNameValue").getMethod("export").invoke(null).asInstanceOf[String],
        scala.io.Codec.UTF8.charSet
      )
      outputFile
    },
    jsonStreamModelModules := {
      val packageNameValue = (packageName in jsonStreamModelModules).value
      val modelRelativeDirectory = packageNameValue.replace('.', '/')
      for {
        haxeSourceDirectory <- (sourceDirectories in Haxe).value
        modelDirectoy = haxeSourceDirectory / modelRelativeDirectory
        if modelDirectoy.exists
        HaxeFileRegex(moduleName) <- modelDirectoy.list
      } yield
        raw"""$packageNameValue.$moduleName"""
    },
    jsonStreamServiceModules := {
      val packageNameValue = (packageName in jsonStreamServiceModules).value
      val modelRelativeDirectory = packageNameValue.replace('.', '/')
      for {
        haxeSourceDirectory <- (sourceDirectories in Haxe).value
        modelDirectoy = haxeSourceDirectory / modelRelativeDirectory
        if modelDirectoy.exists
        HaxeFileRegex(moduleName) <- modelDirectoy.list
      } yield
        raw"""$packageNameValue.$moduleName"""
    },
    jsonStreamSwaggerExporter := {
      val packageNameValue = (packageName in jsonStreamSwaggerExporter).value
      val packagePath = packageNameValue.split('.').foldLeft((sourceManaged in Haxe).value) { (parent, path) =>
        parent / path
      }
      packagePath.mkdirs()
      val classNameValue = (className in jsonStreamSwaggerExporter).value
      val fileName = s"$classNameValue.hx"
      val outputFile = packagePath / fileName
      val content =
        raw"""package $packageNameValue;
import haxe.io.Bytes;
import haxe.format.JsonPrinter;
import jsonStream.SwaggerExporter;
using jsonStream.SwaggerPlugins;
@:final
@:nativeGen
class $classNameValue {
  public static function export():String return {
    var schemaJson = SwaggerExporter.export(${jsonStreamModelModules.value.mkString("[\"", "\",\"", "\"]")});
    JsonPrinter.print(schemaJson, null, "\t");
  }
}
"""
      if (!outputFile.exists || content != IO.read(outputFile, scala.io.Codec.UTF8.charSet)) {
        IO.write(outputFile, content, scala.io.Codec.UTF8.charSet)
      }
      outputFile
    },
    jsonStreamDeserializer := {
      val modelJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamModelModules.value.toList)
      val packageNameValue = (packageName in jsonStreamDeserializer).value
      val classNameValue = (className in jsonStreamDeserializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonDeserializer.generateDeserializer($modelJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, packageNameValue, classNameValue, content)
    },
    jsonStreamSerializer := {
      val modelJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamModelModules.value.toList)
      val packageNameValue = (packageName in jsonStreamSerializer).value
      val classNameValue = (className in jsonStreamSerializer).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonSerializer.generateSerializer($modelJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, packageNameValue, classNameValue, content)
    },
    outgoingProxyFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val packageNameValue = (packageName in outgoingProxyFactory).value
      val classNameValue = (className in outgoingProxyFactory).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using ${(packageName in jsonStreamDeserializer).value}.${(className in jsonStreamDeserializer).value};
using ${(packageName in jsonStreamSerializer).value}.${(className in jsonStreamSerializer).value};
@:expose
@:nativeGen
@:build(jsonStream.rpc.OutgoingProxyFactory.generateOutgoingProxyFactory($rpcJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, packageNameValue, classNameValue, content)
    },
    incomingProxyFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val packageNameValue = (packageName in incomingProxyFactory).value
      val classNameValue = (className in incomingProxyFactory).value
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using ${(packageName in jsonStreamDeserializer).value}.${(className in jsonStreamDeserializer).value};
using ${(packageName in jsonStreamSerializer).value}.${(className in jsonStreamSerializer).value};
@:expose
@:nativeGen
@:build(jsonStream.rpc.IncomingProxyFactory.generateIncomingProxyFactory($rpcJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, packageNameValue, classNameValue, content)
    },
    routeConfigurationFactory := {
      val rpcJson = JSONArray("com.thoughtworks.microbuilder.core.Failure" :: jsonStreamServiceModules.value.toList)
      val packageNameValue = (packageName in routeConfigurationFactory).value
      val classNameValue = (className in routeConfigurationFactory).value
      val content =
        raw"""package $packageNameValue;
@:expose
@:nativeGen
@:build(com.thoughtworks.microbuilder.core.RouteConfigurationFactory.generateRouteConfigurationFactory($rpcJson))
class $classNameValue {}
"""
      genFileForModel((sourceManaged in Haxe).value, packageNameValue, classNameValue, content)
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
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamSwaggerExporter.value)
    }
  )
}
