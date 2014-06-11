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
  final val Haxe = config("haxe")
  final val TestHaxe = config("test-haxe")
  final val dox = TaskKey[Seq[File]]("dox", "Generate haxe documentation.")

  override final def globalSettings =
    super.globalSettings ++ Seq(
      haxeCommand := "haxe",
      haxeOptions := Seq())

  /**
   *  默认的[[haxe]]任务设置值。
   *
   *  当编译Haxe时，某些所需的[[sbt.SettingKey]]是Sbt内置设置。
   *  所以这些[[sbt.SettingKey]]会使用`haxeConfiguration`参数传入[[Haxe]]或[[TestHaxe]]作为[[sbt.Configuration]]以便区分。
   *  例如`sourceDirectory`、`dependencyClasspath`等。
   *
   *  另一些[[sbt.SettingKey]]是[[HaxePlugin]]插件专用设置，不会与内置设置同名。
   *  所以这些设置不用自定义的[[sbt.Configuration]]就可以区分，而用`injectConfiguration`参数传入[[Compile]]或[[Test]]作为[[sbt.Configuration]]。
   *  例如`haxeCommand`、`haxeOptions`等。
   *
   *  @param haxeConfiguration 当把Sbt内置[[sbt.SettingKey]]重用于Haxe编译时所需的[[sbt.Configuration]]。
   *  @param injectConfiguration [[HaxePlugin]]插件专有[[sbt.SettingKey]]所用的Sbt内置[[sbt.Configuration]]。
   */
  final def haxeSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxe in injectConfiguration := {
      val includes = (dependencyClasspath in haxeConfiguration).value
      val cache = (streams in haxeConfiguration).value.cacheDirectory

      val cachedTranfer = FileFunction.cached(cache / "haxe", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          val deps = (buildDependencies in haxeConfiguration).value.classpath((thisProjectRef in haxeConfiguration).value)

          val processBuilder =
            Seq[String](
              (haxeCommand in injectConfiguration).value) ++
              parseProjectPathes(haxeConfiguration.name, (baseDirectory { _ / "src" / "haxe" }).value.toString, (sourceDirectory in haxeConfiguration).value.getPath, deps) ++
              parseJarPathes((managedClasspath in Compile).value) ++
              Seq("-java", temporaryDirectory.getPath,
                "-D", "no-compilation") ++
                (haxeOptions in injectConfiguration).value ++
                parseHaxeSource(in, (sourceDirectories in haxeConfiguration).value)
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

  final def doxSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    dox in injectConfiguration := {
      val cache = (streams in haxeConfiguration).value.cacheDirectory
      val deps = (buildDependencies in haxeConfiguration).value.classpath((thisProjectRef in haxeConfiguration).value)

      val cachedTranfer = FileFunction.cached(cache / "dox", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          (streams in haxeConfiguration).value.log.info("Generating haxe document...")

          val processBuilder =
            Seq[String](
              (haxeCommand in injectConfiguration).value,
              "-D", "doc-gen",
              "-xml", (baseDirectory { _ / "target" / "java.xml" }).value.toString,
              "-java", "dummy", "--no-output") ++
              parseProjectPathes(haxeConfiguration.name, (baseDirectory { _ / "src" / "haxe" }).value.toString, (sourceDirectory in haxeConfiguration).value.getPath, deps) ++
              parseJarPathes((managedClasspath in Compile).value) ++
              parseHaxeSource(in, (sourceDirectories in haxeConfiguration).value)

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
        sourceGenerators in Test <+= haxe in Test,
        doxSetting(Haxe, Compile))

  /*
   * Parse the project and sub project's source path.
   */
  private final def parseProjectPathes(
    configurationName: String,
    mainProjectPath: String,
    projectPath: String,
    deps: Seq[sbt.ClasspathDep[sbt.ProjectRef]]) = {
    val dependSources = for {
      ResolvedClasspathDependency(dep: ProjectRef, _) <- deps
    } yield {
      dep match {
        case ProjectRef(path, subProject) => (path.getPath.substring(1) + subProject + "/src/haxe").replace("/", System.getProperty("file.separator"))
        case _ => ""
      }
    }
    // Add the haxe project's source path in order to build the test-haxe
    val testProjectPath: Seq[String] = configurationName match {
      case "test-haxe" => Seq("-cp", mainProjectPath)
      case _ => Seq()
    }

    Seq("-cp", projectPath) ++
      (Seq[String]() /: dependSources)(_ ++ Seq("-cp", _)) ++
      testProjectPath
  }

  /**
   * Parse the path of library dependencies(.jar) 
   */
  private final def parseJarPathes(managedClasspath: Seq[sbt.Attributed[java.io.File]]) = {
    val jarPathes = managedClasspath map {
      jarPath =>
        jarPath.data.toString
    }
    (Seq[String]() /: jarPathes)(_ ++ Seq("-java-lib", _))
  }

  private final def parseHaxeSource(in: Set[File], parents: Seq[sbt.File]) = {
    in.map { file =>
      val relativePaths = for {
        parent <- parents
        relativePath <- file.relativeTo(parent)
      } yield relativePath
      relativePaths match {
        case Seq(relativePath) => relativePath.toString.substring(0, relativePath.toString.lastIndexOf(".")).replace(System.getProperty("file.separator"), ".")
        case Seq() => throw new MessageOnlyException(raw"$file should be in one of source directories!")
        case _ => throw new MessageOnlyException(raw"$file should not be in multiple source directories!")
      }
    }
  }
}
