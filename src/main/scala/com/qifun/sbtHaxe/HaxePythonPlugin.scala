package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to Python sources.
 */
object HaxePythonPlugin extends AutoPlugin {
  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxePython, packageBin in HaxePython) ++
      inConfig(Python)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestPython)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxePython)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxePython)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxePython)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxePython)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxePython, Python) ++
      SbtHaxe.injectSettings(TestHaxePython, TestPython) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Python).value,
        haxeXmls in Test ++= (haxeXml in TestPython).value,
        haxePlatformName in Python := "python",
        haxePlatformName in TestPython := "python",
        haxeOutputPath in Python := Some(file((target in haxe in Python).value.getPath + ".py")),
        haxeOutputPath in TestPython := Some(file((target in haxe in Python).value.getPath + ".py")),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxePython).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxePython).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxePython,
        ivyConfigurations += TestHaxePython)
  }
}