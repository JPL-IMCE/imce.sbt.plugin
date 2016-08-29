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

import com.typesafe.sbt.SbtGit.git
import sbt.Keys._
import sbt._

import scala.io.Source
import scala.util.control.Exception._
import scala.util.matching.Regex

trait DocSettings {

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

  /**
    * Settings to generate scaladoc for scala source code.
    * If there are uncommitted changes, the generated javadoc will be empty.
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
      sources in (Compile,doc) <<= (git.gitUncommittedChanges, sources in (Compile,compile)) map {
        (uncommitted, compileSources) =>
          if (uncommitted)
            Seq.empty
          else
            compileSources
      },

      sources in (Test,doc) <<= (git.gitUncommittedChanges, sources in (Test,compile)) map {
        (uncommitted, testSources) =>
          if (uncommitted)
            Seq.empty
          else
            testSources
      },

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
      autoAPIMappings := ! git.gitUncommittedChanges.value,
      apiMappings <++=
        ( git.gitUncommittedChanges,
          dependencyClasspath in Compile in doc,
          IMCEKeys.nexusJavadocRepositoryRestAPIURL2RepositoryName,
          IMCEKeys.pomRepositoryPathRegex,
          streams ) map { (uncommitted, deps, repoURL2Name, repoPathRegex, s) =>
          if (uncommitted)
            Map[File, URL]()
          else
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

}