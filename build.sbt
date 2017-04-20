
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

scalacOptions in (Compile, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg"

scalacOptions in (Test, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg"

scalacOptions in (Compile, doc) += "-Xplugin-disable:artima-supersafe"

scalacOptions in (Test, doc) += "-Xplugin-disable:artima-supersafe"

resolvers += "repo.spray.io" at "http://repo.spray.io/"

// https://github.com/sbt/sbt-ghpages
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % Versions.sbt_license_header)

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % Versions.sbt_git)

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % Versions.aether_deploy)

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % Versions.sbt_updates)

// http://www.scala-sbt.org/sbt-pgp/
addSbtPlugin("com.jsuereth" % "sbt-pgp" % Versions.sbt_pgp)

// https://github.com/sbt/sbt-site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % Versions.sbt_site)

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % Versions.sbt_native_packager)

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % Versions.sbt_license_report)

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % Versions.sbt_dependency_graph)

// https://github.com/sbt/sbt-ghpages
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % Versions.sbt_ghpages)

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % Versions.sbt_release)



// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % Versions.sbt_scoverage)

// https://github.com/jrudolph/sbt-optimizer
addSbtPlugin("net.virtual-void" % "sbt-optimizer" % Versions.sbt_optimizer)

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % Versions.sbt_pack)

// https://github.com/sbt/sbt-aspectj
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % Versions.sbt_aspectj)

// https://github.com/sksamuel/sbt-scapegoat
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % Versions.sbt_scapegoat)

// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % Versions.sbt_buildinfo)

// https://github.com/spray/spray-json
libraryDependencies += "io.spray" %%  "spray-json" % Versions.spray_json

// https://github.com/typesafehub/config
libraryDependencies += "com.typesafe" % "config" % Versions.config

// https://github.com/pathikrit/better-files
libraryDependencies += "com.github.pathikrit" %% "better-files" % Versions.better_files

addSbtPlugin("org.scala-js" % "sbt-scalajs" % Versions.scalajs)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % Versions.sbteclipse)

enablePlugins(SignedAetherPlugin)

disablePlugins(AetherPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

overridePublishSignedBothSettings