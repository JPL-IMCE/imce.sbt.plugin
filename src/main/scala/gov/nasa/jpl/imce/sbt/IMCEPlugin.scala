/*
 * Copyright 2015 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nasa.jpl.imce.sbt

import java.io.File
import java.util.{Calendar, Locale}

import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.SonatypeKeys
import com.typesafe.config._
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
    sbtbuildinfo.BuildInfoPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    defaultProjectSettings ++
    defaultJdkSettings ++
    defaultProjectDirectoriesSettings ++
    defaultProjectMavenSettings ++
    defaultDependencyGraphSettings

}

trait IMCEPlugin
  extends AutoPlugin
  with JVMSettings
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

      scalaVersion := "2.11.8"
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
          aether.AetherPlugin.createArtifact(artifacts, coords, mainArtifact)
      }
    ) ++
    (( Option.apply(System.getProperty("JPL_LOCAL_RESOLVE_REPOSITORY")),
      Option.apply(System.getProperty("JPL_REMOTE_RESOLVE_REPOSITORY")) ) match {
      case (Some(dir), _) =>
        if ((new File(dir) / "settings.xml").exists) {
          val cache = new MavenCache("JPL Resolve", new File(dir))
          Seq(resolvers += cache)
        }
        else {
          // TODO: cleanup
          //sys.error(s"The JPL_LOCAL_RESOLVE_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
          Seq.empty
        }
      case (None, Some(url)) =>
        val repo = new MavenRepository("JPL Resolve", url)
        Seq(resolvers += repo)
      case _ =>
        // TODO: cleanup
        //sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or" +
        //          "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where" +
        //          "<dir> is a local Maven repository directory or" +
        //          "<url> is a remote Maven repository URL")
        Seq.empty
    }) ++
    (Option.apply(System.getProperty("JPL_STAGING_CONF_FILE")) match {
      case Some(file) =>
        val config = ConfigFactory.parseFile(new File(file))
        val profileName = config.getString("staging.profileName")
        Seq(
          SonatypeKeys.sonatypeCredentialHost := config.getString("staging.credentialHost"),
          SonatypeKeys.sonatypeRepository := config.getString("staging.repositoryService"),
          SonatypeKeys.sonatypeProfileName := profileName,
          SonatypeKeys.sonatypeStagingRepositoryProfile := Sonatype.StagingRepositoryProfile(
            profileId=config.getString("staging.profileId"),
            profileName=profileName,
            stagingType="open",
            repositoryId=config.getString("staging.repositoryId"),
            description=config.getString("staging.description")),
          publishTo := Some(new MavenRepository(profileName, config.getString("staging.publishTo")))
        )
      case None =>
        (( Option.apply(System.getProperty("JPL_LOCAL_PUBLISH_REPOSITORY")),
          Option.apply(System.getProperty("JPL_REMOTE_PUBLISH_REPOSITORY")) ) match {
          case (Some(dir), _) =>
            if ((new File(dir) / "settings.xml").exists) {
              val cache = new MavenCache("JPL Publish", new File(dir))
              Seq(publishTo := Some(cache))
            }
            else {
              // TODO: cleanup
              // sys.error(s"The JPL_LOCAL_PUBLISH_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
              Seq.empty
            }
          case (None, Some(url)) =>
            val repo = new MavenRepository("JPL Publish", url)
            Seq(publishTo := Some(repo))
          case _ =>
            // TODO: cleanup
            //sys.error("Set either -DJPL_LOCAL_PUBLISH_REPOSITORY=<dir> or" +
            //  "-DJPL_REMOTE_PUBLISH_REPOSITORY=<url> where" +
            //  "<dir> is a local Maven repository directory or" +
            //  "<url> is a remote Maven repository URL")
            Seq.empty
        }) ++
        (Option.apply(System.getProperty("JPL_NEXUS_REPOSITORY_HOST")) match {
          case Some(address) =>
            Seq(
              SonatypeKeys.sonatypeCredentialHost := address,
              SonatypeKeys.sonatypeRepository := s"https://$address/nexus/service/local"
            )
          case None =>
            Seq()
        })
    })


}