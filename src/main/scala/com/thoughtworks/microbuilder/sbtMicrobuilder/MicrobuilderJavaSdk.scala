package com.thoughtworks.microbuilder.sbtMicrobuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJavaPlugin
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object MicrobuilderJavaSdk extends AutoPlugin {

  override def requires = HaxeJavaPlugin && MicrobuilderCommon

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++
      (for {
        c <- Seq(Compile, Test)
      } yield {
        haxeOptions in c ++= Seq("-dce", "no")
      }) ++
      Seq(
        libraryDependencies ++= MicrobuilderSettings.haxeDependencies(HaxeJava),
        libraryDependencies += "com.thoughtworks.microbuilder" % "json-stream-core" % DependencyVersions.JsonStreamCore,
        libraryDependencies += "com.thoughtworks.microbuilder" % "microbuilder-core" % DependencyVersions.MicrobuilderCore
      )
  }

}
