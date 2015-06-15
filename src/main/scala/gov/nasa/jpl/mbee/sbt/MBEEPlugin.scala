package gov.nasa.jpl.mbee.sbt

import java.io.File
import java.util.{Calendar, Locale}

import sbt.Keys._
import sbt._

import scala.language.postfixOps


object MBEEPlugin extends MBEEPlugin {

  object autoImport extends MBEEKeys

  override def trigger = allRequirements

  override def requires =
    aether.AetherPlugin &&
      com.timushev.sbt.updates.UpdatesPlugin &&
      com.typesafe.sbt.packager.universal.UniversalPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    mbeeDefaultProjectSettings ++
      mbeeLicenseSettings ++
      mbeeCommonProjectDirectoriesSettings ++
      mbeeCommonProjectMavenSettings ++
      mbeeDependencyGraphSettings

}

trait MBEEPlugin extends AutoPlugin {

  /**
   * Values intended for the organization of a packaged artifact.
   */
  object MBEEOrganizations {

    val imce = MBEEOrganizationInfo("gov.nasa.jpl.mbee.imce", "JPL IMCE Project", Some(new URL("http://imce.jpl.nasa.gov")))
    val omf = MBEEOrganizationInfo("gov.nasa.jpl.mbee.omf", "JPL IMCE Ontological Modeling Framework Project", Some(new URL("http://imce.jpl.nasa.gov")))
    val oti = MBEEOrganizationInfo("gov.nasa.jpl.mbee.omg.oti", "JPL/OMG Tool-Neutral (OTI) Project", Some(new URL("http://svn.omg.org/repos/TIWG")))
    val secae = MBEEOrganizationInfo("gov.nasa.jpl.mbee.secae", "JPL SECAE", Some(new URL("http://mbse.jpl.nasa.gov")))

  }

  /**
   * TODO: make `publish` have a dependency on `dependencyTree`
   * so that when doing just `publish`, we'd automatically get the `dependencyTree` as well.
   */
  def mbeeDependencyGraphSettings: Seq[Setting[_]] =
    net.virtualvoid.sbt.graph.Plugin.graphSettings

  /**
   * SBT settings that can projects are likely to override.
   */
  def mbeeDefaultProjectSettings: Seq[Setting[_]] =
    Seq(

      organization := MBEEKeys.mbeeOrganizationInfo.value.groupId,
      organizationName := MBEEKeys.mbeeOrganizationInfo.value.name,
      organizationHomepage := MBEEKeys.mbeeOrganizationInfo.value.url,

      // disable automatic dependency on the Scala library
      autoScalaLibrary := false,

      scalaVersion := "2.11.6",

      scalacOptions ++= Seq("-target:jvm-1.7", "-Xlint", "-deprecation"),

      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),

      MBEEKeys.mbeeReleaseVersionPrefix := "1800.02",

