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

final object SbtHaxe {

  private lazy val HaxeFileRegex = """^(.*)\.hx$""".r

  private lazy val WarningRegex = """^(.*)\s:\sWarning\s:\s(.*)$""".r

  private lazy val ErrorRegex = """^(.*):\serror\sCS(.*):\s(.*)$""".r

  private lazy val CSharpUnitTestErrorRegex = """(^ERR:\s(.*)$)|(^FAILED\s(\d)*\stests,(.*)$)|(^Called\sfrom(.*)$)""".r

  private final def haxeSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    haxe in injectConfiguration := {
      val includes = (dependencyClasspath in haxeConfiguration).value
      val haxeStreams = (streams in haxeConfiguration).value
      val data = (settingsData in haxeConfiguration).value
      val target = (crossTarget in haxeConfiguration).value
      val haxeOutput = (Keys.target in haxe in injectConfiguration).value
      val platformName = (haxePlatformName in injectConfiguration).value

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
                    Seq("-" + platformName,
                      (haxeOutputPath in injectConfiguration).value.getOrElse(temporaryDirectory).getPath + (haxeOutputExtension in injectConfiguration).value.getOrElse("")) ++
              (haxeOptions in injectConfiguration in haxe).value ++
                haxeModules(in, (sourceDirectories in haxeConfiguration).value)
              (streams in haxeConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
              val logger = (streams in haxeConfiguration).value.log
              IO.delete(haxeOutput)
              class HaxeProcessLogger extends ProcessLogger {
                def info(s: => String): Unit = {
                  if (ErrorRegex.findAllIn(s).hasNext) {
                    logger.error(s)
                  } else {
                    logger.info(s)
                  }
                }
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
                          haxeOutput / _.getPath
                        }
                      }
                      IO.move(moveMapping)
                      moveMapping.map { _._2 }(collection.breakOut)
                    }
                    case _ =>
                      haxeOutput.get.toSet
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

  private final def haxeXmlSetting(
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
            (streams in haxeConfiguration).value.log.info("Generating haxe xml document...")
            val logger = (streams in haxeConfiguration).value.log
            val xmlFile = (Keys.target in haxeXml in injectConfiguration).value
            xmlFile.getParentFile.mkdirs()
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

  private[sbtHaxe] final def docSetting(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    doc in haxeConfiguration <<= Def.task {
      val haxeStreams = (streams in injectConfiguration).value
      val target = (crossTarget in injectConfiguration).value
      val doxOutputDirectory = target / (injectConfiguration.name + "-dox")

      (streams in injectConfiguration).value.log.info("Generating haxe document...")
      val logger = (streams in injectConfiguration).value.log
      val haxeXmlDirectory = (Keys.target in haxeXml).value

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

  private[sbtHaxe] final def buildDoxRegex(sourceDirectories: Seq[File]) = {
    (for (sourcePath <- sourceDirectories) yield haxelibIncludeFlags(sourcePath, sourcePath)).flatten
  }

  private[sbtHaxe] final def csharpRunSettings(injectConfiguration: Configuration) = {
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

  private[sbtHaxe] final def buildInternalDependencyClasspath(
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
    } yield targetDirectory / (configurationName + "_unpacked_haxe") / haxeJar.data.getName) ++ rawIncludes.map(_.data)
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

  private[sbtHaxe] final val baseHaxeSettings =
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
            (SbtHaxe.buildInternalDependencyClasspath(
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

  private[sbtHaxe] final val extendSettings =
    Seq(
      unmanagedSourceDirectories :=
        unmanagedSourceDirectories.value ++ (unmanagedSourceDirectories in Haxe).value,
      managedSourceDirectories :=
        managedSourceDirectories.value ++ (managedSourceDirectories in Haxe).value,
      sourceGenerators :=
        sourceGenerators.value ++ (sourceGenerators in Haxe).value)

  private[sbtHaxe] final val extendTestSettings =
    Seq(
      unmanagedSourceDirectories :=
        unmanagedSourceDirectories.value ++ (unmanagedSourceDirectories in TestHaxe).value,
      managedSourceDirectories :=
        managedSourceDirectories.value ++ (managedSourceDirectories in TestHaxe).value,
      sourceGenerators :=
        sourceGenerators.value ++ (sourceGenerators in TestHaxe).value)

  private[sbtHaxe] final def injectSettings(
    haxeConfiguration: Configuration,
    injectConfiguration: Configuration) = {
    Seq(
      target in haxe in injectConfiguration := (sourceManaged in injectConfiguration).value,
      target in haxeXml := (crossTarget in injectConfiguration).value / "haxe-xml",
      target in haxeXml in injectConfiguration :=
        (target in haxeXml).value / raw"${(haxePlatformName in injectConfiguration).value}.xml",
      target in haxe in injectConfiguration := (sourceManaged in injectConfiguration).value,
      haxeSetting(haxeConfiguration, injectConfiguration),
      haxeOptions in injectConfiguration in haxe := (haxeOptions in injectConfiguration).value,
      haxeXmlSetting(haxeConfiguration, injectConfiguration),
      haxeOptions in injectConfiguration in doc := (haxeOptions in injectConfiguration).value,
      sourceGenerators in injectConfiguration <+= haxe in injectConfiguration)
  }

}