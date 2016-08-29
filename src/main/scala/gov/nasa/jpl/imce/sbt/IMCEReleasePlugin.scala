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


import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

import sbt.Keys._
import sbt._

object IMCEReleasePlugin extends AutoPlugin {

  object autoImport {

    val extractArchives = TaskKey[Unit]("extract-archives", "Extracts ZIP files")

    val buildUTCDate = SettingKey[String]("build-utc-date", "The UDC Date of the build")

    val hasUncommittedChanges = taskKey[Boolean]("Checks for dirty git by calling out to git directly via runner")

  }

  import autoImport._

  override def requires =
    sbtrelease.ReleasePlugin &&
      com.typesafe.sbt.SbtPgp &&
      aether.SignedAetherPlugin

  override def buildSettings: Seq[Def.Setting[_]] =
    inScope(Global)(Seq(
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

  override def projectSettings: Seq[Def.Setting[_]] =
    aether.SignedAetherPlugin.autoImport.overridePublishSignedBothSettings ++
    Seq(

      releasePublishArtifactsAction := publishSigned.value,

      releaseVersion <<= releaseVersionBump ( bumper => {
        ver => Version(ver)
               .map(_.withoutQualifier)
               .map(_.string)
               .getOrElse(versionFormatError)
      }),

      commands += ciStagingRepositoryCreateCommand,

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


  lazy val ciStagingRepositoryCreateCommand: Command = {

    import com.typesafe.config._
    import xerial.sbt.Sonatype._
    import complete.DefaultParsers._
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets

    val ciStagingRepositoryParser: (State) => complete.Parser[((String, String), String)] = (_: State) => {
      Space ~>
        token("profile=" ~> StringBasic <~ Space, "profile") ~
        token("description=" ~> StringBasic <~ Space, "description") ~
        token("file=" ~> StringBasic, "file")
    }

    val ciStagingRepositoryAction: (State, ((String, String), String)) => State = {
      case (st0: State, ((profile: String, description: String), filename: String)) =>
        val st1 = Project.extract(st0).append(
          Seq(SonatypeKeys.sonatypeProfileName := profile),
          st0)
        val st2 = Command.process("sonatypeOpen \""+description+"\"", st1)

        val e = Project.extract(st2)
        val credentialHost=e.get(SonatypeKeys.sonatypeCredentialHost)
        val srp = e.get(SonatypeKeys.sonatypeStagingRepositoryProfile)
        val repo = e.get(SonatypeKeys.sonatypeRepository)
        val publishRepo=repo+"/staging/deployByRepositoryId/"+srp.repositoryId
        val config =
          ConfigFactory.empty()
            .withValue("staging.description", ConfigValueFactory.fromAnyRef(description))
            .withValue("staging.credentialHost", ConfigValueFactory.fromAnyRef(credentialHost))
            .withValue("staging.repositoryId", ConfigValueFactory.fromAnyRef(srp.repositoryId))
            .withValue("staging.repositoryService", ConfigValueFactory.fromAnyRef(repo))
            .withValue("staging.profileName", ConfigValueFactory.fromAnyRef(srp.profileName))
            .withValue("staging.profileId", ConfigValueFactory.fromAnyRef(srp.profileId))
            .withValue("staging.publishTo", ConfigValueFactory.fromAnyRef(publishRepo))

        val data = config.root().render(ConfigRenderOptions.concise().setFormatted(true))
        val filepath = Paths.get(filename)
        val filedir = filepath.getParent.toFile
        if (!filedir.exists )
          IO.createDirectory(filedir)
        Files.write(filepath, data.getBytes(StandardCharsets.UTF_8))
        st2.log.info(s"Saved staging repository info:\n$data\nto file: $filepath")

        st2
    }

    val ciStagingRepositoryName = "ciStagingRepositoryCreate"
    val ciStagingRepositorySynopsis = "ciStagingRepositoryCreate profile=<name> description=<string> file=<path>"
    val ciStagingRepositoryHelp = "Create a new staging repository for the staging profile <name>" +
      " and with <string> as its description "+
      "and write the result information to the file <path>"
    Command(
      name=ciStagingRepositoryName,
      help=Help(
        name=ciStagingRepositoryName,
        briefHelp=(ciStagingRepositorySynopsis, ciStagingRepositoryHelp),
        detail=ciStagingRepositorySynopsis + " -- " + ciStagingRepositoryHelp)
    )(parser=ciStagingRepositoryParser)(effect=ciStagingRepositoryAction)
  }
}