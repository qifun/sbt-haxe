package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to Neko binary.
 */
object HaxeNekoPlugin extends AutoPlugin {
  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxeNeko, packageBin in HaxeNeko) ++
      inConfig(Neko)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestNeko)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeNeko)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeNeko)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxeNeko)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxeNeko)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxeNeko, Neko) ++
      SbtHaxe.injectSettings(TestHaxeNeko, TestNeko) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Neko).value,
        haxeXmls in Test ++= (haxeXml in TestNeko).value,
        haxePlatformName in Neko := "neko",
        haxePlatformName in TestNeko := "neko",
        haxeOutputPath in Neko := Some(file((target in haxe in Neko).value.getPath + ".n")),
        haxeOutputPath in TestNeko := Some(file((target in haxe in Neko).value.getPath + ".n")),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxeNeko).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxeNeko).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeNeko,
        ivyConfigurations += TestHaxeNeko)
  }
}
