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

import sbt._
import Keys._
import HaxeKeys._
import HaxeConfigurations._
import sbt.AutoPlugin

/**
 * A plugin used to compile Haxe sources to CSharp sources.
 */
final object HaxeCSharpPlugin extends AutoPlugin {
  override final def requires = BaseHaxePlugin
  override final def trigger = allRequirements

  override lazy val projectSettings: Seq[Setting[_]] =
    sbt.addArtifact(artifact in packageBin in HaxeCSharp, packageBin in HaxeCSharp) ++
      inConfig(CSharp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestCSharp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeCSharp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(HaxeCSharp)(SbtHaxe.extendSettings) ++
      inConfig(TestHaxeCSharp)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxeCSharp)(SbtHaxe.extendTestSettings) ++
      SbtHaxe.injectSettings(HaxeCSharp, CSharp) ++
      SbtHaxe.injectSettings(TestHaxeCSharp, TestCSharp) ++
      SbtHaxe.csharpRunSettings(CSharp) ++
      SbtHaxe.csharpRunSettings(TestCSharp) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in CSharp).value,
        haxeXmls in Test ++= (haxeXml in TestCSharp).value,
        haxePlatformName in CSharp := "cs",
        // TODO (haxePlatformName in TestCSharp) should extend from (haxePlatformName in CSharp). 
        // But now it doesn't work.
        haxePlatformName in TestCSharp := "cs",
        haxeOutputPath in CSharp := Some((target in haxe in CSharp).value),
        haxeOutputPath in TestCSharp := Some((target in haxe in CSharp).value),
        doxRegex in Compile := SbtHaxe.buildDoxRegex((sourceDirectories in HaxeCSharp).value),
        doxRegex in Test := SbtHaxe.buildDoxRegex((sourceDirectories in TestHaxeCSharp).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeCSharp,
        ivyConfigurations += TestHaxeCSharp)

}