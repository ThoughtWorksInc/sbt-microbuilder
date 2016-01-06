package com.thoughtworks.microbuilder

import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._
import com.thoughtworks.microbuilder.sbtHaxe.HaxeJsPlugin
import sbt.Keys._
import sbt.{AutoPlugin, _}

object MicrobuilderJsSdk extends AutoPlugin {

  override def requires = HaxeJsPlugin && MicrobuilderCommon

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++ Seq(
      libraryDependencies ++= MicrobuilderSettings.haxeDependencies(HaxeJs)
    )
  }
}
