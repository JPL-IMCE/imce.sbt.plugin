package gov.nasa.jpl.imce.sbt

import sbt._, Keys._

/**
  * A simple wrapper for invoking the JFrog CLI via SBT's Process.
  */
trait JFrogCLIKeys {

  val jfrogCliPath = settingKey[String]("path of the jfrog cli executable")

  val bintrayPackageVersion = settingKey[String]("the version of the bintray package to upload files to")

  val bintrayPackagePath = settingKey[String]("3-level path: subject/repository/package")

  val bintrayPackageFiles = taskKey[Iterable[File]]("Files to upload to a bintray package")

  val uploadToBintrayPackage = taskKey[Unit]("Use jfrog cli to upload artifacts to bintray")

  val publishBintrayPackage = taskKey[Unit]("Use jfrog cli to publish all files in a bintray package version")

}

object JFrogCLIKeys extends JFrogCLIKeys {

  def defaultSettings: Seq[Setting[_]]
  = Seq(

    uploadToBintrayPackage <<=
      (jfrogCliPath, bintrayPackagePath, bintrayPackageVersion, bintrayPackageFiles, streams) map {
        case (cli, pkgPath, pkgV, pkgFiles, s) =>

          s.log.info(s"Upload to bintray: $pkgV => ${pkgFiles.size}")
          pkgFiles.foreach { f =>
            s.log.info(s"uploading to bintray: $f")
            val args = Seq("bt", "u", f.absolutePath, pkgPath + "/" + pkgV)
            Process(cli, args) ! s.log match {
              case 0 => ()
              case n => sys.error(s"Abnormal exit=$n from:\n$cli ${args.mkString(" ")}")
            }
          }

      },

    publishBintrayPackage <<=
      (jfrogCliPath, bintrayPackagePath, bintrayPackageVersion, streams) map {
        case (cli, pkgPath, pkgV, s) =>

          s.log.info(s"Publish all files from version $pkgV of bintray package $pkgPath")
          val args = Seq("bt", "vp", pkgPath + "/" + pkgV)
          Process(cli, args) ! s.log match {
            case 0 => ()
            case n => sys.error(s"Abnormal exit=$n from:\n$cli ${args.mkString(" ")}")
          }
      }
  )
}
