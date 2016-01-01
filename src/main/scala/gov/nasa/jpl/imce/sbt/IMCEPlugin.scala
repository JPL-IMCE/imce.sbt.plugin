package gov.nasa.jpl.imce.sbt

import java.io.File
import java.util.{Calendar, Locale}

import com.typesafe.config.{Config, ConfigFactory}
import sbt.Keys._
import sbt._

import scala.io.Source
import scala.language.postfixOps
import scala.util.control.Exception._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


object IMCEPlugin extends IMCEPlugin {

  object autoImport extends IMCEKeys

  override def trigger = allRequirements

  override def requires =
    aether.AetherPlugin &&
    com.timushev.sbt.updates.UpdatesPlugin &&
    com.typesafe.sbt.packager.universal.UniversalPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    defaultProjectSettings ++
    licenseSettings ++
    commonProjectDirectoriesSettings ++
    commonProjectMavenSettings ++
    dependencyGraphSettings

  def nexusJavadocPOMResolveQueryURLAndPublishURL
  ( repositoryRestAPI: String,
    repositoryName: String,
    moduleID: ModuleID)
  : (URL, Regex.Match => URL) =
    ( url(raw"$repositoryRestAPI/artifact/maven/resolve?r=$repositoryName"+
          raw"&g=${moduleID.organization}&a=${moduleID.name}&v=${moduleID.revision}&c=javadoc"),
      (m: Regex.Match) =>
        url(raw"""$repositoryRestAPI/repositories/$repositoryName/archive${m.group(1)}/!/index.html""")
    )

}

trait IMCEPlugin extends AutoPlugin {

  /**
   * Values intended for the organization of a packaged artifact.
   */
  object Organizations {

    val imce = OrganizationInfo(
      "gov.nasa.jpl.imce.imce", "JPL IMCE Project",
      Some(new URL("http://imce.jpl.nasa.gov")))
    val omf = OrganizationInfo(
      "gov.nasa.jpl.imce.omf",
      "JPL IMCE Ontological Modeling Framework Project",
      Some(new URL("http://imce.jpl.nasa.gov")))
    val oti = OrganizationInfo(
      "gov.nasa.jpl.imce.omg.oti",
      "JPL/OMG Tool-Neutral (OTI) Project",
      Some(new URL("http://svn.omg.org/repos/TIWG")))
    val secae = OrganizationInfo(
      "gov.nasa.jpl.imce.secae",
      "JPL SECAE",
      Some(new URL("http://mbse.jpl.nasa.gov")))

  }

  /**
   * Lookup the location of the runtime library for a specific version of the JDK
   *
   * @param config The Config to lookup the JDK installation location
   * @param versionProperty The Config property path of the form 'jdk_locations.1.<N>' used to lookup the JDK location
   *                        or to map to environment property of the form 'jdk_locations_1_<N>'
   * @return A pair of the JDK version (1.<N>) and, optionally,
   *         the location of the jre/lib/rt.jar in the JDK installation folder
   */
  def getJRERuntimeLib(config: Config, versionProperty: String): (String, Option[File]) = {
    val versionEnv = versionProperty.replace('.', '_')
    val versionKey = versionProperty.stripPrefix("jdk_locations.").replace('_', '.')
    Try(config.getString(versionProperty))
    .orElse(Try(config.getString(versionEnv))) match {
      case Success(location) =>
        val rtLib = Path(location) / "jre" / "lib" / "rt.jar"
        if (rtLib.exists && !rtLib.isDirectory && rtLib.asFile.canRead)
          (versionKey, Some(rtLib.asFile))
        else
          (versionKey, None)
      case Failure(_)        =>
        (versionKey, None)
    }
  }

