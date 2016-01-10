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

  override def requires =
    sbtrelease.ReleasePlugin

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(
      useGpg in ThisBuild := true,

      useGpgAgent in ThisBuild := true
    )

  override def projectSettings: Seq[Def.Setting[_]] =
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

    st.log.info(s"Setting version to '$selected'.")
    val useGlobal =Project.extract(st).get(releaseUseGlobalVersion)
    //val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

    reapply(Seq(
      if (useGlobal) version in ThisBuild := selected
      else version := selected
    ), st)
  }

  lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

  /**
    *
    * @return settings for releasing a package as a zip artifact
    */
  def packageReleaseProcessSettings: Seq[Setting[_]] =
    Seq(
      releaseProcess := Seq(
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        runTest,
        tagRelease,
        publishArtifacts,
        pushChanges
    )) ++
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip")

  /**
    *
    * @return settings for releasing a library
    */
  def libraryReleaseProcessSettings: Seq[Setting[_]] =
    Seq(
      releaseProcess := Seq(
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        runTest,
        tagRelease,
        ReleaseStep(releaseStepTask(publishSigned in Universal)),
        pushChanges
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
        Files.write(filepath, data.getBytes(StandardCharsets.UTF_8))
        st2.log.info(s"Saved staging repository info:\n$data\nto file: $filepath")

        st2
    }

    val ciStagingRepositoryName = "ciStagingRepositoryCreate"
    val ciStagingRepositorySynopsis = "ciStagingRepositoryCreate description=<string> file=<path>"
    val ciStagingRepositoryHelp = "Create a new staging repository with <string> as its description "+
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
