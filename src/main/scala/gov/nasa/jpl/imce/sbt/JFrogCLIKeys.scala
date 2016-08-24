/*
 *
 * License Terms
 *
 * Copyright (c) 2015-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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