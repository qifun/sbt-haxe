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

import sbt.AutoPlugin

object AllHaxePlugins extends AutoPlugin {

  override final def requires = {
    HaxeAs3Plugin && HaxeCppPlugin && HaxeCSharpPlugin && HaxeFlashPlugin && HaxeJavaPlugin && HaxeJsPlugin && HaxeNekoPlugin && HaxePhpPlugin && HaxePythonPlugin
  }

}
