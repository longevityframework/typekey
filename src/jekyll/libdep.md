---
layout: default
---

## Setting up a Library Dependency

### Preliminarys - Scala version

We currently publish artifacts for Scala versions 2.11 and 2.12, so be
sure your project is using a compatible Scala version. For example,
your `build.sbt` file may have:

```scala
scalaVersion := "2.11.12"
```

Or:

```scala
scalaVersion := "2.12.6"
```

### Using Sonatype Artifacts

In your `build.sbt`, include the lines:

```scala
resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += "org.longevityframework" %% "typekey" % "1.0.1"
```

At this time I have no plans to publish snapshots. I'll publish any new releases to Sonatype,
including any bug fix releases. You can follow the procedure below if you want to get the latest
straight from the source.

### Building the Artifacts Yourself

#### Download the Source

Create a clone of the typekey project:

```scala
git clone https://github.com/longevityframework/typekey.git
cd typekey
```

#### Choose the Right Branch

You probably want to be on the `master` branch, as this holds the
latest working version. You are probably already there, but just in
case:

```bash
git checkout master
git pull
```

#### Publish Local

Now use SBT to publish the typekey artifact locally:

```bash
sbt publishLocal
```

#### Include as a Library Dependency

In the projects where you want to use typekey, include a library dependency. If you are on the
`master` branch, use:

```scala
libraryDependencies += "org.longevityframework" %% "typekey" % "1.1-SNAPSHOT"
```
