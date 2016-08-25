
object Versions {

  // this project's version
  val version = "3.16."+Option.apply(System.getenv("TRAVIS_BUILD_NUMBER")).getOrElse("0")

  // https://github.com/Banno/sbt-license-plugin
  val sbt_license_plugin = "0.1.5"

  // https://github.com/sbt/sbt-license-report
  val sbt_license_report = "1.0.0"

  // https://github.com/sbt/sbt-git
  val sbt_git = "0.8.5"

  // https://github.com/scoverage/sbt-scoverage
  val sbt_scoverage = "1.3.5"

  // https://github.com/jrudolph/sbt-optimizer
  val sbt_optimizier = "0.1.2"

  // https://github.com/jrudolph/sbt-dependency-graph
  val sbt_dependency_graph = "0.8.2"

  // https://github.com/xerial/sbt-pack
  val sbt_pack = "0.8.0"

  // https://github.com/arktekk/sbt-aether-deploy
  val aether_deploy = "0.17"

  // https://github.com/rtimush/sbt-updates
  val sbt_updates = "0.1.10"

  // https://github.com/sbt/sbt-native-packager
  val sbt_native_packager = "1.1.1"

  // https://github.com/sbt/sbt-aspectj
  val sbt_aspectj = "0.10.6"

  // https://github.com/sksamuel/sbt-scapegoat
  val sbt_scapegoat = "1.0.4"

  // https://github.com/puffnfresh/wartremover
  val sbt_wartremover = "1.1.0"

  // https://github.com/xerial/sbt-sonatype
  val sbt_sonatype = "1.1"

  // https://github.com/typesafehub/config
  val config = "1.3.0"

  // https://github.com/sbt/sbt-buildinfo
  val sbt_buildinfo = "0.6.1"

  // https://github.com/sbt/sbt-release
  val sbt_release = "1.0.3"

  // http://www.scala-sbt.org/sbt-pgp/
  val sbt_pgp = "1.0.0"

  // https://github.com/spray/spray-json
  val spray_json = "1.3.2"

  // https://github.com/pathikrit/better-files
  val better_files = "2.14.0"

}
