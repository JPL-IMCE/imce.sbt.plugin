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

import sbt._, Keys._
import spray.json._, DefaultJsonProtocol._

import scala.util.control.Exception._
import java.nio.file.Files

class ProjectHelper(val p: Project) extends AnyVal {

  def dependsOnSourceProjectOrLibraryArtifacts
  ( projectID: String,
    projectName: String,
    libs: Seq[ModuleID]): Project =
    dependsOnSourceProjectOrLibraryArtifacts(projectID, projectName, Option.empty[String], libs)

  def dependsOnSourceProjectOrLibraryArtifacts
  ( projectID: String,
    projectName: String,
    projectConf: Option[String],
    libs: Seq[ModuleID]): Project = {
    val linksFile = p.base / "links.json"
    val projectLink: Option[File] =
      nonFatalCatch[Option[File]]
        .withApply { (t: java.lang.Throwable) => Option.empty[File] }
        .apply({
          if (linksFile.exists && linksFile.isFile) {
            val linksJSon = scala.io.Source.fromFile(linksFile).mkString
            val linksAST = linksJSon.parseJson
            val linksMap = linksAST.convertTo[Map[String, String]]
            linksMap.get(projectName) match {
              case None =>
                Option.empty[File]
              case Some(projectPath) =>
                val projectPathLink = new File(projectPath)
                if (projectPathLink.isAbsolute)
                  Some(projectPathLink)
                else
                  Some(p.base.toPath.resolve(projectPath).normalize().toRealPath().toFile)
            }
          } else
            Option.empty[File]
        })

    projectLink match {
      case None =>
        p.settings(libraryDependencies ++= libs)
      case Some(projectDir) =>
        // This must be a RootProject(uri), not ProjectRef(uri, id)
        // With a ProjectRef, SBT sees only 1 level of source-to-source project dependency.
        // With a RootProject, SBT sees the transitive closure of all source-to-source project dependencies.
        val pref = RootProject(projectDir)
        val pdep = projectConf match {
          case None =>
            p.dependsOn(pref)
          case Some(conf) =>
            p.dependsOn(pref % conf)
        }
        pdep.settings(
          clean <<= clean dependsOn (clean in ProjectRef(projectDir, projectID))
        )
    }
  }

}

object ProjectHelper {

  implicit def toProjectHelper(p: Project): ProjectHelper = new ProjectHelper(p)

}