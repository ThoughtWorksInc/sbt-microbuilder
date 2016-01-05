package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJavaPlugin
import sbt.AutoPlugin
import sbt._
import sbt.Keys._

object MicrobuilderPlay extends AutoPlugin {

  override def requires = HaxeJavaPlugin && MicrobuilderCommon

  override lazy val projectSettings: Seq[Setting[_]] = super.projectSettings ++ Seq(
    libraryDependencies ++= Seq("com.thoughtworks.microbuilder" %% "json-stream" % DependencyVersions.JsonStream % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "json-stream" % DependencyVersions.JsonStream,
      "com.qifun" %% "haxe-scala-stm" % DependencyVersions.HaxeScalaStm % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" %% "microbuilder-play" % DependencyVersions.MicrobuilderPlay,
      "com.thoughtworks.microbuilder" %% "microbuilder-core" % DependencyVersions.MicrobuilderCore,
      "com.thoughtworks.microbuilder" %% "microbuilder-core" % DependencyVersions.MicrobuilderCore % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" % "hamu" % DependencyVersions.Hamu % HaxeJava classifier HaxeJava.name,
      "com.thoughtworks.microbuilder" % "auto-parser" % DependencyVersions.AutoParser % HaxeJava classifier HaxeJava.name
    ))

}
