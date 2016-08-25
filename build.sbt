import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.pgp.PgpKeys
import com.banno.license.Plugin.LicenseKeys._

import sbtrelease._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

PgpKeys.useGpg := true

PgpKeys.useGpgAgent := true

licenseSettings

removeExistingHeaderBlock := true

license :=
  s"""|
          |License Terms
      |
          |Copyright (c) 2015-2016, California Institute of Technology ("Caltech").
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

sbtPlugin := true

( Option.apply(System.getProperty("JPL_LOCAL_RESOLVE_REPOSITORY")),
  Option.apply(System.getProperty("JPL_REMOTE_RESOLVE_REPOSITORY")) ) match {
  case (Some(dir), _) =>
    if ((new File(dir) / "settings.xml").exists) {
      val cache = new MavenCache("JPL Resolve", new File(dir))
      Seq(resolvers += cache)
    }
    else
      sys.error(s"The JPL_LOCAL_RESOLVE_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
  case (None, Some(url)) => {
    val repo = new MavenRepository("JPL Resolve", url)
    Seq(resolvers += repo)
  }
  case _ =>
    // TODO: cleanup
    //    sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or"+
    //                      "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where"+
    //                      "<dir> is a local Maven repository directory or"+
    //                      "<url> is a remote Maven repository URL")
    Seq.empty
}

scmInfo := Some(ScmInfo(
  url("https://github.jpl.nasa.gov/imce/imce.sbt.plugin"),
  "git@github.jpl.nasa.gov:imce/imce.sbt.plugin.git"))

developers := List(
  Developer(
    id="rouquett",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/rouquett/default.aspx")))

enablePlugins(AetherPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

overridePublishBothSettings

organization := "JPL-IMCE"

name := "imce.sbt.plugin"

// https://bintray.com/banno/oss/sbt-license-plugin/view
resolvers += Resolver.url(
  "sbt-license-plugin-releases",
  url("http://dl.bintray.com/banno/oss"))(Resolver.ivyStylePatterns)

// https://github.com/Banno/sbt-license-plugin
addSbtPlugin("com.banno" % "sbt-license-plugin" % Versions.sbt_license_plugin)

// https://github.com/sbt/sbt-license-report
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % Versions.sbt_license_report)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// https://github.com/sbt/sbt-git
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % Versions.sbt_git)

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

// https://github.com/scoverage/sbt-scoverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % Versions.sbt_scoverage)

// https://github.com/jrudolph/sbt-optimizer
addSbtPlugin("net.virtual-void" % "sbt-optimizer" % Versions.sbt_optimizier)

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % Versions.sbt_dependency_graph)

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % Versions.sbt_pack)

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % Versions.aether_deploy)

// https://github.com/rtimush/sbt-updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % Versions.sbt_updates)

// https://github.com/arktekk/sbt-aether-deploy
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % Versions.aether_deploy)

// https://github.com/sbt/sbt-native-packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % Versions.sbt_native_packager)

// https://github.com/sbt/sbt-aspectj
addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % Versions.sbt_aspectj)

// https://github.com/sksamuel/sbt-scapegoat
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % Versions.sbt_scapegoat)

// https://github.com/puffnfresh/wartremover
addSbtPlugin("org.wartremover" %% "sbt-wartremover" % Versions.sbt_wartremover)

// https://github.com/xerial/sbt-sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % Versions.sbt_sonatype)

// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % Versions.sbt_buildinfo)

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % Versions.sbt_release)

// http://www.scala-sbt.org/sbt-pgp/
addSbtPlugin("com.jsuereth" % "sbt-pgp" % Versions.sbt_pgp)

pgpSecretRing := file("local.secring.gpg")

pgpPublicRing := file("local.pubring.gpg")

// https://github.com/typesafehub/config
libraryDependencies += "com.typesafe" % "config" % Versions.config

resolvers += "repo.spray.io" at "http://repo.spray.io/"

// https://github.com/spray/spray-json
libraryDependencies += "io.spray" %%  "spray-json" % Versions.spray_json

// https://github.com/pathikrit/better-files
libraryDependencies += "com.github.pathikrit" %% "better-files" % Versions.better_files

import com.typesafe.config._

Option.apply(System.getProperty("JPL_STAGING_CONF_FILE")) match {
  case Some(file) =>
    val config = ConfigFactory.parseFile(new File(file)).resolve()
    val profileName = config.getString("staging.profileName")
    Seq(
      sonatypeCredentialHost := config.getString("staging.credentialHost"),
      sonatypeRepository := config.getString("staging.repositoryService"),
      sonatypeProfileName := profileName,
      sonatypeStagingRepositoryProfile := Sonatype.StagingRepositoryProfile(
        profileId=config.getString("staging.profileId"),
        profileName=profileName,
        stagingType="open",
        repositoryId=config.getString("staging.repositoryId"),
        description=config.getString("staging.description")),
      publishTo := Some(new MavenRepository(profileName, config.getString("staging.publishTo")))
    )
  case None =>
    (Option.apply(System.getProperty("JPL_NEXUS_REPOSITORY_HOST")) match {
      case Some(address) =>
        Seq(
          sonatypeCredentialHost := address,
          sonatypeRepository := s"https://$address/nexus/service/local"
        )
      case None =>
        // TODO: cleanup
        //sys.error(s"Set -DJPL_NEXUS_REPOSITORY_HOST=<address> to the host <address> of a nexus pro repository")
        Seq.empty
    }) ++
    (( Option.apply(System.getProperty("JPL_LOCAL_PUBLISH_REPOSITORY")),
      Option.apply(System.getProperty("JPL_REMOTE_PUBLISH_REPOSITORY")) ) match {
      case (Some(dir), _) =>
        if ((new File(dir) / "settings.xml").exists) {
          val cache = new MavenCache("JPL Publish", new File(dir))
          Seq(publishTo := Some(cache))
        }
        else {
          // TODO: cleanup
          //sys.error(s"The JPL_LOCAL_PUBLISH_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
          Seq.empty
        }
      case (None, Some(url)) => {
        val repo = new MavenRepository("JPL Publish", url)
        Seq(publishTo := Some(repo))
      }
      case _ =>
        // TODO: cleanup
//        sys.error("Set either -DJPL_LOCAL_PUBLISH_REPOSITORY=<dir> or"+
//        "-DJPL_REMOTE_PUBLISH_REPOSITORY=<url> where"+
//        "<dir> is a local Maven repository directory or"+
//        "<url> is a remote Maven repository URL")
        Seq.empty
    })
}

git.baseVersion := Versions.version

git.useGitDescribe := true

val VersionRegex = "([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

git.gitTagToVersionNumber := {
  case VersionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")
  case VersionRegex(v,"") => Some(v)
  case VersionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
  case _ => None
}

git.gitDescribedVersion :=
gitReader.value.withGit(_.describedVersion)
.flatMap(v =>
  Option(v)
  .orElse(formattedShaVersion.value)
  .orElse(Some(git.baseVersion.value))
)

versionWithGit

def setVersionOnly(selectVersion: Versions => String): ReleaseStep =  { st: State =>
  val vs = st
           .get(ReleaseKeys.versions)
           .getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))
  val selected = selectVersion(vs)

  st.log.info(s"Setting version to '$selected'.")
  val useGlobal = Project.extract(st).get(releaseUseGlobalVersion)

  reapply(Seq(
    if (useGlobal) version in ThisBuild := selected
    else version := selected
  ), st)
}

lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

// do not bump the version.
//.map(_.bump(bumper).string)
releaseVersion <<= releaseVersionBump ( bumper => {
  ver => Version(ver)
         .map(_.withoutQualifier)
         .map(_.string)
         .getOrElse(versionFormatError)
})

val showNextVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
val showReleaseVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
showReleaseVersion <<= (version, releaseVersion)((v,f)=>f(v))
showNextVersion <<= (version, releaseNextVersion)((v,f)=>f(v))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

lazy val checkUncommittedChanges: ReleaseStep = { st: State =>
  val extracted = Project.extract(st)
  if (extracted.get(git.gitUncommittedChanges))
    sys.error("Aborting release due to uncommitted changes")
  st
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

releaseProcess := Seq(
  clearSentinel,
  checkUncommittedChanges,
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  runTest,
  tagRelease,
  publishArtifacts,
  pushChanges,
  successSentinel
)

// include *.pom as an artifact
publishMavenStyle := true

// do not include all repositories in the POM
pomAllRepositories := false

// make sure no repositories show up in the POM file
pomIncludeRepository := { _ => false }

lazy val additionalProperties = settingKey[Seq[xml.Node]]("Additional entries for the POM's <properties> section")

additionalProperties := {
  <git.branch>
    {git.gitCurrentBranch.value}
  </git.branch>
    <git.commit>
      {git.gitHeadCommit.value.getOrElse("N/A") + (if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}
    </git.commit>
    <git.tags>
      {git.gitCurrentTags.value}
    </git.tags>
}

pomPostProcess <<= additionalProperties { (additions) =>
  new xml.transform.RuleTransformer(new xml.transform.RewriteRule {
    override def transform(n: xml.Node): Seq[xml.Node] =
      n match {
        case <properties>{props @ _*}</properties> =>
          <properties>{props}{additions}</properties>
        case _ =>
          n
      }
  })
}

val ciStagingRepositoryCreateCommand: Command = {

  import com.typesafe.config._
  import java.nio.file.{Paths, Files}
  import java.nio.charset.StandardCharsets

  import complete.DefaultParsers._
  val ciStagingRepositoryParser: (State) => complete.Parser[((String, String), String)] = (_: State) => {
    Space ~>
      token("profile=" ~> StringBasic <~ Space, "profile") ~
      token("description=" ~> StringBasic <~ Space, "description") ~
      token("file=" ~> StringBasic, "file")
  }

  val ciStagingRepositoryAction: (State, ((String, String), String)) => State = {
    case (st0: State, ((profile: String, description: String), filename: String)) =>
      val st1 = Project.extract(st0).append(
        Seq(sonatypeProfileName := profile),
        st0)
      val st2 = Command.process("sonatypeOpen \""+description+"\"", st1)

      val e = Project.extract(st2)
      val credentialHost=e.get(sonatypeCredentialHost)
      val srp = e.get(sonatypeStagingRepositoryProfile)
      val repo = e.get(sonatypeRepository)
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

commands += ciStagingRepositoryCreateCommand

val jfrogCliPath = settingKey[String]("path of the jfrog cli executable")

val bintrayUser = settingKey[String]("Bintray username")

val bintrayRepo = settingKey[String]("Bintray repo")

val bintrayPackageName = settingKey[String]("Bintray package")

val bintrayPackageVersion = settingKey[String]("the version of the bintray package to upload files to")

val bintrayPackageFiles = taskKey[Iterable[File]]("Files to upload to a bintray package")

val uploadToBintrayPackage = taskKey[Unit]("Use jfrog cli to upload artifacts to bintray")

val publishBintrayPackage = taskKey[Unit]("Use jfrog cli to publish all files in a bintray package version")

uploadToBintrayPackage <<=
  ( jfrogCliPath,
    bintrayUser, bintrayRepo, bintrayPackageName, bintrayPackageVersion, bintrayPackageFiles,
    sbtPlugin, scalaBinaryVersion, sbtBinaryVersion,
    streams) map {
  case (cli, btUser, btRepo, btPkg, btV, files, isSBT, scalaV, sbtV, s) =>

  s.log.info(s"Upload to bintray: $btV => ${files.size}")
  files.foreach { f =>
    s.log.info(s"uploading to bintray: $f")
    val path =
      s"$btUser/$btRepo/$btPkg/$btV"
    val loc =
        btRepo.replace('.','/')+
        (if (isSBT) "/sbt/" else "/")+
        btPkg+"_"+scalaV+(if (isSBT) "_"+sbtV else "")+
        "/"+btV+"/"
    val args = Seq("bt", "u", f.absolutePath, path, loc)
    Process(cli, args) ! s.log match {
      case 0 => ()
      case n => sys.error(s"Abnormal exit=$n from:\n$cli ${args.mkString(" ")}")
    }
  }

}

publishBintrayPackage <<=
  ( jfrogCliPath,
    bintrayUser, bintrayRepo, bintrayPackageName, bintrayPackageVersion,
    streams) map {
  case (cli, btUser, btRepo, btPkg, btV, s) =>

    s.log.info(s"Publish all files from version $btV of bintray package $btPkg")
    val args = Seq("bt", "vp", s"$btUser/$btRepo/$btPkg/$btV")
    Process(cli, args) ! s.log match {
      case 0 => ()
      case n => sys.error(s"Abnormal exit=$n from:\n$cli ${args.mkString(" ")}")
    }
}

jfrogCliPath := Path.userHome.toPath.resolve("bin/jfrog").toFile.absolutePath

bintrayUser := "jpl-imce"

bintrayRepo := "gov.nasa.jpl.imce"

bintrayPackageName := "imce.sbt.plugin"

bintrayPackageVersion := Option.apply(System.getenv("TRAVIS_TAG")).getOrElse(version.value)

bintrayPackageFiles := PgpKeys.signedArtifacts.value.values
