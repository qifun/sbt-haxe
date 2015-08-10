package com.qifun.sbtHaxe

import com.qifun.sbtHaxe.HaxeConfigurations._
import com.qifun.sbtHaxe.HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to Python sources.
 */
object HaxeJavascriptPlugin extends AutoPlugin {
  override final def requires = BaseHaxePlugin

  override final def trigger = allRequirements

  override final lazy val projectSettings: Seq[Setting[_]] = {
    sbt.addArtifact(artifact in packageBin in HaxeJavascript, packageBin in HaxeJavascript) ++
      inConfig(HaxeJavascript)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeJavascript)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxeJavascript)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxeJavascript)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxeJavascript, Compile) ++
      SbtHaxe.injectSettings(TestHaxeJavascript, Test) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Compile).value,
        haxeXmls in Test ++= (haxeXml in Test).value,
        haxePlatformName in Compile := "js",
        haxeOutputPath in Compile := None,
        haxeOptions in Compile ++= Seq("-D", "js-flatten"),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxeJavascript).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxeJavascript).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeJavascript,
        ivyConfigurations += TestHaxeJavascript)
  }

}
