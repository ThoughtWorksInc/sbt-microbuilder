package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{DependencyVersion, HaxeJavaPlugin}
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object Microbuilder extends AutoPlugin {

  val packageNameValue = "proxy"

  def getModelDir(baseDir: File, subDir: String): File ={
    baseDir/ s"src/haxe/${subDir}"
  }

  def getOutputDir(baseDir: File):File={
    packageNameValue.split('.').foldLeft(baseDir) { (parent, path) =>
      parent / path
    }
  }
  def writeFile(outPutFile: File, content: String)={
    if (!outPutFile.exists || content != IO.read(outPutFile, scala.io.Codec.UTF8.charSet)) {
      IO.write(outPutFile, content, scala.io.Codec.UTF8.charSet)
    }
    outPutFile
  }
  def getAllModelNamesFrom(modelPath: File, packageName: String): Array[String] ={
    modelPath.list.map("\""+packageName+"."+_.replaceFirst("[.][^.]+$", "")+"\"")
  }
  object autoImport {
    val jsonStreamDeserializer = taskKey[File]("Generates deserizlier for models.")
    val jsonStreamSerializer = taskKey[File]("Generates serizlier for models.")
    val outgoingProxyFactoryGen = taskKey[File]("Generates outgoing proxy factory")

    val className = settingKey[String]("Class name of a specific generating class.")
  }

  import autoImport._

  override def requires = HaxeJavaPlugin

  override def globalSettings = Seq(
    className in jsonStreamDeserializer := "MicrobuilderDeserializer" ,
    className in jsonStreamSerializer := "MicrobuilderSerializer",
    className in outgoingProxyFactoryGen := "MicrobuilderOutgoingProxyFactory"
  )


  val haxelibs = Map(
    "continuation" -> DependencyVersion.SpecificVersion("1.3.2")
  )


  override lazy val projectSettings: Seq[Setting[_]] = (for (c <- AllTargetConfigurations ++ AllTestTargetConfigurations) yield {
    haxeOptions in c ++= haxelibOptions(haxelibs)
  }) ++
    (for (c <- AllTargetConfigurations ++ AllTestTargetConfigurations) yield {
    haxeOptions in c ++= Seq("-dce", "no")
  }) ++
    (for (c <- Seq(Compile, Test)) yield {
    haxeOptions in c ++= Seq("-D", "scala")
  }) ++ Seq(
    libraryDependencies ++= Seq("com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0",
      "com.qifun" %% "haxe-scala-stm" % "0.1.4" % HaxeJava classifier HaxeJava.name
    ),
    jsonStreamDeserializer := {
      val modelPath = getModelDir(baseDirectory.value, "model")
      val modelNames = getAllModelNamesFrom(modelPath, "model").mkString(",")
      val packagePath = getOutputDir((sourceManaged in Haxe).value)
      packagePath.mkdirs()
      val classNameValue = (className in jsonStreamDeserializer).value
      val fileName = s"${classNameValue}.hx"
      val outputFile = packagePath / fileName
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonDeserializer.generateDeserializer(["com.thoughtworks.microbuilder.core.Failure",${modelNames}]))
class $classNameValue {}
"""

      writeFile(outputFile, content)
  },
    jsonStreamSerializer := {
      val modelPath = getModelDir(baseDirectory.value, "model")
      val modelNames = getAllModelNamesFrom(modelPath, "model").mkString(",")
      val packagePath = getOutputDir((sourceManaged in Haxe).value)
      packagePath.mkdirs()
      val classNameValue = (className in jsonStreamSerializer).value
      val fileName = s"${classNameValue}.hx"
      val outputFile = packagePath / fileName
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
@:nativeGen
@:build(jsonStream.JsonSerializer.generateSerializer(["com.thoughtworks.microbuilder.core.Failure",${modelNames}]))
class $classNameValue {}
"""

      writeFile(outputFile, content)
    },
    outgoingProxyFactoryGen := {
      val modelPath = getModelDir(baseDirectory.value, "rpc")
      val modelNames = getAllModelNamesFrom(modelPath, "rpc").mkString(",")
      val packagePath = getOutputDir((sourceManaged in Haxe).value)
      packagePath.mkdirs()
      val classNameValue = (className in outgoingProxyFactoryGen).value
      val fileName = s"${classNameValue}.hx"
      val outputFile = packagePath / fileName
      val content =
        raw"""package $packageNameValue;
using jsonStream.Plugins;
using proxy.MicrobuilderDeserializer;
using proxy.MicrobuilderSerializer;
@:nativeGen
@:build(jsonStream.rpc.OutgoingProxyFactory.generateOutgoingProxyFactory([${modelNames}]))
class $classNameValue {}
"""

      writeFile(outputFile, content)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamDeserializer.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamSerializer.value)
    },
    sourceGenerators in Haxe <+= Def.task {
      Seq(outgoingProxyFactoryGen.value)
    }
  )
}
