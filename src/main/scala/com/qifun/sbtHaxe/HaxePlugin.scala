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

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File
import java.nio.file.Path
import scala.Some
import java.util.regex.Pattern

final object HaxePlugin extends Plugin {

  private val HaxeFileRegex = """^(.*)\.hx$""".r

  private val WarningRegex = """^(.*)\s:\sWarning\s:\s(.*)$""".r

  private val CSharpUnitTestErrorRegex = """(^ERR:\s(.*)$)|(^FAILED\s(\d)*\stests,(.*)$)|(^Called\sfrom(.*)$)""".r

  final val Haxe = config("haxe")
  final val TestHaxe = config("test-haxe") extend Haxe
  final val HaxeJava = config("haxe-java") extend Haxe
  final val TestHaxeJava = config("test-haxe-java") extend HaxeJava
  final val CSharp = config("csharp")
  final val TestCSharp = config("test-csharp") extend CSharp
  final val HaxeCSharp = config("haxe-csharp") extend Haxe
  final val TestHaxeCSharp = config("test-haxe-csharp") extend HaxeCSharp

  final val haxeOptions = SettingKey[Seq[String]]("haxe-options", "Additional command-line options for Haxe compiler.")
  final val haxeCommand = SettingKey[String]("haxe-command", "The Haxe executable.")
  final val haxelibCommand = SettingKey[String]("haxeilb-command", "The haxelib executable")
  final val haxePlatformName = SettingKey[String]("haxe-platform-name", "The name of the haxe platform")
  final val haxe = TaskKey[Seq[File]]("haxe", "Convert Haxe source code to Java or C#.")
  final val haxeXmls = TaskKey[Seq[File]]("haxe-xmls", "Generate Haxe xmls.")
  final val doxRegex = SettingKey[Seq[String]]("dox-regex", "The Regex that used to generate Haxe documentation.")
  final val haxeXml = TaskKey[Seq[File]]("haxeXml", "Generate Haxe xml.")

  override final def globalSettings =
    super.globalSettings ++ Seq(
      haxeCommand := "haxe",
      haxelibCommand := "haxelib")

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
      val haxeStreams = (streams in haxeConfiguration).value
      val data = (settingsData in haxeConfiguration).value
      val target = (crossTarget in haxeConfiguration).value
      val sourceManagedValue = (sourceManaged in injectConfiguration).value

