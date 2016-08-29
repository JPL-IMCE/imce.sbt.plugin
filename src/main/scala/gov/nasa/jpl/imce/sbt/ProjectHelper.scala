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
          clean <<= clean dependsOn (clean in pref)
        )
    }
  }

}

object ProjectHelper {

  implicit def toProjectHelper(p: Project): ProjectHelper = new ProjectHelper(p)

}