      MBEEKeys.mbeeLicenseYearOrRange := Calendar.getInstance().getDisplayName(Calendar.YEAR, Calendar.LONG_STANDALONE, Locale.getDefault)
    )

  /**
   * SBT settings to exclude directories that do not exist.
   */
  def mbeeCommonProjectDirectoriesSettings: Seq[Setting[_]] =
    Seq(
      sourceDirectories in Compile ~= {
        _.filter(_.exists)
      },
      sourceDirectories in Test ~= {
        _.filter(_.exists)
      },
      unmanagedSourceDirectories in Compile ~= {
        _.filter(_.exists)
      },
      unmanagedSourceDirectories in Test ~= {
        _.filter(_.exists)
      },
      unmanagedResourceDirectories in Compile ~= {
        _.filter(_.exists)
      },
      unmanagedResourceDirectories in Test ~= {
        _.filter(_.exists)
      }
    )

  def mbeeCommonProjectMavenSettings: Seq[Setting[_]] =
    aether.AetherPlugin.autoImport.overridePublishSettings ++
      Seq(
        // include repositories used in module configurations into the POM repositories section
        pomAllRepositories := true,

        // publish Maven POM metadata (instead of Ivy); this is important for the UpdatesPlugin's ability to find available updates.
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
      ((Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")), Option.apply(System.getProperty("JPL_MBEE_REMOTE_REPOSITORY"))) match {
        case (Some(dir), _) =>
          if (new File(dir) / "settings.xml" exists) {
            val cache = new MavenCache("JPL MBEE", new File(dir))
            Seq(
              publishTo := Some(cache),
              resolvers += cache)
          }
          else
            sys.error(s"The JPL_MBEE_LOCAL_REPOSITORY folder, '$dir', does not have a 'settings.xml' file.")
        case (None, Some(url)) => {
          val repo = new MavenRepository("JPL MBEE", url)
          Seq(
            publishTo := Some(repo),
            resolvers += repo)
        }
        case _ => sys.error("Set either -DJPL_MBEE_LOCAL_REPOSITORY=<dir> or -DJPL_MBEE_REMOTE_REPOSITORY=<url> where <dir> is a local Maven repository directory or <url> is a remote Maven repository URL")
      })

  /**
   * Cannot use Ivy repositories because UpdatesPlugin 0.1.8 only works with Maven repositories
   */
  //  def mbeeCommonProjectIvySettings: Seq[Setting[_]] =
  //    (Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")), Option.apply(System.getProperty("JPL_MBEE_REMOTE_REPOSITORY"))) match {
  //      case (Some(dir), _) =>
  //        val r = Resolver.file("JPL MBEE", new File(dir))(Resolver.ivyStylePatterns)
  //        Seq(
  //          publishMavenStyle := false,
  //          publishTo := Some(r),
  //          resolvers += r
  //        )
  //      case (None, Some(url)) =>
  //        val r = Resolver.url("JPL MBEE", new URL(url))(Resolver.ivyStylePatterns)
  //        Seq(
  //          publishMavenStyle := false,
  //          publishTo := Some(r),
  //          resolvers += r
  //        )
  //      case _ => sys.error("Set either -DJPL_MBEE_LOCAL_REPOSITORY=<dir> or -DJPL_MBEE_REMOTE_REPOSITORY=<url> where <dir> is a local Maven repository directory or <url> is a remote Maven repository URL")
  //    }


  /**
   * SBT settings to ensure all source files have the same license header.
   */
  def mbeeLicenseSettings: Seq[Setting[_]] = com.banno.license.Plugin.licenseSettings ++
    Seq(

      com.banno.license.Plugin.LicenseKeys.removeExistingHeaderBlock := true,

      com.banno.license.Plugin.LicenseKeys.license :=
        s"""|
           |License Terms
           |
           |Copyright (c) ${MBEEKeys.mbeeLicenseYearOrRange.value}, California Institute of Technology ("Caltech").
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
    def getFileIfExists(f: File, where: String): Option[(File, String)] = if (f.exists()) Some((f, s"$where/${f.getName}")) else None

    val ivyHome: File = Classpaths.bootIvyHome(appConfiguration.value) getOrElse sys.error("Launcher did not provide the Ivy home directory.")

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
    // this overrides the classification of all jars -- i.e., it is as if all library dependencies had been classified as "compile".

    // for now... it's a reasonable approaximation of the goal...
    val managed: Classpath = Classpaths.managedJars(Compile, classpathTypes.value, update.value)
    val result: Seq[(File, String)] = managed flatMap { af: Attributed[File] =>
      af.metadata.entries.toList flatMap { e: AttributeEntry[_] =>
        e.value match {
          case null => Seq()
          case m: ModuleID => Seq() ++
            getFileIfExists(new File(ivyHome, s"cache/${m.organization}/${m.name}/srcs/${m.name}-${m.revision}-sources.jar"), "lib.srcs") ++
            getFileIfExists(new File(ivyHome, s"cache/${m.organization}/${m.name}/docs/${m.name}-${m.revision}-javadoc.jar"), "lib.javadoc")
          case _ => Seq()
        }
      }
    }
    result
  }

  def mbeePackageLibraryDependenciesSettings: Seq[Setting[_]] =
    xerial.sbt.Pack.packSettings ++
      xerial.sbt.Pack.publishPackZipArchive ++
      Seq(
        xerial.sbt.Pack.packExpandedClasspath := false,
        xerial.sbt.Pack.packLibJars := Seq.empty,
        xerial.sbt.Pack.packExcludeArtifactTypes := Seq("src", "doc"),
        (mappings in xerial.sbt.Pack.pack) := {
          extraPackFun.value
        }
      )

  def mbeePackageLibraryDependenciesWithoutSourcesSettings: Seq[Setting[_]] =
    mbeePackageLibraryDependenciesSettings ++
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
        // sbt-pack produces Artifact(name.value, "arch", "zip"), which works with Ivy repos but doesn't with Maven repos.
        artifact := Artifact(name.value, "zip", "zip"),
        artifacts += artifact.value,

        // normally, we would use `publishPackZipArchive` but we have to tweak the settings to work with AetherPlugin,
        // that is, make the packArchive file correspond to the file AetherPlugin expects for `artifact`
        xerial.sbt.Pack.packArchivePrefix := "scala-" + scalaBinaryVersion.value + "/" + name.value + "_" + scalaBinaryVersion.value,
        packagedArtifacts += artifact.value -> xerial.sbt.Pack.packArchiveZip.value
      )

  def mbeeAspectJSettings: Seq[Setting[_]] = {

    import com.typesafe.sbt.SbtAspectj._
    import com.typesafe.sbt.SbtAspectj.AspectjKeys._

    aspectjSettings ++ Seq(
      scalacOptions += "-g:vars",

      javacOptions += "-g:vars",

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
   * Generates SBT settings for the UniversalPlugin such that `univeral:packageBin` will create a '*-resource.zip' archive
   * consisting of the jar, source, javadoc for Compile & Test, if available, any *.md documentation and any models/\*.mdzip MD models
   *
   * @param dynamicScriptsProjectName The dot-qualified Java package name of the dynamicScripts project; no '-' characters allowed
   * @return SBT settings for the UniversalPlugin
   */
  def mbeeDynamicScriptsProjectResourceSettings(dynamicScriptsProjectName: String): Seq[Setting[_]] = {

    import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

    require(!dynamicScriptsProjectName.contains("-"),"A dynamicScripts project name must be a dot-qualified Java package name, no '-' characters allowed!")

    def addIfExists(f: File, name: String): Seq[(File, String)] =
      if (!f.exists) Seq()
      else Seq((f, name))

    Seq(
      // the '*-resource.zip' archive will start from: 'dynamicScripts/<dynamicScriptsProjectName>'
      com.typesafe.sbt.packager.Keys.topLevelDirectory in Universal :=
        Some("dynamicScripts/" + dynamicScriptsProjectName),

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