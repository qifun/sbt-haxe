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
import java.io.File

trait HaxeKeys {
  final val haxeOptions = SettingKey[Seq[String]]("haxe-options", "Additional command-line options for Haxe compiler.")
  final val haxeCommand = SettingKey[String]("haxe-command", "The Haxe executable.")
  final val haxelibCommand = SettingKey[String]("haxelib-command", "The haxelib executable")
  final val haxePlatformName = SettingKey[String]("haxe-platform-name", "The name of the haxe platform")
  final val haxe = TaskKey[Seq[File]]("haxe", "Convert Haxe source code to target source code.")
  final val haxeOutputPath = SettingKey[Option[File]]("haxe-output-path", "The path where the Haxe code will be compiled to.")

  final val haxeXmls = TaskKey[Seq[File]]("haxe-xmls", "Generate Haxe xmls.")
  final val doxRegex = TaskKey[Seq[String]]("dox-regex", "The Regex that used to generate Haxe documentation.")
  final val haxeXml = TaskKey[Seq[File]]("haxeXml", "Generate Haxe xml.")

}

final object HaxeKeys extends HaxeKeys