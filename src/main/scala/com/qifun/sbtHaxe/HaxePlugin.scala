package com.qifun.sbtHaxe

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File
import scala.Some

final object HaxePlugin extends Plugin {
  final val haxeOptions = SettingKey[Seq[String]]("haxe-options", "Additional command-line options for Haxe compiler.")
  final val haxeCommand = SettingKey[String]("haxe-command", "The Haxe executable.")
  final val haxe = TaskKey[Seq[File]]("haxe", "Convert Haxe source code to Java.")
  final val unmanagedInclude = SettingKey[File]("unmanaged-include", "The default directory for manually managed included haxe.")
  final val Haxe = config("haxe")
  final val TestHaxe = config("test-haxe")
  
  override final def globalSettings =
    super.globalSettings ++ Seq(
      haxeCommand := "haxe",
      haxeOptions := Seq())
  
  final def haxeSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxe in injectConfiguration := {
      val includes = (dependencyClasspath in haxeConfiguration).value
      val cache = (cacheDirectory in haxeConfiguration).value
      val cachedTranfer = FileFunction.cached(cache / "haxe", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          val processBuilder =
            Seq[String](
                (haxeCommand in injectConfiguration).value,
                "-cp", (sourceDirectory in haxeConfiguration).value.getPath,
                "-java", temporaryDirectory.getPath,
                "-D", "no-compilation") ++
                (haxeOptions in haxeConfiguration).value ++
                in.map { file =>
                  val relativePaths = for {
                    parent <- (sourceDirectories in haxeConfiguration).value
                    relativePath <- file.relativeTo(parent) 
                  } yield relativePath
                  relativePaths match {
                    case Seq(relativePath) => relativePath.toString.substring(0, relativePath.toString.lastIndexOf(".")).replace(System.getProperty("file.separator"), ".")
                    case Seq() => throw new MessageOnlyException(raw"$file should be in one of source directories!")
                    case _ => throw new MessageOnlyException(raw"$file should not be in multiple source directories!")
                  }
                }
          (streams in haxeConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
          (streams in haxeConfiguration).value.log.info("test: " + update)
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
          exportedProducts := {
            val files = if (exportJars.value) {
              Seq(packageBin.value)
            } else {
              products.value
            }
            for (f <- files) yield {
              Classpaths.analyzed(f, compile.value)
            }
          },
          unmanagedSourceDirectories := Seq(sourceDirectory.value),
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
// vim: et sts=2 sw=2
