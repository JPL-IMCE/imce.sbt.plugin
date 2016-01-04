package gov.nasa.jpl.imce.sbt

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager._
import com.typesafe.sbt.pgp.PgpKeys._

import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

import xerial.sbt.Sonatype._

import sbt.Keys._
import sbt._

trait ReleaseSettings {

  def defaultReleaseSettings: Seq[Setting[_]] =
    Seq(
      useGpg := true,

      useGpgAgent := true,

      releasePublishArtifactsAction := publishSigned.value,

      releaseVersion <<= releaseVersionBump ( bumper => {
        ver => Version(ver)
               .map(_.withoutQualifier)
               .map(_.string)
               .getOrElse(versionFormatError)
      })
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
        ReleaseStep(action =
          Command.process(s"sonatypeOpen g=${organization.value}, a=${name.value} v=${version.value}", _)),
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        runTest,
        tagRelease,
        ReleaseStep(releaseStepTask(publishSigned in Universal)),
        pushChanges,
        ReleaseStep(action = Command.process(s"sonatypeClose", _))
    )) ++
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip")

  /**
    *
    * @return settings for releasing a library
    */
  def libraryReleaseProcessSettings: Seq[Setting[_]] =
    Seq(
      releaseProcess := Seq(
        ReleaseStep(action =
          Command.process(s"sonatypeOpen g=${organization.value},a=${name.value},v=${version.value}", _)),
        checkUncommittedChanges,
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        runTest,
        tagRelease,
        publishArtifacts,
        pushChanges,
        ReleaseStep(action = Command.process(s"sonatypeClose", _))
    ))
}
