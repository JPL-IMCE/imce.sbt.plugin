sbtPlugin := true

enablePlugins(AetherPlugin, GitVersioning, GitBranchPrompt)

overridePublishBothSettings

organization := "gov.nasa.jpl.mbee.sbt"

name := "sbt.mbee.plugin"

git.baseVersion := "1800.02"

git.useGitDescribe := true

versionWithGit

scalaVersion := "2.10.5"

// https://bintray.com/banno/oss/sbt-license-plugin/view
resolvers += Resolver.url("sbt-license-plugin-releases", url("http://dl.bintray.com/banno/oss"))(Resolver.ivyStylePatterns)

// https://github.com/Banno/sbt-license-plugin
addSbtPlugin("com.banno" % "sbt-license-plugin" % "0.1.4")

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.0.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.1.0")

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.12")

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.8")

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.14")

publishMavenStyle := true

pomAllRepositories := true

(Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")), Option.apply(System.getProperty("JPL_MBEE_REMOTE_REPOSITORY"))) match {
  case (Some(dir), _) =>
    if (new File(dir) / "settings.xml" exists) {
      val cache = new MavenCache("JPL MBEE", new File(dir))
      Seq(
        publishTo := Some(cache),
        resolvers += cache)
    }
    else
      sys.error(s"The JPL_MBEE_LOCAL_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
  case (None, Some(url)) => {
    val repo = new MavenRepository("JPL MBEE", url)
    Seq(
      publishTo := Some(repo),
      resolvers += repo)
  }
  case _ => sys.error("Set either -DJPL_MBEE_LOCAL_REPOSITORY=<dir> or -DJPL_MBEE_REMOTE_REPOSITORY=<url> where <dir> is a local Maven repository directory or <url> is a remote Maven repository URL")
}
