logLevel := Level.Warn

// http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#sbt+0.13.8
addMavenResolverPlugin

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.14")