package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{DependencyVersion, HaxeJavaPlugin}
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object Microbuilder extends AutoPlugin {

  object autoImport {
    val jsonStreamDeserializer = taskKey[File]("Generates deserizlier for model.")

    val className = settingKey[String]("Class name of a specific generating class.")
  }

  import autoImport._

  override def requires = HaxeJavaPlugin

  override def globalSettings = Seq(
    className in jsonStreamDeserializer := "MicrobuilderDeserializer"
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
      val modelPath = baseDirectory.value / "src/haxe/model";
      val modelNames = modelPath.list.map("\"model."+_.replaceFirst("[.][^.]+$", "")+"\"").mkString(",")
      val packageNameValue = "proxy"
      val packagePath = packageNameValue.split('.').foldLeft((sourceManaged in Haxe).value) { (parent, path) =>
        parent / path
      }
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

      if (!outputFile.exists || content != IO.read(outputFile, scala.io.Codec.UTF8.charSet)) {
        IO.write(outputFile, content, scala.io.Codec.UTF8.charSet)
      }

      outputFile
  }, sourceGenerators in Haxe <+= Def.task {
      Seq(jsonStreamDeserializer.value)
    }
  )
}
