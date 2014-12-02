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
import sbt.plugins.JvmPlugin
import HaxeConfigurations._

/**
 * A plugin that provides the common settings for HaxeJavaPlugin and HaxeCSharpPlugin etc.
 *
 */
final object BaseHaxePlugin extends AutoPlugin {
  override final def requires = JvmPlugin
  override final def trigger = allRequirements

  final object autoImport extends HaxeKeys with HaxeConfigurations
  
  import autoImport._

  override final lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Haxe)(SbtHaxe.baseHaxeSettings) ++
      inConfig(TestHaxe)(SbtHaxe.baseHaxeSettings) ++
      SbtHaxe.docSetting(Haxe, Compile) ++
      SbtHaxe.docSetting(TestHaxe, Test)

  override final def globalSettings =
    super.globalSettings ++ Seq(
      haxeOptions := Nil,
      haxeXmls := Nil,
      haxeCommand := "haxe",
      haxelibCommand := "haxelib")

  final val HaxeUnit = new TestFramework("com.qifun.sbtHaxe.testInterface.HaxeUnitFramework")

  override final def buildSettings =
    super.buildSettings :+ (testFrameworks += HaxeUnit)

}