      val cachedTranfer =
        FileFunction.cached(
          haxeStreams.cacheDirectory / ("haxe_" + scalaVersion.value),
          inStyle = FilesInfo.lastModified,
          outStyle = FilesInfo.exists) { (in: Set[File]) =>
            IO.withTemporaryDirectory { temporaryDirectory =>
              val deps =
                (buildDependencies in haxeConfiguration).value.classpath((thisProjectRef in haxeConfiguration).value)

              val processBuilder =
                Seq[String](
                  (haxeCommand in injectConfiguration).value) ++
                  (for (sourcePath <- (sourceDirectories in haxeConfiguration).value) yield {
                    Seq("-cp", sourcePath.getPath.toString)
                  }).flatten ++
                  projectPathFlags(
                    haxeStreams,
                    target,
                    includes,
                    scalaVersion.value,
                    haxeConfiguration.name) ++
                    (for {
                      path <- (dependencyClasspath in injectConfiguration).value
                      if path.data.exists
                    } yield {
                      Seq("-java-lib", path.data.toString)
                    }).flatten ++
                    outputFlag(haxeConfiguration, temporaryDirectory, sourceManagedValue) ++
                    (haxeOptions in injectConfiguration in haxe).value ++
                    haxeModules(in, (sourceDirectories in haxeConfiguration).value)
              (streams in haxeConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
              val logger = (streams in haxeConfiguration).value.log
              IO.delete(sourceManagedValue)
              class HaxeProcessLogger extends ProcessLogger {
                def info(s: => String): Unit = logger.info(s)
                def error(s: => String): Unit = {
                  if (WarningRegex.findAllIn(s).hasNext) {
                    logger.warn(s)
                  } else {
                    logger.error(s)
                  }
                }
                def buffer[T](f: => T): T = f
              }
              val haxeLogger = new HaxeProcessLogger
              processBuilder !< haxeLogger match {
                case 0 => {
                  (haxeConfiguration == HaxeJava || haxeConfiguration == TestHaxeJava) match {
                    case true => {
                      val temporarySrc = temporaryDirectory / "src"
                      val moveMapping = (temporaryDirectory ** (globFilter("*.java"))) pair {
                        _.relativeTo(temporarySrc).map {
                          sourceManagedValue / _.getPath
                        }
                      }
                      IO.move(moveMapping)
                      moveMapping.map { _._2 }(collection.breakOut)
                    }
                    case _ =>
                      Seq[File]().toSet
                  }
                }
                case result => {
                  throw new MessageOnlyException("Haxe returns " + result)
                }
              }
            }
          }
      cachedTranfer((sources in haxeConfiguration).value.toSet).toSeq
    }
  }

  final def haxeXmlSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxeXml in injectConfiguration := {
      val haxeStreams = (streams in haxeConfiguration).value
      val target = (crossTarget in haxeConfiguration).value
      val includes = (dependencyClasspath in haxeConfiguration).value
      val doxPlatform = (haxePlatformName in injectConfiguration).value

      val cachedTranfer =
        FileFunction.cached(
          haxeStreams.cacheDirectory / ("dox_" + scalaVersion.value),
          inStyle = FilesInfo.lastModified,
          outStyle = FilesInfo.exists) { (in: Set[File]) =>
            (streams in haxeConfiguration).value.log.info("Generating haxe document...")
            val logger = (streams in haxeConfiguration).value.log
            val sourceManagedValue = (sourceManaged in injectConfiguration).value
            val haxeXmlDirectory = target / "haxe-xml"
            haxeXmlDirectory.mkdirs()
            val xmlFile = (haxeXmlDirectory / raw"$doxPlatform.xml")
            val processBuilderXml =
              Seq[String](
                (haxeCommand in injectConfiguration).value,
                "-D", "doc-gen",
                "-xml", xmlFile.toString,
                raw"-$doxPlatform", "dummy", "--no-output") ++
                (haxeOptions in injectConfiguration in haxeXml).value ++
                (for (sourcePath <- (sourceDirectories in haxeConfiguration).value) yield {
                  Seq("-cp", sourcePath.getPath.toString)
                }).flatten ++
                projectPathFlags(
                  haxeStreams,
                  target,
                  includes,
                  scalaVersion.value,
                  haxeConfiguration.name) ++
                  (for {
                    path <- (dependencyClasspath in injectConfiguration).value
                    if path.data.exists
                  } yield {
                    Seq("-java-lib", path.data.toString)
                  }).flatten ++
                  haxeModules(in, (sourceDirectories in haxeConfiguration).value)
            haxeStreams.log.info(processBuilderXml.mkString("\"", "\" \"", "\""))
            processBuilderXml !< logger match {
              case 0 =>
                haxeStreams.log.debug(raw"Generate $doxPlatform.xml success!")
              case result =>
                throw new MessageOnlyException(raw"Generate $doxPlatform.xml fail: " + result)
            }
            Seq[File]().toSet + xmlFile
          }
      cachedTranfer((sources in haxeConfiguration).value.toSet).toSeq
    }
  }

  final def docSetting(
    haxeConfiguraion: Configuration,
    injectConfiguration: Configuration) = {
    doc in haxeConfiguraion <<= Def.task {
      val haxeStreams = (streams in injectConfiguration).value
      val target = (crossTarget in injectConfiguration).value
      val doxOutputDirectory = target / (injectConfiguration.name + "-dox")

      (streams in injectConfiguration).value.log.info("Generating haxe document...")
      val logger = (streams in injectConfiguration).value.log
      val sourceManagedValue = (sourceManaged in injectConfiguration).value
      val haxeXmlDirectory = target / "haxe-xml"

      val processBuildDoc =
        Seq[String](
          (haxelibCommand in injectConfiguration).value,
          "run", "dox", "--input-path", haxeXmlDirectory.toString,
          "--output-path", doxOutputDirectory.getPath.toString) ++
          (doxRegex in injectConfiguration).value
      (streams in injectConfiguration).value.log.info(processBuildDoc.mkString("\"", "\" \"", "\""))
      processBuildDoc !< logger match {
        case 0 =>
          (doxOutputDirectory ** (
            globFilter("*.html") ||
            globFilter("*.css") ||
            globFilter("*.js") ||
            globFilter("*.png") ||
            globFilter("*.ico"))).get
          doxOutputDirectory
        case result =>
          throw new MessageOnlyException("Haxe create doc exception: " + result)
      }
    }.dependsOn(haxeXmls in injectConfiguration)
  }

  private final def doxRegex(sourceDirectories: Seq[File]) = {
    (for (sourcePath <- sourceDirectories) yield haxelibIncludeFlags(sourcePath, sourcePath)).flatten
  }

  private final def csharpRunSettings(injectConfiguration: Configuration) = {
    run in injectConfiguration <<= Def.inputTask {
      val exeDirectory = (sourceManaged in injectConfiguration).value / "bin"
      val logger = (streams in injectConfiguration).value.log
      class TestProcessLogger extends ProcessLogger {
        def info(s: => String): Unit = {
          if (CSharpUnitTestErrorRegex.findAllIn(s).hasNext) {
            logger.error(s)
          } else {
            logger.info(s)
          }
        }
        def error(s: => String): Unit = logger.error(s)
        def buffer[T](f: => T): T = f
      }
      val testLogger = new TestProcessLogger
      for (exe <- (exeDirectory ** (globFilter("*.exe"))).get) {
        exe.getPath !< testLogger match {
          case 0 =>
            logger.debug(raw"Excecute ${exe.getPath} success!")
          case result =>
            throw new MessageOnlyException("Test csharp exception: " + result)
        }
      }
    }.dependsOn(haxe in injectConfiguration)
  }

  final val baseHaxeSettings =
    Defaults.configTasks ++
      Defaults.configPaths ++
      Classpaths.configSettings ++
      Defaults.packageTaskSettings(
        packageBin,
        Defaults.sourceMappings) ++
        Seq(
          managedClasspath := {
            def makeArtifactFilter(configuration: Configuration): DependencyFilter = {
              configuration.extendsConfigs.map(makeArtifactFilter).fold(artifactFilter(classifier = configuration.name))(_ || _)
            }
            update.value.filter(
              (configurationFilter(configuration.value.name) || configurationFilter("provided")) &&
                makeArtifactFilter(configuration.value)).toSeq.map {
                case (conf, module, art, file) => {
                  Attributed(file)(
                    AttributeMap.empty.
                      put(artifact.key, art).
                      put(moduleID.key, module).
                      put(configuration.key, configuration.value))
                }
              }.distinct
          },
          internalDependencyClasspath := {
            (buildInternalDependencyClasspath(
              thisProjectRef.value,
              configuration.value,
              settingsData.value,
              buildDependencies.value, Seq[File]()) ++ (for {
                ac <- Classpaths.allConfigs(configuration.value)
                if ac != configuration.value
                sourcePaths <- (sourceDirectories in (thisProjectRef.value, ac)).get(settingsData.value).toList
                sourcePath <- sourcePaths
              } yield sourcePath)).classpath
          },
          unmanagedSourceDirectories := Seq(sourceDirectory.value),
          includeFilter in unmanagedSources := new FileFilter { override final def accept(file: File) = file.isFile })

  final val extendSettings =
    Seq(
      unmanagedSourceDirectories :=
        unmanagedSourceDirectories.value ++ (unmanagedSourceDirectories in Haxe).value,
      managedSourceDirectories :=
        managedSourceDirectories.value ++ (managedSourceDirectories in Haxe).value,
      sourceGenerators :=
        sourceGenerators.value ++ (sourceGenerators in Haxe).value)

  final val extendTestSettings =
    Seq(
      unmanagedSourceDirectories :=
        unmanagedSourceDirectories.value ++ (unmanagedSourceDirectories in TestHaxe).value,
      managedSourceDirectories :=
        managedSourceDirectories.value ++ (managedSourceDirectories in TestHaxe).value,
      sourceGenerators :=
        sourceGenerators.value ++ (sourceGenerators in TestHaxe).value)

  private final def buildInternalDependencyClasspath(
    projectRef: ProjectRef,
    configuration: Configuration,
    settingsData: Settings[Scope],
    buildDependencies: BuildDependencies,
    acc: Seq[File]): Seq[File] = {
    val dependencies = buildDependencies.classpath(projectRef)
    dependencies match {
      case dependencies1 if !dependencies1.isEmpty =>
        val newAcc: Seq[File] = acc ++ (for {
          ResolvedClasspathDependency(dep, _) <- dependencies1
          conf <- Classpaths.allConfigs(configuration)
          sourceDirectoriesOption = (sourceDirectories in (dep, conf)).get(settingsData)
          if sourceDirectoriesOption.isDefined
          directory <- sourceDirectoriesOption.get
        } yield directory)
        dependencies.foldLeft(newAcc) { (newAcc1: Seq[File], classpathDep: ClasspathDep[ProjectRef]) =>
          buildInternalDependencyClasspath(
            classpathDep.project,
            configuration,
            settingsData,
            buildDependencies,
            newAcc1)
        }
      case List() =>
        acc
    }
  }

  override final def buildSettings =
    super.buildSettings :+
      (testFrameworks += new TestFramework("com.qifun.sbtHaxe.testInterface.HaxeUnitFramework"))

  override final def projectSettings = super.projectSettings ++ Seq(
    haxeOptions in CSharp := Nil,
    haxeOptions in TestCSharp := Nil,
    haxeOptions in Compile := Nil,
    haxeOptions in Test := Nil,
    haxeXmls in Compile := Nil,
    haxeXmls in Test := Nil)

  final def injectSettings(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    Seq(
      haxeSetting(haxeConfiguration, injectConfiguration),
      haxeOptions in injectConfiguration in haxe := (haxeOptions in injectConfiguration).value,
      haxeXmlSetting(haxeConfiguration, injectConfiguration),
      haxeOptions in injectConfiguration in doc := (haxeOptions in injectConfiguration).value,
      sourceGenerators in injectConfiguration <+= haxe in injectConfiguration)
  }

  final val haxeSettings =
    inConfig(Haxe)(baseHaxeSettings) ++
      inConfig(TestHaxe)(baseHaxeSettings) ++
      docSetting(Haxe, Compile) ++
      docSetting(TestHaxe, Test)

  final val haxeJavaSettings =
    sbt.addArtifact(artifact in packageBin in HaxeJava, packageBin in HaxeJava) ++
      inConfig(HaxeJava)(baseHaxeSettings) ++
      inConfig(HaxeJava)(extendSettings) ++
      inConfig(TestHaxeJava)(baseHaxeSettings) ++
      inConfig(TestHaxeJava)(extendTestSettings) ++
      injectSettings(HaxeJava, Compile) ++
      injectSettings(TestHaxeJava, Test) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in Compile).value,
        haxeXmls in Test ++= (haxeXml in Test).value,
        haxePlatformName in Compile := "java",
        doxRegex in Compile := doxRegex((sourceDirectories in HaxeJava).value),
        doxRegex in Test := doxRegex((sourceDirectories in TestHaxeJava).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeJava,
        ivyConfigurations += TestHaxeJava)

  final val haxeCSharpSettings =
    sbt.addArtifact(artifact in packageBin in HaxeCSharp, packageBin in HaxeCSharp) ++
      inConfig(CSharp)(baseHaxeSettings) ++
      inConfig(TestCSharp)(baseHaxeSettings) ++
      inConfig(HaxeCSharp)(baseHaxeSettings) ++
      inConfig(HaxeCSharp)(extendSettings) ++
      inConfig(TestHaxeCSharp)(baseHaxeSettings) ++
      inConfig(TestHaxeCSharp)(extendTestSettings) ++
      injectSettings(HaxeCSharp, CSharp) ++
      injectSettings(TestHaxeCSharp, TestCSharp) ++
      csharpRunSettings(CSharp) ++
      csharpRunSettings(TestCSharp) ++
      Seq(
        haxeXmls in Compile ++= (haxeXml in CSharp).value,
        haxeXmls in Test ++= (haxeXml in TestCSharp).value,
        haxePlatformName in CSharp := "cs",
        // TODO (haxePlatformName in TestCSharp) should extend from (haxePlatformName in CSharp). 
        // But now it doesn't work.
        haxePlatformName in TestCSharp := "cs",
        doxRegex in Compile := doxRegex((sourceDirectories in HaxeCSharp).value),
        doxRegex in Test := doxRegex((sourceDirectories in TestHaxeCSharp).value),
        ivyConfigurations += Haxe,
        ivyConfigurations += TestHaxe,
        ivyConfigurations += HaxeCSharp,
        ivyConfigurations += TestHaxeCSharp)

  private final def outputFlag(
    languageConfiguration: Configuration,
    temporaryDirectory: File,
    sourceManagedValue: File): Seq[String] = {
    if (languageConfiguration == HaxeJava | languageConfiguration == TestHaxeJava) {
      Seq("-java", temporaryDirectory.getPath,
        "-D", "no-compilation")
    } else if (languageConfiguration == HaxeCSharp | languageConfiguration == TestHaxeCSharp) {
      Seq("-cs", sourceManagedValue.getPath)
    } else {
      Seq()
    }
  }

  /**
   * Builds -cp xxx command-line options for haxe compile from dependent projects.
   */
  private final def projectPathFlags(
    taskStreams: TaskStreams,
    targetDirectory: RichFile,
    depsClasspath: Seq[Attributed[File]],
    scalaVersion: String,
    configurationName: String): Seq[String] = {
    val unpack = FileFunction.cached(
      taskStreams.cacheDirectory / ("unpacked_haxe_" + scalaVersion),
      inStyle = FilesInfo.lastModified,
      outStyle = FilesInfo.exists) { haxeJars: Set[File] =>
        for {
          haxeJar <- haxeJars
          output <- IO.unzip(haxeJar, targetDirectory / (configurationName + "_unpacked_haxe") / haxeJar.getName)
        } yield output
      }
    val (unpacking, rawIncludes) = depsClasspath.partition { _.data.getPath.endsWith(".jar") }
    val unpacked = unpack(unpacking.map { _.data }(collection.breakOut))
    val directories = (for {
      haxeJar <- unpacking
    } yield targetDirectory / (configurationName + "_unpacked_haxe") / haxeJar.data.getName) ++ depsClasspath.map(_.data)
    val dependSources = (for {
      dep <- directories
      if dep.exists
    } yield Seq("-cp", dep.getPath)).flatten
    val extraParamsHxmls = (for {
      dep <- directories
      extraParamsHxmlFile = dep / "extraParams.hxml"
      if extraParamsHxmlFile.exists
    } yield Seq("-cp", extraParamsHxmlFile.getPath)).flatten
    dependSources ++ extraParamsHxmls
  }

  /**
   * Build the Haxe module name according to the Haxe file name
   */
  private final def haxeModules(haxeSources: Set[File], parents: Seq[sbt.File]) = {
    for {
      haxeSource <- haxeSources
      relativePaths = for {
        parent <- parents
        relativePath <- haxeSource.relativeTo(parent)
      } yield relativePath
      Seq(haxeBaseName) <- relativePaths match {
        case Seq(relativePath) =>
          HaxeFileRegex.unapplySeq(relativePath.toString)
        case Seq() =>
          throw new MessageOnlyException(raw"$haxeSource should be in one of source directories!")
        case _ =>
          throw new MessageOnlyException(raw"$haxeSource should not be in multiple source directories!")
      }
    } yield haxeBaseName.replace(System.getProperty("file.separator"), ".")
  }

  private final def haxelibIncludeFlags(projectSource: File, source: File): Seq[String] = {
    if (projectSource.exists()) {
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
                    "^" + path.substring(0, path.lastIndexOf(fileSeparator) + 1).replace(fileSeparator, "\\.") +
                      "[a-zA-Z_0-9]*$"
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
    } else {
      Seq()
    }
  }

}

// vim: et sts=2 sw=2
