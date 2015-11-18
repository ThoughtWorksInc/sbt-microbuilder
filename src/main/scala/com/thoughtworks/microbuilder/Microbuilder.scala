package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.{DependencyVersion, HaxeJavaPlugin}
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object Microbuilder extends AutoPlugin {
  override def requires = HaxeJavaPlugin

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
  }) :+
    (libraryDependencies ++= Seq("com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0" % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "json-stream" % "2.0.0",
      "com.qifun" %% "haxe-scala-stm" % "0.1.4" % HaxeJava classifier HaxeJava.name
    ))
}
