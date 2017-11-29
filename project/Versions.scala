
object Versions {

  // this project's version
  val version = "4.23."+Option.apply(System.getenv("TRAVIS_BUILD_NUMBER")).getOrElse("0")

  // https://github.com/sbt/sbt-header
  val sbt_license_header = "3.0.1"

  // https://github.com/sbt/sbt-git
  val sbt_git = "0.9.0"

  // https://github.com/arktekk/sbt-aether-deploy
  val aether_deploy = "0.18.2"

  // https://github.com/rtimush/sbt-updates
  val sbt_updates = "0.3.0"

  // https://github.com/sbt/sbt-pgp/releases
  val sbt_pgp = "1.0.1"

  // https://github.com/sbt/sbt-site
  val sbt_site = "1.2.0"

  // https://github.com/sbt/sbt-native-packager
  val sbt_native_packager = "1.3.2"

  // https://github.com/sbt/sbt-license-report
  val sbt_license_report = "1.2.0"

  // https://github.com/jrudolph/sbt-dependency-graph
  val sbt_dependency_graph = "0.8.2"

  // https://github.com/sbt/sbt-ghpages
  val sbt_ghpages = "0.6.2"

  // https://github.com/spray/spray-json
  val spray_json = "1.3.3"

  // https://github.com/typesafehub/config
  val config = "1.3.2"

  // https://github.com/scoverage/sbt-scoverage
  val sbt_scoverage = "1.5.0"

  // https://github.com/xerial/sbt-pack
  val sbt_pack = "0.8.2"

  // https://github.com/sbt/sbt-aspectj
  val sbt_aspectj = "0.11.0"

  // https://github.com/sksamuel/sbt-scapegoat
  val sbt_scapegoat = "1.0.4"

  // https://github.com/sbt/sbt-buildinfo
  val sbt_buildinfo = "0.7.0"

  // https://github.com/pathikrit/better-files
  val better_files = "2.17.0"

  // https://www.scala-js.org/doc/project/
  val scalajs="0.6.21"

  // https://github.com/typesafehub/sbteclipse
  val sbteclipse="5.2.4"
}
