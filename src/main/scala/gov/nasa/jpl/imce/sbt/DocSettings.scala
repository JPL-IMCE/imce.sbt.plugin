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