package gov.nasa.jpl.mbee.sbt

import java.util.{Calendar, Locale}

import com.banno.license.Plugin.LicenseKeys._
import sbt.Keys._
import sbt._
import xerial.sbt.Pack._

import scala.language.postfixOps

object MBEEPlugin extends AutoPlugin {

  override def trigger = allRequirements

  case class OrganizationInfo(groupId: String, name: String, url: Option[URL] = None)

  /**
   * Values intended for the organization of a packaged artifact.
   */
  object Organizations {

    val imce = OrganizationInfo("gov.nasa.jpl.mbee.imce", "JPL IMCE Project", Some(new URL("http://imce.jpl.nasa.gov")))
    val omf = OrganizationInfo("gov.nasa.jpl.mbee.omf", "JPL IMCE Ontological Modeling Framework Project", Some(new URL("http://imce.jpl.nasa.gov")))
    val oti = OrganizationInfo("gov.nasa.jpl.mbee.omg.oti", "JPL/OMG Tool-Neutral (OTI) Project", Some(new URL("http://svn.omg.org/repos/TIWG")))
    val secae = OrganizationInfo("gov.nasa.jpl.mbee.secae", "JPL SECAE", Some(new URL("http://mbse.jpl.nasa.gov")))

  }

  object autoImport {

    val mbeeOrganizationInfo = settingKey[OrganizationInfo](
    """The characteristics of the MBEE organization (artifact groupID, organization name, and optionally, URL)"""
    )

    val mbeeReleaseVersionPrefix = settingKey[String](
      """The version prefix for the next release of the JPL MBEE toolkit (e.g., "1800.02");
        | the version suffix will be generated from the Source Code Management (SCM) system (GIT, SVN)""".stripMargin
    )

    val mbeeLicenseYearOrRange = settingKey[String](
      """The license copyright year (e.g., "2014", "2015") or year range (e.g., "2011-2014")"""
      )

  }

  import autoImport._

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    mbeeDefaultProjectSettings ++
      mbeeLicenseSettings ++
      mbeeCommonProjectDirectoriesSettings ++
      mbeeCommonProjectMavenSettings

  /**
   * SBT settings that can projects are likely to override.
   */
  def mbeeDefaultProjectSettings: Seq[Setting[_]] =
    Seq(

      organization := mbeeOrganizationInfo.value.groupId,
      organizationName := mbeeOrganizationInfo.value.name,
      organizationHomepage := mbeeOrganizationInfo.value.url,

      scalaVersion := "2.11.6",

      scalacOptions ++= Seq("-target:jvm-1.7", "-Xlint", "-deprecation"),

      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),

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
      unmanagedSourceDirectories in Compile ~= { _.filter(_.exists) },
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

  def mbeePackageLibraryDependenciesSettings: Seq[Setting[_]] =
    packSettings ++
      publishPackZipArchive ++
      Seq(
        packExpandedClasspath := false,
        packLibJars := Seq.empty,
        packExcludeArtifactTypes := Seq("src", "doc"),
        (mappings in pack) := { extraPackFun.value }
      )

  def mbeePackageLibraryDependenciesWithoutSourcesSettings: Seq[Setting[_]] =
    mbeePackageLibraryDependenciesSettings ++
      Seq(
        // disable publishing artifacts produced by `package`, `packageDoc`, `packageSrc`
        // in all configurations (Compile, Test, ...)
        publishArtifact := false
      )

  val extraPackFun: Def.Initialize[Task[Seq[(File, String)]]] = Def.task[Seq[(File, String)]] {
    def getFileIfExists(f: File, where: String): Option[(File, String)] = if (f.exists()) Some((f, s"$where/${f.getName}")) else None

    val ivyHome: File = Classpaths.bootIvyHome(appConfiguration.value) getOrElse sys.error("Launcher did not provide the Ivy home directory.")

    // this is a workaround; how should it be done properly in sbt?

    // goal: process the list of library dependencies of the project.
    // that is, we should be able to tell the classification of each library dependency module as shown in sbt:
    //
    // > show libraryDependencies
    // [info] List(
    //    org.scala-lang:scala-library:2.11.2,
    //    org.scala-lang:scala-library:2.11.2:provided,
    //    org.scala-lang:scala-compiler:2.11.2:provided,
    //    org.scala-lang:scala-reflect:2.11.2:provided,
    //    com.typesafe:config:1.2.1:compile,
    //    org.scalacheck:scalacheck:1.11.5:compile,
    //    org.scalatest:scalatest:2.2.1:compile,
    //    org.specs2:specs2:2.4:compile,
    //    org.parboiled:parboiled:2.0.0:compile)

    // but... libraryDependencies is a SettingKey (see ld below)
    // I haven't figured out how to get the sequence of modules from it.
    val ld: SettingKey[Seq[ModuleID]] = libraryDependencies

    // workaround... I found this API that I managed to call...
    // this overrides the classification of all jars -- i.e., it is as if all library dependencies had been classified as "compile".

    // for now... it's a reasonable approaximation of the goal...
    val managed: Classpath = Classpaths.managedJars(Compile, classpathTypes.value, update.value)
    val result: Seq[(File, String)] = managed flatMap { af: Attributed[File] =>
      af.metadata.entries.toList flatMap { e: AttributeEntry[_] =>
        e.value match {
          case null => Seq()
          case m: ModuleID => Seq() ++
            getFileIfExists(new File(ivyHome, s"cache/${m.organization}/${m.name}/srcs/${m.name}-${m.revision}-sources.jar"), "lib.srcs") ++
            getFileIfExists(new File(ivyHome, s"cache/${m.organization}/${m.name}/docs/${m.name}-${m.revision}-javadoc.jar"), "lib.javadoc")
          case _ => Seq()
        }
      }
    }
    result
  }

}