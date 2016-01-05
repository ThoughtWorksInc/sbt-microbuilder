package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJsPlugin
import sbt.Keys._
import sbt.{AutoPlugin, _}

object MicrobuilderJs extends AutoPlugin {

  override def requires = HaxeJsPlugin && MicrobuilderCommon

  override lazy val projectSettings: Seq[Setting[_]] = super.projectSettings ++ Seq(
    libraryDependencies += "com.thoughtworks.microbuilder" %% "json-stream" % DependencyVersions.JsonStream % HaxeJs classifier HaxeJs.name,
    libraryDependencies += "com.thoughtworks.microbuilder" %% "microbuilder-core" % DependencyVersions.MicrobuilderCore % HaxeJs classifier HaxeJs.name,
//    libraryDependencies += "com.thoughtworks.microbuilder" %% "microbuilder-js" % DependencyVersions.MicrobuilderJs % HaxeJs classifier HaxeJs.name,
    libraryDependencies += "com.thoughtworks.microbuilder" % "hamu" % DependencyVersions.Hamu % HaxeJs classifier HaxeJs.name,
    libraryDependencies += "com.thoughtworks.microbuilder" % "auto-parser" % DependencyVersions.AutoParser % HaxeJs classifier HaxeJs.name
  )
}
