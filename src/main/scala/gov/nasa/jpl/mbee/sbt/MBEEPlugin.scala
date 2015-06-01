package gov.nasa.jpl.mbee.sbt

import java.util.{Calendar, Locale}

import com.banno.license.Plugin.LicenseKeys._
import sbt.Keys._
import sbt._

import scala.language.postfixOps

object MBEEPlugin extends AutoPlugin {

  /**
   * Values intended for the organization of a packaged artifact.
   */
  object Organizations {

    val imce = "gov.nasa.jpl.mbee.imce"
    val omf = "gov.nasa.jpl.mbee.omf"
    val oti = "gov.nasa.jpl.mbee.omg.oti"

  }

  object autoImport {

    val mbeeReleaseVersionPrefix = settingKey[String](
      """The version prefix for the next release of the JPL MBEE toolkit (e.g., "1800.02");
        | the version suffix will be generated from the Source Code Management (SCM) system (GIT, SVN)""".stripMargin
    )

    val mbeeLicenseYearOrRange = settingKey[String](
      """The license copyright year (e.g., "2014", "2015") or year range (e.g., "2011-2014")"""
      )


  }

  import autoImport._


  override def projectSettings: Seq[Setting[_]] =
    mbeeDefaultProjectSettings ++
      mbeeLicenseSettings ++
      mbeeCommonProjectDirectoriesSettings

  /**
   * SBT settings that can projects are likely to override.
   */
  def mbeeDefaultProjectSettings: Seq[Setting[_]] =
    Seq(

      mbeeReleaseVersionPrefix := "1800-02",

      mbeeLicenseYearOrRange := Calendar.getInstance().getDisplayName(Calendar.YEAR, Calendar.LONG_STANDALONE, Locale.getDefault)
    )

  /**
   * SBT settings to exclude directories that do not exist.
   */
  def mbeeCommonProjectDirectoriesSettings: Seq[Setting[_]] =
    Seq(
      sourceDirectories in Compile ~= { _.filter(_.exists) },
      sourceDirectories in Test ~= { _.filter(_.exists) },
      unmanagedSourceDirectories in Compile ~= {
        _.filter(_.exists)
      },
      unmanagedSourceDirectories in Test ~= { _.filter(_.exists) },
      unmanagedResourceDirectories in Compile ~= { _.filter(_.exists) },
      unmanagedResourceDirectories in Test ~= { _.filter(_.exists) }
    )

  /**
   * SBT settings for Maven packaged artifact repository
   */
  def mbeeCommonProjectMavenSettings: Seq[Setting[_]] =
    (Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")), Option.apply(System.getProperty("JPL_MBEE_REMOTE_REPOSITORY"))) match {
      case (Some(dir), _) =>
        if (new File(dir) / "settings.xml" exists) {
          val cache = new MavenCache("JPL MBEE", new File(dir))
          Seq(
            publishMavenStyle := true,
            publishTo := Some(cache),
            resolvers += cache)
        }
        else
          sys.error(s"The JPL_MBEE_LOCAL_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
      case (None, Some(url)) => {
          val repo = new MavenRepository("JPL MBEE",  url)
          Seq(
            publishMavenStyle := true,
            publishTo := Some(repo),
            resolvers += repo)
        }
      case _ => sys.error("Set either -DJPL_MBEE_LOCAL_REPOSITORY=<dir> or -DJPL_MBEE_REMOTE_REPOSITORY=<url> where <dir> is a local Maven repository directory or <url> is a remote Maven repository URL")
    }


  /**
   * SBT settings to ensure all source files have the same license header.
   */
  def mbeeLicenseSettings: Seq[Setting[_]] = com.banno.license.Plugin.licenseSettings ++
    Seq(

      removeExistingHeaderBlock := true,

      license := s"""|
                   | License Terms
                   |
                   | Copyright (c) ${mbeeLicenseYearOrRange.value}, California Institute of Technology ("Caltech").
                   | U.S. Government sponsorship acknowledged.
                   |
                   | All rights reserved.
                   |
                   | Redistribution and use in source and binary forms, with or without
                   | modification, are permitted provided that the following conditions are
                   | met:
                   |
                   |
                   |  *   Redistributions of source code must retain the above copyright
                   |      notice, this list of conditions and the following disclaimer.
                   |
                   |  *   Redistributions in binary form must reproduce the above copyright
                   |      notice, this list of conditions and the following disclaimer in the
                   |      documentation and/or other materials provided with the
                   |      distribution.
                   |
                   |  *   Neither the name of Caltech nor its operating division, the Jet
                   |      Propulsion Laboratory, nor the names of its contributors may be
                   |      used to endorse or promote products derived from this software
                   |      without specific prior written permission.
                   |
                   | THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
                   | IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
                   | TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
                   | PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
                   | OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
                   | EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
                   | PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
                   | PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
                   | LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
                   | NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
                   | SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
                   |""".stripMargin


    )

}