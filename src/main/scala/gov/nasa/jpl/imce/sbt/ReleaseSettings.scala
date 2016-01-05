package gov.nasa.jpl.imce.sbt

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager._
import com.typesafe.sbt.pgp._
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

  lazy val sonatypeOpenGAV: ReleaseStep = { st1: State =>
    val e1 = Project.extract(st1)
    val command = s"sonatypeOpen g=${e1.get(organization)},a=${e1.get(name)},v=${e1.get(version)}"
    st1.log.info(s"pre: comand: $command")
    st1.log.info(s"pre: git.gitUncommittedChanges=${e1.get(git.gitUncommittedChanges)}")
    st1.log.info(s"pre: publishTo=${e1.get(publishTo)}")
    val st2 = releaseStepCommand(command)(st1)
    val e2 = Project.extract(st2)
    st2.log.info(s"post: publishTo=${e2.get(publishTo)}")
    val st3 = e2.append(
      //PgpSettings.signingSettings
      Seq(
        signedArtifacts <<= (packagedArtifacts, pgpSigner, skip in pgpSigner, streams) map {
          (artifacts, r, skipZ, s) =>
            if (!skipZ) {
              artifacts flatMap {
                case (art, file) =>
                  Seq(art                                                -> file,
                    art.copy(extension = art.extension + gpgExtension) -> r.sign(file, new File(file.getAbsolutePath + gpgExtension), s))
              }
            } else artifacts
        },
        publishSignedConfiguration <<= (signedArtifacts, publishTo, publishMavenStyle, deliver, checksums in publish, ivyLoggingLevel) map { (arts, publishTo, mavenStyle, ivyFile, checks, level) =>
          Classpaths.publishConfig(arts, if(mavenStyle) None else Some(ivyFile), resolverName = Classpaths.getPublishTo(publishTo).name, checksums = checks, logging = level)
        },
        publishSigned <<= Classpaths.publishTask(publishSignedConfiguration, deliver),
        publishLocalSignedConfiguration <<= (signedArtifacts, deliverLocal, checksums in publishLocal, ivyLoggingLevel) map {
          (arts, ivyFile, checks, level) => Classpaths.publishConfig(arts, Some(ivyFile), checks, logging = level )
        },
        publishLocalSigned <<= Classpaths.publishTask(publishLocalSignedConfiguration, deliver)
      )
      , st2)
    st3
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
        sonatypeOpenGAV,
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
        ReleaseStep(releaseStepTask(publishSigned in Universal)),
        pushChanges,
        ReleaseStep(action = Command.process(s"sonatypeClose", _))
    ))
}
