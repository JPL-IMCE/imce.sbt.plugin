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

import com.typesafe.config._

import sbt.Keys._
import sbt._

import scala.language.postfixOps

object IMCEPlugin extends IMCEPlugin {

  object autoImport extends IMCEKeys

  override def trigger = allRequirements

  override def requires =
    aether.SignedAetherPlugin &&
    com.timushev.sbt.updates.UpdatesPlugin &&
    com.typesafe.sbt.packager.universal.UniversalPlugin &&
    sbtbuildinfo.BuildInfoPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    defaultProjectSettings ++
    defaultJdkSettings ++
    defaultProjectDirectoriesSettings ++
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


}