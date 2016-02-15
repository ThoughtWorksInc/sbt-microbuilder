package com.thoughtworks.microbuilder.sbtMicrobuilder

import sbt.Keys._
import sbt._

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
object MicrobuilderPlay extends AutoPlugin {

  override def requires = MicrobuilderJavaSdk

  override lazy val projectSettings: Seq[Setting[_]] = {
    super.projectSettings ++ Seq(
      libraryDependencies += "com.thoughtworks.microbuilder" %% "microbuilder-play" % DependencyVersions.MicrobuilderPlay
    )
  }


}
