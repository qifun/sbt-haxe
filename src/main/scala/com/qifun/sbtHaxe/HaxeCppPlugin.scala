package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to C++ sources.
 */
object HaxeCppPlugin extends AutoPlugin {
  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxeCpp, packageBin in HaxeCpp) ++
      inConfig(Cpp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestCpp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeCpp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeCpp)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxeCpp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxeCpp)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxeCpp, Cpp) ++
      SbtHaxe.injectSettings(TestHaxeCpp, TestCpp) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Cpp).value,
        haxeXmls in Test ++= (haxeXml in TestCpp).value,
        haxePlatformName in Cpp := "cpp",
        haxePlatformName in TestCpp := "cpp",
        haxeOutputPath in Cpp := Some((target in haxe in Cpp).value),
        haxeOutputPath in TestCpp := Some((target in haxe in Cpp).value),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxeCpp).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxeCpp).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeCpp,
        ivyConfigurations += TestHaxeCpp)
  }
}
