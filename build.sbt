
lazy val typekey = project.in(file("."))
  .enablePlugins(JekyllPlugin, SiteScaladocPlugin, GhpagesPlugin)
  .settings(
  organization := "org.longevityframework",
  version := "1.0.0",
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),

  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("Apache License, Version 2.0" ->
    url("http://www.apache.org/licenses/LICENSE-2.0")),

  scalacOptions ++= Seq(
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked"),

  scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits", "-encoding", "UTF-8", "-diagrams"),
  scalacOptions in (Compile, doc) ++= {
    val projectName = (name in (Compile, doc)).value
    val projectVersion = (version in (Compile, doc)).value
    Seq("-doc-title", s"$projectName $projectVersion API")
  },

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,

  homepage := Some(url("https://github.com/longevityframework/typekey")),
  pomExtra := (
    <scm>
      <url>git@github.com:longevityframework/typekey.git</url>
      <connection>scm:git:git@github.com:longevityframework/typekey.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sullivan-</id>
        <name>John Sullivan</name>
        <url>https://github.com/sullivan-</url>
      </developer>
    </developers>),
  git.remoteRepo := "git@github.com:longevityframework/typekey.git",
  siteSubdirName in SiteScaladoc := "api")
