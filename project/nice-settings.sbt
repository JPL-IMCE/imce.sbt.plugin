// argh... version conflicts!
//
// [error] org.scala-lang.modules#scala-xml_2.11;1.0.4
// (needed by [org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8])
// conflicts with org.scala-lang.modules#scala-xml_2.11;1.0.2
// (needed by [org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6])
// [trace] Stack trace suppressed: run 'last *:update' for the full output.
// [trace] Stack trace suppressed: run 'last *:ssExtractDependencies' for the full output.
// [error] (*:update) org.apache.ivy.plugins.conflict.StrictConflictException: org.scala-lang.modules#scala-xml_2.11;1.0.4
// (needed by [org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8])
// conflicts with org.scala-lang.modules#scala-xml_2.11;1.0.2
// (needed by [org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6])
// [error] (*:ssExtractDependencies) org.apache.ivy.plugins.conflict.StrictConflictException: org.scala-lang.modules#scala-xml_2.11;1.0.4
// (needed by [org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8, org.scala-lang#scala-compiler;2.11.8])
// conflicts with org.scala-lang.modules#scala-xml_2.11;1.0.2
// (needed by [org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6, org.scalatest#scalatest_2.11;2.2.6])


// resolvers += "Era7 maven releases" at "https://s3-eu-west-1.amazonaws.com/releases.era7.com"

// addSbtPlugin("ohnosequences" % "nice-sbt-settings" % "0.8.0-RC2")
