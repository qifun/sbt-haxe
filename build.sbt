sbtPlugin := true

name := "sbt-haxe"

organization := "com.qifun"

releaseUseGlobalVersion := false

scalacOptions += "-deprecation"

scalacOptions += "-feature"

description := "A Sbt plugin used to compile Haxe sources in Java/Scala projects."

homepage := Some(url("https://github.com/qifun/sbt-haxe"))

startYear := Some(2014)

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

publishTo <<= (isSnapshot) { isSnapshot: Boolean =>
  if (isSnapshot)
    Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

scmInfo := Some(ScmInfo(
  url("https://bitbucket.org/qforce/sbt-haxe"),
  "scm:git:https://bitbucket.org/qforce/ai-demo.git",
  Some("scm:git:git@bitbucket.org:qforce/sbt-haxe.git")))

pomExtra :=
  <developers>
    <developer>
      <id>chank</id>
      <name>方里权</name>
      <timezone>+8</timezone>
      <email>fangliquan@qq.com</email>
    </developer>
    <developer>
      <id>Atry</id>
      <name>杨博</name>
      <timezone>+8</timezone>
      <email>pop.atry@gmail.com</email>
    </developer>
  </developers>
