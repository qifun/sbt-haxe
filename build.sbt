crossBuildingSettings

sbtPlugin := true

name := "sbt-haxe-to-java"

organization := "com.qifun"

scalacOptions += "-deprecation"

version := "0.1.5-SNAPSHOT"

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

publishTo <<= (isSnapshot) { isSnapshot: Boolean =>
  if (isSnapshot)
    Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots") 
  else
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

