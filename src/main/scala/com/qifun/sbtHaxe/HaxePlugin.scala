package com.qifun.sbtHaxe

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File
import java.nio.file.Path
import scala.Some

final object HaxePlugin extends Plugin {

  final val Haxe = config("haxe")
  final val TestHaxe = config("test-haxe")

  final val haxeOptions = SettingKey[Seq[String]]("haxe-options", "Additional command-line options for Haxe compiler.")
  final val haxeCommand = SettingKey[String]("haxe-command", "The Haxe executable.")
  final val haxelibCommand = SettingKey[String]("haxeilb-command", "The haxelib executable")
  final val haxe = TaskKey[Seq[File]]("haxe", "Convert Haxe source code to Java.")
  final val dox = TaskKey[Seq[File]]("dox", "Generate Haxe documentation.")
  final val doxPlatforms = SettingKey[Seq[String]]("dox-platforms", "The platforms that Haxe documentation is generated for.")

  override final def globalSettings =
    super.globalSettings ++ Seq(
      doxPlatforms := Seq("java"),
      haxeCommand := "haxe",
      haxelibCommand := "haxelib",
      haxeOptions := Seq())

  /**
   *  默认的[[haxe]]任务设置值。
   *
   *  当编译Haxe时，某些所需的`SettingKey`是Sbt内置设置。
   *  所以这些`SettingKey`会使用`haxeConfiguration`参数传入[[Haxe]]或[[TestHaxe]]作为`Configuration`以便区分。
   *  例如`sourceDirectory`、`dependencyClasspath`等。
   *
   *  另一些`SettingKey`是[[HaxePlugin]]插件专用设置，不会与内置设置同名。
   *  所以这些设置不用自定义的`Configuration`就可以区分，而用`injectConfiguration`参数传入`Compile`或`Test`作为`Configuration`。
   *  例如[[haxeCommand]]、[[haxeOptions]]等。
   *
   *  @param haxeConfiguration 当把Sbt内置`SettingKey`重用于Haxe编译时所需的`Configuration`。
   *  @param injectConfiguration [[HaxePlugin]]插件专有`SettingKey`所用的Sbt内置`Configuration`。
   */
  final def haxeSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxe in injectConfiguration := {
      val includes = (dependencyClasspath in haxeConfiguration).value
      val cache = (streams in haxeConfiguration).value
      val data = (settingsData in haxeConfiguration).value
      val target = (crossTarget in haxeConfiguration).value
      val managedFiles = (managedClasspath in injectConfiguration).value

      val cachedTranfer = FileFunction.cached(cache.cacheDirectory / "haxe", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          val deps = (buildDependencies in haxeConfiguration).value.classpath((thisProjectRef in haxeConfiguration).value)

          (streams in haxeConfiguration).value.log.info(".........." + (managedClasspath in Compile).value)
          val processBuilder =
            Seq[String](
              (haxeCommand in injectConfiguration).value) ++
              Seq("-cp", (sourceDirectory in haxeConfiguration).value.getPath.toString) ++
              projectPathFlags((sourceDirectory in Haxe).value.toString, data, deps, haxeConfiguration == Haxe, cache, target, managedFiles) ++
              (for (path <- (managedClasspath in injectConfiguration).value) yield Seq("-java-lib", path.data.toString)).flatten ++
              Seq("-java", temporaryDirectory.getPath,
                "-D", "no-compilation") ++
                (haxeOptions in injectConfiguration in haxe).value ++
                haxeSources(in, (sourceDirectories in haxeConfiguration).value)
          (streams in haxeConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
          val sourceManagedValue = (sourceManaged in injectConfiguration).value
          val logger = (streams in haxeConfiguration).value.log
          processBuild(processBuilder, temporaryDirectory, sourceManagedValue, logger)
        }
      }
      cachedTranfer((sources in haxeConfiguration).value.toSet).toSeq
    }
  }

  final def doxSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    dox in injectConfiguration := {
      val cache = (streams in haxeConfiguration).value
      val deps = (buildDependencies in haxeConfiguration).value.classpath((thisProjectRef in haxeConfiguration).value)
      val sourceDir = (sourceDirectory in Haxe).value
      val data = (settingsData in haxeConfiguration).value
      val target = (crossTarget in haxeConfiguration).value
      val managedFiles = (managedClasspath in injectConfiguration).value

      val cachedTranfer = FileFunction.cached(cache.cacheDirectory / "dox", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
        IO.withTemporaryDirectory { temporaryDirectory =>
          (streams in haxeConfiguration).value.log.info("Generating haxe document...")

          val logger = (streams in haxeConfiguration).value.log
          val sourceManagedValue = (sourceManaged in injectConfiguration).value
          for (doxPlatform <- (doxPlatforms in injectConfiguration).value) {
            val processBuilderXml =
              Seq[String](
                (haxeCommand in injectConfiguration).value,
                "-D", "doc-gen",
                "-xml", ((crossTarget in haxeConfiguration).value / raw"$doxPlatform.xml").toString,
                raw"-$doxPlatform", "dummy", "--no-output") ++
                (haxeOptions in injectConfiguration in dox).value ++
                Seq("-cp", (sourceDirectory in haxeConfiguration).value.getPath.toString) ++
                projectPathFlags((sourceDirectory in Haxe).value.toString, data, deps, haxeConfiguration == Haxe, cache, target, managedFiles) ++
                (for (path <- (managedClasspath in injectConfiguration).value) yield Seq("-java-lib", path.data.toString)).flatten ++
                haxeSources(in, (sourceDirectories in haxeConfiguration).value)
            (streams in haxeConfiguration).value.log.info(processBuilderXml.mkString("\"", "\" \"", "\""))
            processBuild(processBuilderXml, temporaryDirectory, sourceManagedValue, logger)
          }

          val processBuildDoc =
            Seq[String](
              (haxelibCommand in injectConfiguration).value,
              "run", "dox", "--input-path", (crossTarget in haxeConfiguration).value.toString,
              "--output-path", ((crossTarget in haxeConfiguration).value / "doc").toString) ++
              haxelibIncludeFlags(sourceDir, sourceDir)
          (streams in haxeConfiguration).value.log.info(processBuildDoc.mkString("\"", "\" \"", "\""))

          processBuild(processBuildDoc, temporaryDirectory, sourceManagedValue, logger)
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
          managedClasspath <<= (configuration, classpathTypes, update) map {
            (config: Configuration, jarTypes: Set[String], up: UpdateReport) =>
              up.filter(configurationFilter(config.name) && artifactFilter(classifier = config.name)).toSeq.map {
                case (conf, module, art, file) => {
                  Attributed(file)(AttributeMap.empty.put(artifact.key, art).put(moduleID.key, module).put(configuration.key, config))
                }
              }.distinct
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

  /**
   * Parse the project and sub project's source path.
   */
  private final def projectPathFlags(
    mainSourcePath: String,
    data: Settings[Scope],
    deps: Seq[ClasspathDep[sbt.ProjectRef]],
    isMain: Boolean,
    cache: TaskStreams,
    targetDirectory: RichFile,
    managedFiles: Seq[Attributed[File]]) = {
    val dependSources = ((for {
      ResolvedClasspathDependency(dep, _) <- deps
      sourcePath <- (sourceDirectory in (dep, Haxe)).get(data)
    } yield Seq("-cp", sourcePath.getPath.toString)) flatten)

    val unpack = FileFunction.cached(cache.cacheDirectory / "unpacked_haxe", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { haxeJars: Set[File] =>
      for {
        haxeJar <- haxeJars
        output <- IO.unzip(haxeJar, targetDirectory / "unpacked_haxe")
      } yield output
    }
    val (unpacking, rawIncludes) = managedFiles.partition { _.data.getName.endsWith("-haxe.jar") }
    val unpacked = unpack(unpacking.map { _.data }(collection.breakOut))
    val unpackedHaxe = if (unpacked.isEmpty) {
      Nil
    } else {
      Seq("-cp", (targetDirectory / "unpacked_haxe").getPath)
    }

    if (isMain)
      dependSources ++ unpackedHaxe
    else
      Seq("-cp", mainSourcePath) ++ dependSources ++ unpackedHaxe
  }

  private final def haxeSources(in: Set[File], parents: Seq[sbt.File]) = {
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

  private final def haxelibIncludeFlags(projectSource: File, source: File): Seq[String] = {
    source.listFiles partition (_.isDirectory) match {
      case (directories, files) => {
        val fileCount = files.count(_ => true)
        val directoryCount = directories.count(_ => true)
        val fileSeparator = System.getProperty("file.separator")
        if (directoryCount == 0 && fileCount == 0) {
          Seq()
        } else {

          (if (fileCount > 0 && (files exists { _.getPath.toString.endsWith(".hx") })) {
            Seq("--include",
              files(0).relativeTo(projectSource) match {
                case Some(relativeFile: File) =>
                  val path = relativeFile.getPath.toString
                  "^" + path.substring(0, path.lastIndexOf(fileSeparator) + 1).replace(fileSeparator, "\\.") + "[a-zA-Z_0-9]*$"
                case _ => ""
              })
          } else Seq()) ++
            directories.foldLeft(Seq[String]())((foldIncludes, directory) => {
              val directoryReg = directory.relativeTo(projectSource) match {
                case Some(relativePath: File) =>
                  val path = relativePath.getPath.toString
                  "^" + path.replace(fileSeparator, "\\.") + "$"
                case _ => ""
              }
              foldIncludes ++ Seq("--include", directoryReg) ++
                haxelibIncludeFlags(projectSource, directory)
            })
        }
      }
    }
  }

  /**
   * execute the command
   */
  private final def processBuild(command: Seq[String], temporaryDirectory: File, sourceManaged: File, logger: Logger): Set[File] = {
    command !< logger match {
      case 0 => {
        val temporarySrc = temporaryDirectory / "src"
        val moveMapping = (temporaryDirectory ** globFilter("*.java")) x {
          _.relativeTo(temporarySrc).map {
            sourceManaged / _.getPath
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

// vim: et sts=2 sw=2
