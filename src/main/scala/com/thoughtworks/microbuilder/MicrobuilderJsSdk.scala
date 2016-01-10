package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJsPlugin
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJsNpmPlugin
import sbt.Keys._
import sbt.{AutoPlugin, _}

object MicrobuilderJsSdk extends AutoPlugin {

  override def requires = HaxeJsPlugin && MicrobuilderCommon && HaxeJsNpmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++ Seq(
      libraryDependencies ++= MicrobuilderSettings.haxeDependencies(HaxeJs)
    )
  }
}
