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

import sbt.Keys._
import sbt._

import scala.language.postfixOps

trait PackagingSettings {

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

}