  /**
   * Computes the additional JDK version-specific content to append to javacOptions
   *
   * @see https://blogs.oracle.com/darcy/entry/bootclasspath_older_source
   * @see https://blogs.oracle.com/darcy/entry/how_to_cross_compile_for
    * @param jdk JDK version & installation location, if available
   * @return content to append to javacOptions
   */
  def getJavacOptionsForJDKIfAvailable(jdk: SettingKey[(String, Option[File])])
  : Def.Initialize[Task[Seq[String]]] =
    Def.task[Seq[String]] {
    jdk.value match {
    case (version, Some(loc)) =>
      Seq(
        "-source", version,
        "-target", version,
        "-bootclasspath", loc.absolutePath)
    case (version, None) =>
      sLog.value.warn(
        "No configuration or property information for "+
        jdk.key.description.getOrElse(jdk.key.label))
      Seq(
        "-source", version,
        "-target", version)
    }
  }

  /**
   * Computes the additional JDK version-specific content to append to scalacOptions
   *
   * @see http://stackoverflow.com/questions/32419353/
   * @see https://blogs.oracle.com/darcy/entry/bootclasspath_older_source
   * @see https://blogs.oracle.com/darcy/entry/how_to_cross_compile_for
    * @param jdk JDK version & installation location, if available
   * @return content to append to scalacOptions
   */
  def getScalacOptionsForJDKIfAvailable(jdk: SettingKey[(String, Option[File])]) = Def.task[Seq[String]] {
    jdk.value match {
    case (version, Some(loc)) =>
      Seq(
        "-target:jvm-"+version,
        "-javabootclasspath", loc.absolutePath)
    case (version, None) =>
      sLog.value.warn(
        "No configuration or property information for "+
        jdk.key.description.getOrElse(jdk.key.label))
      Seq(
        "-target:jvm-"+version)
    }
  }

  /**
   * `publish` have a dependency on `dependencyTree`
   * so that when doing just `publish`, we'd automatically get the `dependencyTree` as well.
   */
  def dependencyGraphSettings: Seq[Setting[_]] =
    net.virtualvoid.sbt.graph.DependencyGraphSettings.graphSettings

