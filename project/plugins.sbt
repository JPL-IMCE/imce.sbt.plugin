logLevel := Level.Warn

// https://bintray.com/banno/oss/sbt-license-plugin/view
resolvers += Resolver.url(
  "sbt-license-plugin-releases",
  url("http://dl.bintray.com/banno/oss"))(Resolver.ivyStylePatterns)

// https://github.com/sbt/sbt-header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.6.0")

// https://github.com/Banno/sbt-license-plugin
addSbtPlugin("com.banno" % "sbt-license-plugin" %"0.1.5")

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.14")

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")

// http://www.scala-sbt.org/sbt-pgp/
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// https://github.com/xerial/sbt-sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

// https://github.com/typesafehub/config
libraryDependencies += "com.typesafe" % "config" % "1.3.0"

// https://github.com/spray/spray-json
resolvers += "repo.spray.io" at "http://repo.spray.io/"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.10")

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.2.0")

// https://github.com/sbt/sbt-site
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.1.0")

// https://github.com/sbt/sbt-ghpages
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-ghpages
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")
