package com.qifun.sbtHaxe

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File
import scala.Some

final object HaxePlugin extends Plugin {
  final val haxeCommand = SettingKey[String]("haxe-command", "haxe executable")
  final val haxe = TaskKey[Seq[File]]("haxe", "Convert haxe source code to java.")
  final val Haxe = config("haxe")
  final val TestHaxe = config("test-haxe")

  override final def globalSettings =
    super.globalSettings :+ (haxeCommand := "haxe")

  final def haxeSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxe in injectConfiguration := {
      val includes = (dependencyClasspath in haxeConfiguration).value
      val cache = (cacheDirectory in haxeConfiguration).value
      val projectRef = (thisProjectRef in haxeConfiguration).value
      val deps = (buildDependencies in haxeConfiguration).value
      val data = (settingsData in haxeConfiguration).value

      val cachedTranfer = FileFunction.cached(cache / "haxe", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          val processBuilder =
            Seq(
              (haxeCommand in injectConfiguration).value,
              "-cp", (sourceDirectory.value / "haxe").getPath,
              "-java", temporaryDirectory.getPath,
              "-D", "no-compilation") ++
              in.map { file =>
                (file.relativeTo(sourceDirectory.value / "haxe")) match {
                  case Some(relativePath: File) => relativePath.toString.substring(0, relativePath.toString.lastIndexOf(".")).replace(System.getProperty("file.separator"), ".")
                  case _ => ""
                }
              }

          (streams in haxeConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
          processBuilder !< (streams in haxeConfiguration).value.log match {
            case 0 => {
              val moveMapping = (temporaryDirectory ** globFilter("*.java")) x {
                _.relativeTo(temporaryDirectory).map {
                  (sourceManaged in injectConfiguration).value / _.getPath
                }
              }
              IO.move(moveMapping)
              moveMapping.map { _._2 }(collection.breakOut)
            }
            case result => {
              throw new MessageOnlyException("haxe returns " + result)
            }
          }
        }
      }
      cachedTranfer((sources in haxeConfiguration).value.toSet).toSeq
    }
  }

  final val baseHaxeSettings =
    Defaults.configTasks ++
      Defaults.configPaths ++
      Classpaths.configSettings ++
      Defaults.packageTaskSettings(
        packageBin,
        Defaults.concatMappings(Defaults.sourceMappings,
          unmanagedClasspath map { cp =>
            for {
              attributedPath <- cp
              path = attributedPath.data
              f <- path.***.get
              r <- f.relativeTo(path)
            } yield f -> r.getPath
          })) ++
        Seq(
          exportedProducts <<=
            (products.task, packageBin.task, exportJars, compile) flatMap { (psTask, pkgTask, useJars, analysis) =>
              (if (useJars) Seq(pkgTask).join else psTask) map { _ map { f => Classpaths.analyzed(f, analysis) } }
            },
          unmanagedSourceDirectories <<= sourceDirectory { Seq(_) },
          includeFilter in unmanagedSources := "*.hx")

  final val haxeSettings =
    sbt.addArtifact(artifact in packageBin in Haxe, packageBin in Haxe) ++
      inConfig(Haxe)(baseHaxeSettings) ++
      inConfig(TestHaxe)(baseHaxeSettings) ++
      Seq(
        ivyConfigurations += Haxe,
        haxeSetting(Haxe, Compile),
        sourceGenerators in Compile <+= haxe in Compile,
        ivyConfigurations += TestHaxe,
        haxeSetting(TestHaxe, Test),
        sourceGenerators in Test <+= haxe in Test)
}
