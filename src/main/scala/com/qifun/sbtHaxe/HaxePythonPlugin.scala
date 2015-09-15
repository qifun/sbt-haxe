/*
 * sbt-haxe
 * Copyright 2014 深圳岂凡网络有限公司 (Shenzhen QiFun Network Corp., LTD)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qifun.sbtHaxe

import HaxeConfigurations._
import HaxeKeys._
import sbt.Keys._
import sbt._

/**
 * A Plugin used to compile Haxe sources to Python sources.
 */
object HaxePythonPlugin extends AutoPlugin {

  override final def requires = BaseHaxePlugin

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