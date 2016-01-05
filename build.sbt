import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.pgp.PgpKeys

import sbtrelease._
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion=>_,_}

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
  case _ => sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or"+
                      "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where"+
                      "<dir> is a local Maven repository directory or"+
                      "<url> is a remote Maven repository URL")
}

Option.apply(System.getProperty("JPL_NEXUS_REPOSITORY_HOST")) match {
  case Some(address) =>
    Seq(
      sonatypeCredentialHost := address,
      sonatypeRepository := s"https://$address/nexus/service/local"
    )
  case None =>
    sys.error(s"Set -DJPL_NEXUS_REPOSITORY_HOST=<address> to the host <address> of a nexus pro repository")
}

publishTo := None

enablePlugins(AetherPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

overridePublishBothSettings

organization := "gov.nasa.jpl.imce"

name := "imce.sbt.plugin"

logLevel in Compile := Level.Debug

persistLogLevel := Level.Debug


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

// https://github.com/jrudolph/sbt-dependency-graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % Versions.sbt_dependency_graph)

// https://github.com/xerial/sbt-pack
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % Versions.sbt_pack)

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
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % Versions.sbt_wartremover)

// https://github.com/xerial/sbt-sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % Versions.sbt_sonatype)

// https://github.com/sbt/sbt-buildinfo
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % Versions.sbt_buildinfo)

// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.gseitz" % "sbt-release" % Versions.sbt_release)

// http://www.scala-sbt.org/sbt-pgp/
addSbtPlugin("com.jsuereth" % "sbt-pgp" % Versions.sbt_pgp)

// https://github.com/typesafehub/config
libraryDependencies += "com.typesafe" % "config" % Versions.config

useGpg := true

useGpgAgent := true

git.baseVersion := Versions.version

git.useGitDescribe := true

val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

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
  .map(_.drop(1))
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

lazy val sonatypeOpenGAV: ReleaseStep = { st1: State =>
  val e1 = Project.extract(st1)
  val command = s"sonatypeOpen g=${e1.get(organization)},a=${e1.get(name)},v=${e1.get(version)}"
  st1.log.info(s"st1: comand: $command")
  st1.log.info(s"st1: git.gitUncommittedChanges=${e1.get(git.gitUncommittedChanges)}")
  st1.log.info(s"st1: publishTo=${e1.get(publishTo)}")
  val st2 = releaseStepCommand(command)(st1)
  val e2 = Project.extract(st2)
  val pTo = e2.get(publishTo)
  st2.log.info(s"st2: publishTo=$pTo")
  import PgpKeys._
  val st3 = e2.append(
    //PgpSettings.signingSettings
    Seq(
      publishTo := pTo,
      signedArtifacts <<= (packagedArtifacts, pgpSigner, skip in pgpSigner, streams) map {
        (artifacts, r, skipZ, s) =>
          if (!skipZ) {
            artifacts flatMap {
              case (art, file) =>
                import com.typesafe.sbt.pgp._
                Seq(
                  art                                                ->
                  file,
                  art.copy(extension = art.extension + gpgExtension) ->
                  r.sign(file, new File(file.getAbsolutePath + gpgExtension), s))
            }
          } else artifacts
      },
      publishSignedConfiguration <<=
      (signedArtifacts, publishTo, publishMavenStyle, deliver, checksums in publish, ivyLoggingLevel) map {
        (arts, publishTo, mavenStyle, ivyFile, checks, level) =>
          st2.log.info(s"publishSignedConfiguration: publishTo=$publishTo")
          Classpaths.publishConfig(
            arts,
            if(mavenStyle) None else Some(ivyFile),
            resolverName = Classpaths.getPublishTo(publishTo).name,
            checksums = checks,
            logging = level,
            overwrite = true)
      },
      publishSigned <<= Classpaths.publishTask(publishSignedConfiguration, deliver),
      publishLocalSignedConfiguration <<=
      (signedArtifacts, deliverLocal, checksums in publishLocal, ivyLoggingLevel) map {
        (arts, ivyFile, checks, level) =>
          Classpaths.publishConfig(arts, Some(ivyFile), checks, logging = level )
      },
      publishLocalSigned <<= Classpaths.publishTask(publishLocalSignedConfiguration, deliver)
    )
    , st2)

  val e3 = Project.extract(st3)
  st3.log.info(s"st3: publishTo=${e3.get(publishTo)}")
  st3
}

releaseProcess := Seq(
  checkUncommittedChanges,
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  //ReleaseStep(action = Command.process("reload", _)),
  //sonatypeOpenGAV,
  runTest,
  tagRelease,
  ReleaseStep(releaseStepTask(PgpKeys.publishSigned in Universal)),
  pushChanges,
  ReleaseStep(action = Command.process(s"sonatypeClose", _))
)

publishMavenStyle := true

// do not include all repositories in the POM
pomAllRepositories := false

// make sure no repositories show up in the POM file
pomIncludeRepository := { _ => false }

lazy val additionalProperties = settingKey[Seq[xml.Node]]("Additional entries for the POM's <properties> section")

additionalProperties :=
  <git.branch>{git.gitCurrentBranch.value}</git.branch>
  <git.commit>{git.gitHeadCommit.value.getOrElse("N/A")+(if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
  <git.tags>{git.gitCurrentTags.value}</git.tags>

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
