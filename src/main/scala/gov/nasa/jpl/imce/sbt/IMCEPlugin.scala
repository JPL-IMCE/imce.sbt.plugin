package gov.nasa.jpl.imce.sbt

import java.io.File
import java.util.{Calendar, Locale}

import com.typesafe.config.ConfigFactory
import sbt.Keys._
import sbt._

import scala.language.postfixOps

object IMCEPlugin extends IMCEPlugin {

  object autoImport extends IMCEKeys

  override def trigger = allRequirements

  override def requires =
    aether.AetherPlugin &&
    com.timushev.sbt.updates.UpdatesPlugin &&
    com.typesafe.sbt.packager.universal.UniversalPlugin &&
    sbtbuildinfo.BuildInfoPlugin &&
    sbtrelease.ReleasePlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    defaultProjectSettings ++
    defaultJdkSettings ++
    defaultLicenseSettings ++
    defaultProjectDirectoriesSettings ++
    defaultProjectMavenSettings ++
    defaultDependencyGraphSettings ++
    defaultReleaseSettings

}

trait IMCEPlugin
  extends AutoPlugin
  with JVMSettings
  with ReleaseSettings
  with CompilationSettings
  with DocSettings
  with PackagingSettings
  with DynamicScriptsProjectSettings {

  /**
   * Values intended for the organization of a packaged artifact.
   */
  object Organizations {

    val thirdParty = OrganizationInfo(
      "gov.nasa.jpl.imce.thirdParty", "JPL IMCE Third-Party Dependencies",
      Some(new URL("http://imce.jpl.nasa.gov")))
    val omf = OrganizationInfo(
      "gov.nasa.jpl.imce.omf",
      "JPL IMCE Ontological Modeling Framework Project",
      Some(new URL("http://imce.jpl.nasa.gov")))
    val oti = OrganizationInfo(
      "gov.nasa.jpl.imce.omg.oti",
      "JPL/OMG Tool-Neutral (OTI) Project",
      Some(new URL("http://svn.omg.org/repos/TIWG")))
    val cae = OrganizationInfo(
      "gov.nasa.jpl.imce.secae",
      "JPL CAE",
      Some(new URL("http://cae.jpl.nasa.gov")))

  }

  /**
   * `publish` have a dependency on `dependencyTree`
   * so that when doing just `publish`, we'd automatically get the `dependencyTree` as well.
   */
  def defaultDependencyGraphSettings: Seq[Setting[_]] =
    net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings

  /**
   * SBT settings that can projects are likely to override.
   */
  def defaultProjectSettings: Seq[Setting[_]] =
    Seq(
      IMCEKeys.sbtConfig := {

        // Default Classpath configuration, i.e., application.{conf,json,properties}
        // Can override with -Dconfig.file=<file>
        ConfigFactory.load()
        // If no configuration, try looking up in the system environment
        .withFallback(ConfigFactory.systemEnvironment())

      },

      organization := IMCEKeys.organizationInfo.value.groupId,
      organizationName := IMCEKeys.organizationInfo.value.name,
      organizationHomepage := IMCEKeys.organizationInfo.value.url,

      IMCEKeys.licenseYearOrRange :=
      Calendar.getInstance()
      .getDisplayName(Calendar.YEAR, Calendar.LONG_STANDALONE, Locale.getDefault),

      // disable automatic dependency on the Scala library
      autoScalaLibrary := false,

      scalaVersion := "2.11.7"
    )

  /**
   * SBT settings to exclude directories that do not exist.
   */
  def defaultProjectDirectoriesSettings: Seq[Setting[_]] =
    Seq(
      sourceDirectories in Compile ~= { _.filter(_.exists) },
      sourceDirectories in Test ~= { _.filter(_.exists) },
      unmanagedSourceDirectories in Compile ~= { _.filter(_.exists) },
      unmanagedSourceDirectories in Test ~= { _.filter(_.exists)},
      unmanagedResourceDirectories in Compile ~= { _.filter(_.exists)},
      unmanagedResourceDirectories in Test ~= { _.filter(_.exists) }
    )

  def defaultProjectMavenSettings: Seq[Setting[_]] =
    aether.AetherPlugin.autoImport.overridePublishSettings ++
    Seq(
      // do not include all repositories in the POM
      // (this is important for staging since artifacts published to a staging repository
      //  can be promoted (i.e. published) to another repository)
      pomAllRepositories := false,

      // make sure no repositories show up in the POM file
      pomIncludeRepository := { _ => false },

      // include *.zip artifacts in the POM dependency section
      makePomConfiguration :=
        makePomConfiguration.value.copy(includeTypes = Set(Artifact.DefaultType, Artifact.PomType, "zip")),

      // publish Maven POM metadata (instead of Ivy);
      // this is important for the UpdatesPlugin's ability to find available updates.
      publishMavenStyle := true,

      // make aether publish all packaged artifacts
      aether.AetherKeys.aetherArtifact <<=
      (aether.AetherKeys.aetherCoordinates,
        aether.AetherKeys.aetherPackageMain,
        makePom in Compile,
        packagedArtifacts in Compile) map {
        (coords: aether.MavenCoordinates, mainArtifact: File, pom: File, artifacts: Map[Artifact, File]) =>
          aether.AetherPlugin.createArtifact(artifacts, pom, coords, mainArtifact)
      }
    ) ++
      (( Option.apply(System.getProperty("JPL_LOCAL_RESOLVE_REPOSITORY")),
         Option.apply(System.getProperty("JPL_REMOTE_RESOLVE_REPOSITORY")) ) match {
        case (Some(dir), _) =>
          if ((new File(dir) / "settings.xml").exists) {
            val cache = new MavenCache("JPL Resolve", new File(dir))
            Seq(resolvers += cache)
          }
          else
            sys.error(s"The JPL_LOCAL_RESOLVE_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
        case (None, Some(url)) =>
          val repo = new MavenRepository("JPL Resolve", url)
          Seq(resolvers += repo)
        case _ =>
          sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or" +
          "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where" +
          "<dir> is a local Maven repository directory or" +
          "<url> is a remote Maven repository URL")
      }) ++
      (( Option.apply(System.getProperty("JPL_LOCAL_PUBLISH_REPOSITORY")),
         Option.apply(System.getProperty("JPL_REMOTE_PUBLISH_REPOSITORY")) ) match {
        case (Some(dir), _) =>
          if ((new File(dir) / "settings.xml").exists) {
            val cache = new MavenCache("JPL Publish", new File(dir))
            Seq(publishTo := Some(cache))
          }
          else
            sys.error(s"The JPL_LOCAL_PUBLISH_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
        case (None, Some(url)) =>
          val repo = new MavenRepository("JPL Publish", url)
          Seq(publishTo := Some(repo))
        case _ =>
          sys.error("Set either -DJPL_LOCAL_PUBLISH_REPOSITORY=<dir> or" +
          "-DJPL_REMOTE_PUBLISH_REPOSITORY=<url> where" +
          "<dir> is a local Maven repository directory or" +
          "<url> is a remote Maven repository URL")
      })

  /**
    * SBT settings to ensure all source files have the same license header.
    */
  def defaultLicenseSettings: Seq[Setting[_]] =
    com.banno.license.Plugin.licenseSettings ++
    Seq(
      com.banno.license.Plugin.LicenseKeys.removeExistingHeaderBlock := true,
      com.banno.license.Plugin.LicenseKeys.license :=
      s"""|
          |License Terms
          |
          |Copyright (c) ${IMCEKeys.licenseYearOrRange.value}, California Institute of Technology ("Caltech").
          |U.S. Government sponsorship acknowledged.
          |
          |All rights reserved.
          |
          |Redistribution and use in source and binary forms, with or without
          |modification, are permitted provided that the following conditions are
          |met:
          |
          |*   Redistributions of source code must retain the above copyright
          |   notice, this list of conditions and the following disclaimer.
          |
          |*   Redistributions in binary form must reproduce the above copyright
          |   notice, this list of conditions and the following disclaimer in the
          |   documentation and/or other materials provided with the
          |   distribution.
          |
          |*   Neither the name of Caltech nor its operating division, the Jet
          |   Propulsion Laboratory, nor the names of its contributors may be
          |   used to endorse or promote products derived from this software
          |   without specific prior written permission.
          |
          |THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
          |IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
          |TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
          |PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
          |OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
          |EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
          |PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
          |PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
          |LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
          |NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
          |SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
          |""".stripMargin
    )
}