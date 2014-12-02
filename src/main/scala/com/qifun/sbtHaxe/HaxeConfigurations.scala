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

trait HaxeConfigurations {
  final lazy val Haxe = config("haxe")
  final lazy val TestHaxe = config("test-haxe") extend Haxe
  
  final lazy val HaxeJava = config("haxe-java") extend Haxe
  final lazy val TestHaxeJava = config("test-haxe-java") extend HaxeJava
  
  final lazy val CSharp = config("csharp")
  final lazy val TestCSharp = config("test-csharp") extend CSharp
  
  final lazy val HaxeCSharp = config("haxe-csharp") extend Haxe
  final lazy val TestHaxeCSharp = config("test-haxe-csharp") extend HaxeCSharp

}

final object HaxeConfigurations extends HaxeConfigurations