package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to PHP sources.
 */
object HaxePhpPlugin extends AutoPlugin {
  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxePhp, packageBin in HaxePhp) ++
      inConfig(Php)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestPhp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxePhp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxePhp)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxePhp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxePhp)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxePhp, Php) ++
      SbtHaxe.injectSettings(TestHaxePhp, TestPhp) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Php).value,
        haxeXmls in Test ++= (haxeXml in TestPhp).value,
        haxePlatformName in Php := "php",
        haxePlatformName in TestPhp := "php",
        haxeOutputPath in Php := Some((target in haxe in Php).value),
        haxeOutputPath in TestPhp := Some((target in haxe in Php).value),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxePhp).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxePhp).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxePhp,
        ivyConfigurations += TestHaxePhp)
  }
}