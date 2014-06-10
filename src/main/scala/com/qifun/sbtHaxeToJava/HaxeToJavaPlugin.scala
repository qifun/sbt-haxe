package com.qifun.sbtHaxeToJava

import sbt.Plugin
import sbt.Keys._
import sbt._
import java.io.File
import scala.Some

final object HaxeToJavaPlugin extends Plugin {
	final val haxeCommand = SettingKey[String]("haxe-command", "haxe executable")
	final val haxe = TaskKey[Seq[File]]("haxe", "Convert haxe source code to java.")
	final val unmanagedInclude = SettingKey[File]("unmanaged-include", "The default directory for manually managed included haxe.")
	final val HaxeToJava = config("haxe")
	final val TestHaxeToJava = config("test-haxe")
	
	override final def globalSettings =
	  super.globalSettings :+ (haxeCommand := "haxe")
	
	final def haxeToJavaSetting(
	    haxeToJavaConfiguration: Configuration,
	    injectConfiguration: Configuration) = {
	  haxe in injectConfiguration := {
	    val includes = (dependencyClasspath in haxeToJavaConfiguration).value
	    val cache = (cacheDirectory in haxeToJavaConfiguration).value
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
	                  case Some(relativePath:File) => relativePath.toString.substring(0, relativePath.toString.lastIndexOf(".")).replace(System.getProperty("file.separator"), ".")
	                  case _ => ""
	                }
	              }
	        (streams in haxeToJavaConfiguration).value.log.info(processBuilder.mkString("\"", "\" \"", "\""))
	        (streams in haxeToJavaConfiguration).value.log.info("test: " + update)
	        processBuilder !< (streams in haxeToJavaConfiguration).value.log match {
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
	    cachedTranfer((sources in haxeToJavaConfiguration).value.toSet).toSeq
	  }
	}

  final val baseHaxeToJavaSettings =
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
	 
	final val haxeToJavaSettings = 
	  sbt.addArtifact(artifact in packageBin in HaxeToJava, packageBin in HaxeToJava) ++
	  	inConfig(HaxeToJava)(baseHaxeToJavaSettings) ++
	  	inConfig(TestHaxeToJava)(baseHaxeToJavaSettings) ++
	  	Seq(
	  	    ivyConfigurations += HaxeToJava,
	  	    haxeToJavaSetting(HaxeToJava, Compile),
	  	    sourceGenerators in Compile <+= haxe in Compile,
	  	    ivyConfigurations += TestHaxeToJava,
	  	    haxeToJavaSetting(TestHaxeToJava, Test),
	  	    sourceGenerators in Test <+= haxe in Test)
}
