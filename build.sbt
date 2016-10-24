enablePlugins(AetherPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

overridePublishBothSettings

// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % Versions.sbt_license_header)

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % Versions.sbt_license_report)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % Versions.sbt_git)

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % Versions.sbt_scoverage)

// https://github.com/jrudolph/sbt-optimizer
addSbtPlugin("net.virtual-void" % "sbt-optimizer" % Versions.sbt_optimizier)

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % Versions.sbt_dependency_graph)

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % Versions.sbt_pack)

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % Versions.aether_deploy)

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % Versions.sbt_updates)

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % Versions.sbt_native_packager)

// https://github.com/sbt/sbt-aspectj
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % Versions.sbt_aspectj)

// https://github.com/sksamuel/sbt-scapegoat
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % Versions.sbt_scapegoat)

// https://github.com/puffnfresh/wartremover
addSbtPlugin("org.wartremover" %% "sbt-wartremover" % Versions.sbt_wartremover)

// https://github.com/xerial/sbt-sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % Versions.sbt_sonatype)

// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % Versions.sbt_buildinfo)

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % Versions.sbt_release)

// http://www.scala-sbt.org/sbt-pgp/
addSbtPlugin("com.jsuereth" % "sbt-pgp" % Versions.sbt_pgp)

// https://github.com/typesafehub/config
libraryDependencies += "com.typesafe" % "config" % Versions.config

resolvers += "repo.spray.io" at "http://repo.spray.io/"

// https://github.com/spray/spray-json
libraryDependencies += "io.spray" %%  "spray-json" % Versions.spray_json

// https://github.com/pathikrit/better-files
libraryDependencies += "com.github.pathikrit" %% "better-files" % Versions.better_files

// https://github.com/sbt/sbt-site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % Versions.sbt_site)

// https://github.com/sbt/sbt-ghpages
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % Versions.sbt_ghpages)

addSbtPlugin("org.scala-js" % "sbt-scalajs" % Versions.scalajs)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % Versions.sbteclipse)
