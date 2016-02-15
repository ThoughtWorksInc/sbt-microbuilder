package com.thoughtworks.microbuilder.sbtMicrobuilder

import sbt._

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
private[microbuilder] object MicrobuilderSettings {

  def haxeDependencies(haxeConfiguration: Configuration) = Seq(
    "com.thoughtworks.microbuilder" % "json-stream-core" % DependencyVersions.JsonStreamCore % haxeConfiguration classifier haxeConfiguration.name,
    "com.thoughtworks.microbuilder" % "microbuilder-core" % DependencyVersions.MicrobuilderCore % haxeConfiguration classifier haxeConfiguration.name,
    "com.thoughtworks.microbuilder" % "hamu" % DependencyVersions.Hamu % haxeConfiguration classifier haxeConfiguration.name,
    "com.thoughtworks.microbuilder" % "auto-parser" % DependencyVersions.AutoParser % haxeConfiguration classifier haxeConfiguration.name
  )

}
