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

import java.io.File
import java.nio.file.{Files, Path, Paths}

import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._


object IMCEMagicDrawEclipseClasspathPlugin extends IMCEMagicDrawEclipseClasspathPlugin {

  object autoImport extends IMCEMagicDrawEclipseClasspathKeys

  import autoImport._

  override def trigger = noTrigger

  override def requires = IMCEPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    Seq(

      mdInstallDir <<= mdInstallDir or {
        val dir =
          Option.apply(System.getenv("MD_INSTALL_DIR"))
          .orElse(Option.apply(System.getProperty("MD_INSTALL_DIR")))
          .getOrElse(
            sys.error("Set environment variable MD_INSTALL_DIR=<path> or add -DMD_INSTALL_DIR=<path>"))

        initialize { Unit => Paths.get(dir) }
      },

      mdBinFolders := mdClasspathBinFolders(baseDirectory.value, mdInstallDir.value),

      mdLibFolders := mdClasspathLibFolders(baseDirectory.value, mdInstallDir.value),

      mdJars := mdClasspathJars(mdBinFolders.value, mdLibFolders.value),

      unmanagedJars in Compile <++= mdJars map identity
    )
}

trait IMCEMagicDrawEclipseClasspathPlugin extends AutoPlugin {


  val MD_CLASSPATH = "^gov.nasa.jpl.magicdraw.CLASSPATH_LIB_CONTAINER/(.*)$".r

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

  val MD_PATH = ",([^,]*)".r

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