  /**
   * @see https://tpolecat.github.io/2014/04/11/scalac-flags.html
   * @return SBT settings
   */
  def strictScalacFatalWarningsSettings: Seq[Setting[_]] =
    Seq(
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",     // yes, this is 2 args
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-unchecked",
        "-Xfatal-warnings",
        "-Xlint",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",       // N.B. doesn't work well with the ??? hole
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Xfuture",
        "-Ywarn-unused-import",   // 2.11 only
        "-Yno-imports"            // no automatic imports at all; all symbols must be imported explicitly
      ))

  /**
    * Settings to generate scaladoc for scala source code.
    *
    * Limitations (probably due to scaladoc)
    *
    * @see https://groups.google.com/forum/#!topic/scala-user/gMXOnVqTYo0
    * @see http://stackoverflow.com/questions/33715459/scaladoc-how-to-make-scaladoc-encode-method-names-like-scalaz
    * @param diagrams true: use graphviz to generate diagrams
    * @return
    */
  def scalaDocSettings(diagrams:Boolean): Seq[Setting[_]] =
    Seq(
      scalacOptions in (Compile,doc) ++=
        (if (diagrams)
          Seq("-diagrams")
        else
          Seq()
        ) ++
        Seq(
          "-doc-title", name.value,
          "-doc-root-content", baseDirectory.value + "/rootdoc.txt"
        ),
      autoAPIMappings := true,
      apiMappings <++=
        ( dependencyClasspath in Compile in doc,
          IMCEKeys.nexusJavadocRepositoryRestAPIURL2RepositoryName,
          IMCEKeys.pomRepositoryPathRegex,
          streams ) map { (deps, repoURL2Name, repoPathRegex, s) =>
          (for {
            jar <- deps
            url <- jar.metadata.get(AttributeKey[ModuleID]("moduleId")).flatMap { moduleID =>
              val urls = for {
                (repoURL, repoName) <- repoURL2Name
                (query, match2publishF) = IMCEPlugin.nexusJavadocPOMResolveQueryURLAndPublishURL(
                  repoURL, repoName, moduleID)
                url <- nonFatalCatch[Option[URL]]
                  .withApply { (_: java.lang.Throwable) => None }
                  .apply({
                    val conn = query.openConnection.asInstanceOf[java.net.HttpURLConnection]
                    conn.setRequestMethod("GET")
                    conn.setDoOutput(true)
                    repoPathRegex
                      .findFirstMatchIn(Source.fromInputStream(conn.getInputStream).getLines.mkString)
                      .map { m =>
                        val javadocURL = match2publishF(m)
                        s.log.info(s"Javadoc for: $moduleID")
                        s.log.info(s"= mapped to: $javadocURL")
                        javadocURL
                      }
                  })
              } yield url
              urls.headOption
            }
          } yield jar.data -> url).toMap
        }
    )

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

      IMCEKeys.jdk18 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.8"),
      IMCEKeys.jdk17 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.7"),
      IMCEKeys.jdk16 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.6"),

      organization := IMCEKeys.organizationInfo.value.groupId,
      organizationName := IMCEKeys.organizationInfo.value.name,
      organizationHomepage := IMCEKeys.organizationInfo.value.url,

      // disable automatic dependency on the Scala library
      autoScalaLibrary := false,

      scalaVersion := "2.11.7",

      scalacOptions in (Compile, compile) <++= getScalacOptionsForJDKIfAvailable(IMCEKeys.targetJDK),

      javacOptions in (Compile, compile) <++= getJavacOptionsForJDKIfAvailable(IMCEKeys.targetJDK),

      IMCEKeys.releaseVersionPrefix := "1800.02",

      IMCEKeys.licenseYearOrRange :=
        Calendar.getInstance()
        .getDisplayName(Calendar.YEAR, Calendar.LONG_STANDALONE, Locale.getDefault)
    )

  /**
   * SBT settings to exclude directories that do not exist.
   */
  def commonProjectDirectoriesSettings: Seq[Setting[_]] =
    Seq(
      sourceDirectories in Compile ~= { _.filter(_.exists) },
      sourceDirectories in Test ~= { _.filter(_.exists) },
      unmanagedSourceDirectories in Compile ~= { _.filter(_.exists) },
      unmanagedSourceDirectories in Test ~= { _.filter(_.exists)},
      unmanagedResourceDirectories in Compile ~= { _.filter(_.exists)},
      unmanagedResourceDirectories in Test ~= { _.filter(_.exists) }
    )

  def commonProjectMavenSettings: Seq[Setting[_]] =
    aether.AetherPlugin.autoImport.overridePublishSettings ++
    Seq(
      // do not include all repositories in the POM
      // (this is important for staging since artifacts published to a staging repository
      //  can be promoted (i.e. published) to another repository)
      pomAllRepositories := false,

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
          aether.AetherPlugin.createArtifact(artifacts, pom, coords, mainArtifact)
      }
    ) ++
      (( Option.apply(System.getProperty("JPL_LOCAL_RESOLVE_REPOSITORY")),
         Option.apply(System.getProperty("JPL_REMOTE_RESOLVE_REPOSITORY")) ) match {
        case (Some(dir), _) =>
          if (new File(dir) / "settings.xml" exists) {
            val cache = new MavenCache("JPL Resolve", new File(dir))
            Seq(resolvers += cache)
          }
          else
            sys.error(s"The JPL_LOCAL_RESOLVE_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
        case (None, Some(url)) => {
          val repo = new MavenRepository("JPL Resolve", url)
          Seq(resolvers += repo)
        }
        case _ => sys.error("Set either -DJPL_LOCAL_RESOLVE_REPOSITORY=<dir> or" +
          "-DJPL_REMOTE_RESOLVE_REPOSITORY=<url> where" +
          "<dir> is a local Maven repository directory or" +
          "<url> is a remote Maven repository URL")
      }) ++
      (( Option.apply(System.getProperty("JPL_LOCAL_PUBLISH_REPOSITORY")),
         Option.apply(System.getProperty("JPL_REMOTE_PUBLISH_REPOSITORY")) ) match {
        case (Some(dir), _) =>
          if (new File(dir) / "settings.xml" exists) {
            val cache = new MavenCache("JPL Publish", new File(dir))
            Seq(publishTo := Some(cache))
          }
          else
            sys.error(s"The JPL_LOCAL_PUBLISH_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
        case (None, Some(url)) => {
          val repo = new MavenRepository("JPL Publish", url)
          Seq(publishTo := Some(repo))
        }
        case _ => sys.error("Set either -DJPL_LOCAL_PUBLISH_REPOSITORY=<dir> or" +
          "-DJPL_REMOTE_PUBLISH_REPOSITORY=<url> where" +
          "<dir> is a local Maven repository directory or" +
          "<url> is a remote Maven repository URL")
      })

  ///**
  // * Cannot use Ivy repositories because UpdatesPlugin 0.1.8 only works with Maven repositories
  // */
  //  def imceCommonProjectIvySettings: Seq[Setting[_]] =
  //    (Option.apply(System.getProperty("JPL_IMCE_LOCAL_REPOSITORY")),
  //     Option.apply(System.getProperty("JPL_IMCE_REMOTE_REPOSITORY"))) match {
  //      case (Some(dir), _) =>
  //        val r = Resolver.file("JPL IMCE", new File(dir))(Resolver.ivyStylePatterns)
  //        Seq(
  //          publishMavenStyle := false,
  //          publishTo := Some(r),
  //          resolvers += r
  //        )
  //      case (None, Some(url)) =>
  //        val r = Resolver.url("JPL IMCE", new URL(url))(Resolver.ivyStylePatterns)
  //        Seq(
  //          publishMavenStyle := false,
  //          publishTo := Some(r),
  //          resolvers += r
  //        )
  //      case _ => sys.error("Set either -DJPL_IMCE_LOCAL_REPOSITORY=<dir> or "+
  //                           "-DJPL_IMCE_REMOTE_REPOSITORY=<url> where "+
  //                           "<dir> is a local Maven repository directory or <url> is a remote Maven repository URL")
  //    }


  /**
   * SBT settings to ensure all source files have the same license header.
   */
  def licenseSettings: Seq[Setting[_]] =
    com.banno.license.Plugin.licenseSettings ++
    Seq(
      com.banno.license.Plugin.LicenseKeys.removeExistingHeaderBlock := true,
      com.banno.license.Plugin.LicenseKeys.license :=
      s"""|
          |License Terms
          |
          |Copyright (c) ${IMCEKeys.licenseYearOrRange.value}, California Institute of Technology ("Caltech").
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
    )

  val extraPackFun: Def.Initialize[Task[Seq[(File, String)]]] = Def.task[Seq[(File, String)]] {

    def getFileIfExists(f: File, where: String)
    : Option[(File, String)] =
      if (f.exists()) Some((f, s"$where/${f.getName}")) else None

    val ivyHome: File =
      Classpaths
      .bootIvyHome(appConfiguration.value)
      .getOrElse(sys.error("Launcher did not provide the Ivy home directory."))

    // this is a workaround; how should it be done properly in sbt?

    // goal: process the list of library dependencies of the project.
    // that is, we should be able to tell the classification of each library dependency module as shown in sbt:
    //
    // > show libraryDependencies
    // [info] List(
    //    org.scala-lang:scala-library:2.11.2,
    //    org.scala-lang:scala-library:2.11.2:provided,
    //    org.scala-lang:scala-compiler:2.11.2:provided,
    //    org.scala-lang:scala-reflect:2.11.2:provided,
    //    com.typesafe:config:1.2.1:compile,
    //    org.scalacheck:scalacheck:1.11.5:compile,
    //    org.scalatest:scalatest:2.2.1:compile,
    //    org.specs2:specs2:2.4:compile,
    //    org.parboiled:parboiled:2.0.0:compile)

    // but... libraryDependencies is a SettingKey (see ld below)
    // I haven't figured out how to get the sequence of modules from it.
    val ld: SettingKey[Seq[ModuleID]] = libraryDependencies

    // workaround... I found this API that I managed to call...
    // this overrides the classification of all jars -- i.e.,
    // it is as if all library dependencies had been classified as "compile".

    // for now... it's a reasonable approaximation of the goal...
    val managed: Classpath = Classpaths.managedJars(Compile, classpathTypes.value, update.value)
    val result: Seq[(File, String)] = managed flatMap { af: Attributed[File] =>
      af.metadata.entries.toList flatMap { e: AttributeEntry[_] =>
        e.value match {
          case m: ModuleID =>
            val cachePath = s"cache/${m.organization}/${m.name}"
            Seq() ++
            getFileIfExists(new File(ivyHome, s"$cachePath/srcs/${m.name}-${m.revision}-sources.jar"), "lib.srcs") ++
            getFileIfExists(new File(ivyHome, s"$cachePath/docs/${m.name}-${m.revision}-javadoc.jar"), "lib.javadoc")
          case _ =>
            Seq()
        }
      }
    }
    result
  }

  def packageLibraryDependenciesSettings: Seq[Setting[_]] =
    xerial.sbt.Pack.packSettings ++
    xerial.sbt.Pack.publishPackArchiveZip ++
    Seq(
      xerial.sbt.Pack.packExpandedClasspath := false,
      xerial.sbt.Pack.packLibJars := Seq.empty,
      xerial.sbt.Pack.packExcludeArtifactTypes := Seq("src", "doc"),
      (mappings in xerial.sbt.Pack.pack) := {
        extraPackFun.value
      }
    )

  def packageLibraryDependenciesWithoutSourcesSettings: Seq[Setting[_]] =
    packageLibraryDependenciesSettings ++
    Seq(

      // http://www.scala-sbt.org/0.13.5/docs/Detailed-Topics/Artifacts.html
      // to disable publishing artifacts produced by `package`, `packageDoc`, `packageSrc`
      // in all configurations (Compile, Test, ...), it would seem sensible to do:
      // publishArtifact := false
      // However, this also turns off publishing maven POM metadata but surprisingly *not* Ivy metadata!
      // So instead we revert to selectively turning off publishing.

      // disable publishing the main jar produced by `package`
      publishArtifact in(Compile, packageBin) := false,

      // disable publishing the main API jar
      publishArtifact in(Compile, packageDoc) := false,

      // disable publishing the main sources jar
      publishArtifact in(Compile, packageSrc) := false,

      // disable publishing the jar produced by `test:package`
      publishArtifact in(Test, packageBin) := false,

      // disable publishing the test API jar
      publishArtifact in(Test, packageDoc) := false,

      // disable publishing the test sources jar
      publishArtifact in(Test, packageSrc) := false,

      // This is a workaround use both AetherPlugin 0.14 & sbt-pack 0.6.12
      // AetherPlugin assumes all artifacts are "jar".
      // sbt-pack produces Artifact(name.value, "arch", "zip"),
      // which works with Ivy repos but doesn't with Maven repos.
      artifact := Artifact(name.value, "zip", "zip"),
      artifacts += artifact.value,

      // normally, we would use `publishPackZipArchive` but we have to tweak the settings to work with AetherPlugin,
      // that is, make the packArchive file correspond to the file AetherPlugin expects for `artifact`
      xerial.sbt.Pack.packArchivePrefix :=
        "scala-" + scalaBinaryVersion.value + "/" + name.value + "_" + scalaBinaryVersion.value,
      packagedArtifacts += artifact.value -> xerial.sbt.Pack.packArchiveZip.value
    )

  def debugSymbolsSettings: Seq[Setting[_]] =
    Seq(
      scalacOptions in (Compile, compile) += "-g:vars",

      javacOptions in (Compile, compile) += "-g:vars"
    )

  def aspectJSettings: Seq[Setting[_]] = {

    import com.typesafe.sbt.SbtAspectj.AspectjKeys._
    import com.typesafe.sbt.SbtAspectj._

    aspectjSettings ++
    debugSymbolsSettings ++
    Seq(
      extraAspectjOptions in Aspectj := Seq("-g"),

      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj,

      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj
    )

  }

  /**
   * Generates SBT settings for the UniversalPlugin such that `univeral:packageBin`
   * will create a '*-resource.zip' archive consisting of
   * he jar, source, javadoc for Compile & Test, if available,
   * any *.md documentation and any models/\*.mdzip MD models
   *
   * @example Example usage in *.sbt or *.scala SBT file (OK to use this with Jenkins CI)
   *
   *          {{{
   *           lazy val core = Project("<sbt name, '-' separated>", file(".")).
   *             settings(GitVersioning.buildSettings). // should be unnecessary but it doesn't work without this
   *             enablePlugins(IMCEGitPlugin).
   *             settings(dynamicScriptsProjectResourceSettings(Some("<java-compatible project qualified name>")).
   *             ...
   *          }}}
    * @example Example usage in *.sbt or *.scala SBT file (don't use this with Jenkins CI!)
   *
   *          {{{
   *           lazy val core = Project("<sbt name, '-' separated>", file(".")).
   *             settings(GitVersioning.buildSettings). // should be unnecessary but it doesn't work without this
   *             enablePlugins(IMCEGitPlugin).
   *             settings(dynamicScriptsProjectResourceSettings).
   *             ...
   *          }}}
    * @param dynamicScriptsProjectName override the default dynamicScripts project name calculated
   *                                  from SBT's baseDirectory
   * @return SBT settings for the UniversalPlugin
   */
  def dynamicScriptsProjectResourceSettings(dynamicScriptsProjectName: Option[String] = None): Seq[Setting[_]] = {

    import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

    def addIfExists(f: File, name: String): Seq[(File, String)] =
      if (!f.exists) Seq()
      else Seq((f, name))

    val QUALIFIED_NAME = "^[a-zA-Z][\\w_]*(\\.[a-zA-Z][\\w_]*)*$".r

    val resourceArtifact = (name in Universal) { n =>
      Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map())
    }

    Seq(
      // the '*-resource.zip' archive will start from: 'dynamicScripts/<dynamicScriptsProjectName>'
      com.typesafe.sbt.packager.Keys.topLevelDirectory in Universal := {
        val projectName = dynamicScriptsProjectName.getOrElse(baseDirectory.value.getName)
        require(
          QUALIFIED_NAME.pattern.matcher(projectName).matches,
          s"The project name, '$projectName` is not a valid Java qualified name")
        Some("dynamicScripts/" + projectName)
      },

      // name the '*-resource.zip' in the same way as other artifacts
      com.typesafe.sbt.packager.Keys.packageName in Universal :=
      normalizedName.value + "_" + scalaBinaryVersion.value + "-" + version.value + "-resource",

      // contents of the '*-resource.zip' to be produced by 'universal:packageBin'
      mappings in Universal <++= (baseDirectory,
                                   packageBin in Compile, packageSrc in Compile, packageDoc in Compile,
                                   packageBin in Test, packageSrc in Test, packageDoc in Test) map {
                                   (dir, bin, src, doc, binT, srcT, docT) =>
                                     (dir ** "*.dynamicScripts").pair(relativeTo(dir)) ++
                                     ((dir ** "*.md") --- (dir / "sbt.staging" ***)).pair(relativeTo(dir)) ++
                                     (dir / "models" ** "*.mdzip").pair(relativeTo(dir)) ++
                                     com.typesafe.sbt.packager.MappingsHelper.directory(dir / "resources") ++
                                     addIfExists(bin, "lib/" + bin.name) ++
                                     addIfExists(binT, "lib/" + binT.name) ++
                                     addIfExists(src, "lib.sources/" + src.name) ++
                                     addIfExists(srcT, "lib.sources/" + srcT.name) ++
                                     addIfExists(doc, "lib.javadoc/" + doc.name) ++
                                     addIfExists(docT, "lib.javadoc/" + docT.name)
                                 },

      // add the '*-resource.zip' to the list of artifacts to publish; note that '.zip' will change to '.jar'
      artifacts <+= (name in Universal) { n => Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map()) },
      packagedArtifacts <+= (packageBin in Universal, name in Universal) map { (p, n) =>
        Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map()) -> p
      }
    )
  }
}