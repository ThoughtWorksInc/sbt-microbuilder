package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJavaPlugin
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object MicrobuilderPlay extends AutoPlugin {

  override def requires = HaxeJavaPlugin && MicrobuilderCommon

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++
      (for {
        c <- Seq(Compile, Test)
      } yield {
        haxeOptions in c ++= Seq("-dce", "no")
      }) ++
      Seq(
        libraryDependencies += "com.thoughtworks.microbuilder" % "json-stream-core" % DependencyVersions.JsonStreamCore % HaxeJava classifier HaxeJava.name,
        libraryDependencies += "com.thoughtworks.microbuilder" % "json-stream-core" % DependencyVersions.JsonStreamCore,
        libraryDependencies += "com.thoughtworks.microbuilder" %% "microbuilder-play" % DependencyVersions.MicrobuilderPlay,
        libraryDependencies += "com.thoughtworks.microbuilder" % "microbuilder-core" % DependencyVersions.MicrobuilderCore,
        libraryDependencies += "com.thoughtworks.microbuilder" % "microbuilder-core" % DependencyVersions.MicrobuilderCore % HaxeJava classifier HaxeJava.name,
        libraryDependencies += "com.thoughtworks.microbuilder" % "hamu" % DependencyVersions.Hamu % HaxeJava classifier HaxeJava.name,
        libraryDependencies += "com.thoughtworks.microbuilder" % "auto-parser" % DependencyVersions.AutoParser % HaxeJava classifier HaxeJava.name
      )
  }

}
