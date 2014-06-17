crossBuildingSettings

sbtPlugin := true

name := "sbt-haxe"

organization := "com.qifun"

scalacOptions += "-deprecation"

scalacOptions += "-feature"

CrossBuilding.crossSbtVersions := Seq("0.13")
