package com.thoughtworks.microbuilder

import sbt.Keys._
import sbt._
import com.thoughtworks.microbuilder.sbtHaxe.BaseHaxePlugin.autoImport._

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object MicrobuilderJs extends AutoPlugin {

  override def requires = MicrobuilderJsSdk

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++ Seq(
      libraryDependencies += "com.thoughtworks.microbuilder" % "microbuilder-js" % DependencyVersions.MicrobuilderJs % HaxeJs classifier HaxeJs.name
    )
  }


}
