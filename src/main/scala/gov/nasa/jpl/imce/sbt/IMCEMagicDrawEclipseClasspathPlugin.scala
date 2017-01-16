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

import java.io.File
import java.nio.file.{Files, Path, Paths}

import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._
import scala.util.matching.Regex


object IMCEMagicDrawEclipseClasspathPlugin extends IMCEMagicDrawEclipseClasspathPlugin {

  object autoImport extends IMCEMagicDrawEclipseClasspathKeys

  import autoImport._

  override def trigger: PluginTrigger = noTrigger

  override def requires: Plugins = IMCEPlugin

  override def buildSettings: Seq[Setting[_]] = Seq()

  override def projectSettings
  : Seq[Setting[_]]
  = Seq(

      mdInstallDir := mdInstallDir.?.value.getOrElse {
        val dir =
          Option.apply(System.getenv("MD_INSTALL_DIR"))
          .orElse(Option.apply(System.getProperty("MD_INSTALL_DIR")))
          .getOrElse(
            sys.error("Set environment variable MD_INSTALL_DIR=<path> or add -DMD_INSTALL_DIR=<path>"))

        Paths.get(dir)
      },

      mdBinFolders := mdClasspathBinFolders(baseDirectory.value, mdInstallDir.value),

      mdLibFolders := mdClasspathLibFolders(baseDirectory.value, mdInstallDir.value),

      mdJars := mdClasspathJars(mdBinFolders.value, mdLibFolders.value),

      unmanagedJars in Compile ++= (mdJars map identity).value
    )
}

trait IMCEMagicDrawEclipseClasspathPlugin extends AutoPlugin {


  val MD_CLASSPATH: Regex = "^gov.nasa.jpl.magicdraw.CLASSPATH_LIB_CONTAINER/(.*)$".r

  def mdClasspathBinFolders(eclipseProjectDir: File, mdInstallRoot: Path): List[Path] = {
    val top = scala.xml.XML.loadFile(IO.resolve(eclipseProjectDir, file(".classpath")))
    val projects = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      projectName = entry.text
      if projectName.startsWith("/")
      projectPath = mdInstallRoot.resolve("dynamicScripts"+projectName)
      projectBinPath <- (projectPath.toFile ** "bin*").get
      if projectBinPath.isDirectory && projectBinPath.canExecute && projectBinPath.canRead
    } yield projectBinPath.toPath

    projects.toList
  }

  def mdClasspathLibFolders(eclipseProjectDir: File, mdInstallRoot: Path): List[Path] = {
    val top = scala.xml.XML.loadFile(IO.resolve(eclipseProjectDir, file(".classpath")))
    val folders = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      variables: List[String] <- MD_CLASSPATH.unapplySeq(entry.text)
      if 1 == variables.size
    } yield for {path <- MD_PATH.findAllIn(variables.head)} yield mdInstallRoot.resolve(path.drop(1))

    val projects = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      projectName = entry.text
      if projectName.startsWith("/")
      projectPath = mdInstallRoot.resolve("dynamicScripts"+projectName)
      projectLibPath = projectPath.resolve("lib").toFile
      if projectLibPath.isDirectory && projectLibPath.canExecute && projectLibPath.canRead
    } yield projectLibPath.toPath

    folders.flatten.toList ++ projects.toList
  }

  val MD_PATH: Regex = ",([^,]*)".r

  def mdClasspathJars(mdBinFolders: List[Path], mdLibFolders: List[Path]): List[Attributed[File]] = {
    val jars = for {
      folder <- mdLibFolders
      if folder.toFile.exists
      jar <- Files.walk(folder).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
    } yield Attributed.blank(jar)

    val bins = for {
      folder <- mdBinFolders
    } yield Attributed.blank(folder.toFile)

    jars ++ bins
  }

}