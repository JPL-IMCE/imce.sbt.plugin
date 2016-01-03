package gov.nasa.jpl.imce.sbt

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

}
