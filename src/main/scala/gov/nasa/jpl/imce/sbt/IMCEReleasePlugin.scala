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

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager._
import com.typesafe.sbt.pgp.PgpKeys._

import com.typesafe.config._

import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

import sbt.Keys._
import sbt._

object IMCEReleasePlugin extends AutoPlugin {

  object autoImport {

    val extractArchives
    : TaskKey[Unit]
    = TaskKey[Unit]("extract-archives", "Extracts ZIP files")

    val buildUTCDate
    : SettingKey[String]
    = SettingKey[String]("build-utc-date", "The UDC Date of the build")

    val hasUncommittedChanges
    : TaskKey[Boolean]
    = taskKey[Boolean]("Checks for dirty git by calling out to git directly via runner")

  }

  import autoImport._

  override def requires
  : Plugins
  = sbtrelease.ReleasePlugin &&
      com.typesafe.sbt.SbtPgp &&
      aether.SignedAetherPlugin

  override def buildSettings
  : Seq[Def.Setting[_]]
  = inScope(Global)(Seq(
      useGpg in ThisBuild := true,

      useGpgAgent in ThisBuild := true,

      buildUTCDate in Global := {
        import java.util.{ Date, TimeZone }
        val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm")
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
        formatter.format(new Date)
      }
    )) ++
      com.typesafe.sbt.SbtPgp.buildSettings

  override def projectSettings
  : Seq[Def.Setting[_]]
  = aether.SignedAetherPlugin.autoImport.overridePublishSignedBothSettings ++
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
      aether.AetherKeys.aetherArtifact := {
        val coords = aether.AetherKeys.aetherCoordinates.value
        val mainArtifact = aether.AetherKeys.aetherPackageMain.value
        val pom = (makePom in Compile).value
        val artifacts = (packagedArtifacts in Compile).value
        aether.AetherPlugin.createArtifact(artifacts, coords, mainArtifact)
      },

      releasePublishArtifactsAction := publishSigned.value,

      releaseVersion := {
        val _ = releaseVersionBump.value
          ver => Version(ver)
            .map(_.withoutQualifier)
            .map(_.string)
            .getOrElse(versionFormatError)
      },

      hasUncommittedChanges := {
        val statusCommands = Seq(
          Seq("diff-index", "--cached", "HEAD"),
          Seq("diff-index", "HEAD"),
          Seq("diff-files"),
          Seq("ls-files", "--exclude-standard", "--others")
        )
        val runner = git.runner.value
        val dir = baseDirectory.value
        val uncommittedChanges = statusCommands.map { c =>
          runner(c: _*)(dir, com.typesafe.sbt.git.NullLogger)
        }

        uncommittedChanges.exists(_.nonEmpty)
      }
    )

  lazy val checkUncommittedChanges: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    st.log.info(s"*** ReleaseStep: checkUncommittedChanges")
    if (extracted.get(git.gitUncommittedChanges))
      sys.error("Aborting release due to uncommitted changes")
    st
  }

  // @see https://github.com/jeantil/blog-samples/blob/painless-sbt-build/build.sbt
  def setVersionOnly(selectVersion: Versions => String): ReleaseStep =  { st: State =>
    val vs = st
             .get(ReleaseKeys.versions)
             .getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
    val selected = selectVersion(vs)
    st.log.info(s"*** ReleaseStep: Setting version to '$selected'.")
    val useGlobal =Project.extract(st).get(releaseUseGlobalVersion)
    //val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

    reapply(Seq(
      if (useGlobal) version in ThisBuild := selected
      else version := selected
    ), st)
  }

  lazy val setReleaseVersion: ReleaseStep = {
    val r: ReleaseStep = setVersionOnly(_._1)
    r
  }

  lazy val extractStep: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    val ref = extracted.get(thisProjectRef)
    st.log.info(s"*** ReleaseStep: extractArchives")
    extracted.runAggregated(extractArchives in Global in ref, st)
  }

  lazy val clearSentinel: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    IO.delete(extracted.get(baseDirectory) / "target" / "imce.success")
    st
  }

  lazy val successSentinel: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)
    val sentinel = extracted.get(baseDirectory) / "target" / "imce.success"
    IO.touch(sentinel)
    st.log.info(s"*** IMCE Success! ***")
    st
  }

  lazy val runCompile: ReleaseStep = ReleaseStep(
    action = { st: State =>
      val extracted = Project.extract(st)
      val ref = extracted.get(thisProjectRef)
      st.log.info(s"*** ReleaseStep: runCompile")
      extracted.runAggregated(compile in Compile in ref, st)
    },
    enableCrossBuild = true
  )

  /**
    *
    * @return settings for releasing a package as a zip artifact
    */
  def packageReleaseProcessSettings: Seq[Setting[_]] =
    Seq(
      releaseProcess := Seq(
        clearSentinel,
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        extractStep,
        setReleaseVersion,
        runCompile,
        runTest,
        tagRelease,
        publishArtifacts,
        pushChanges,
        successSentinel
    )) ++
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip")

  /**
    *
    * @return settings for releasing a library
    */
  def libraryReleaseProcessSettings: Seq[Setting[_]] =
    Seq(
      releaseProcess := Seq(
        clearSentinel,
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        extractStep,
        setReleaseVersion,
        runCompile,
        runTest,
        tagRelease,
        ReleaseStep(releaseStepTask(publishSigned in Universal)),
        pushChanges,
        successSentinel
    ))

}