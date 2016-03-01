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

      commands += ciStagingRepositoryCreateCommand
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