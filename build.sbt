sbtPlugin := true

enablePlugins(AetherPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

overridePublishBothSettings

organization := "gov.nasa.jpl.imce"

name := "imce.sbt.plugin"

logLevel in Compile := Level.Debug

persistLogLevel := Level.Debug

version := "1.1"

git.useGitDescribe := true

versionWithGit

scalaVersion := "2.10.5"

// https://bintray.com/banno/oss/sbt-license-plugin/view
resolvers += Resolver.url(
  "sbt-license-plugin-releases",
  url("http://dl.bintray.com/banno/oss"))(Resolver.ivyStylePatterns)

// https://github.com/Banno/sbt-license-plugin
addSbtPlugin("com.banno" % "sbt-license-plugin" % "0.1.5")

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.0.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.0")

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.7.7")

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.9")

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.16")

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")

// https://github.com/sbt/sbt-aspectj
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.2")

// https://github.com/sksamuel/sbt-scapegoat
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.3")

// https://github.com/puffnfresh/wartremover
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

publishMavenStyle := true

// do not include all repositories in the POM
pomAllRepositories := false

pomExtra :=
  <properties>
    <git.branch>{git.gitCurrentBranch.value}</git.branch>
    <git.commit>{git.gitHeadCommit.value.getOrElse("N/A")+(if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
    <git.tags>{git.gitCurrentTags.value}</git.tags>
  </properties>

( Option.apply(System.getProperty("JPL_LOCAL_RESOLVE_REPOSITORY")),
  Option.apply(System.getProperty("JPL_REMOTE_RESOLVE_REPOSITORY")) ) match {
  case (Some(dir), _) =>
    if (new File(dir) / "settings.xml" exists) {
      val cache = new MavenCache("JPL Resolve", new File(dir))
      Seq(resolvers += cache)
    }
    else
      sys.error(s"The JPL_LOCAL_RESOLVE_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
  case (None, Some(url)) => {
    val repo = new MavenRepository("JPL Resolve", url)
    Seq(resolvers += repo)
  }
  case _ => sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or"+
                      "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where"+
                      "<dir> is a local Maven repository directory or"+
                      "<url> is a remote Maven repository URL")
}

( Option.apply(System.getProperty("JPL_LOCAL_PUBLISH_REPOSITORY")),
  Option.apply(System.getProperty("JPL_REMOTE_PUBLISH_REPOSITORY")) ) match {
  case (Some(dir), _) =>
    if (new File(dir) / "settings.xml" exists) {
      val cache = new MavenCache("JPL Publish", new File(dir))
      Seq(publishTo := Some(cache))
    }
    else
      sys.error(s"The JPL_LOCAL_PUBLISH_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
  case (None, Some(url)) => {
    val repo = new MavenRepository("JPL Publish", url)
    Seq(publishTo := Some(repo))
  }
  case _ => sys.error("Set either -DJPL_LOCAL_PUBLISH_REPOSITORY=<dir> or"+
    "-DJPL_REMOTE_PUBLISH_REPOSITORY=<url> where"+
    "<dir> is a local Maven repository directory or"+
    "<url> is a remote Maven repository URL")
}


