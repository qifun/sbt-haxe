package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to JavaScript sources.
 */
object HaxeJsPlugin extends AutoPlugin {
  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxeJs, packageBin in HaxeJs) ++
      inConfig(Js)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestJs)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeJs)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeJs)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxeJs)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxeJs)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxeJs, Js) ++
      SbtHaxe.injectSettings(TestHaxeJs, TestJs) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Js).value,
        haxeXmls in Test ++= (haxeXml in TestJs).value,
        haxePlatformName in Js := "js",
        haxePlatformName in TestJs := "js",
        haxeOutputPath in Js := Some((target in haxe in Js).value),
        haxeOutputPath in TestJs := Some((target in haxe in Js).value),
        haxeOutputExtension in Js := Some(".js"),
        haxeOutputExtension in TestJs := Some(".js"),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxeJs).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxeJs).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeJs,
        ivyConfigurations += TestHaxeJs)
  }

}
