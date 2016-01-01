package gov.nasa.jpl.imce.sbt

import com.typesafe.config.Config
import sbt._

import scala.util.matching.Regex

trait IMCEKeys {

  lazy val sbtConfig = settingKey[Config]("SBT Config properties"+
    "(defaults to application.{conf,json,properties}, override with -Dconfig.file=<path>)")

  lazy val jdk18 = settingKey[(String, Option[File])]("Location of the JDK 1.8")

  lazy val jdk17 = settingKey[(String, Option[File])]("Location of the JDK 1.7")

  lazy val jdk16 = settingKey[(String, Option[File])]("Location of the JDK 1.6")

  lazy val targetJDK = settingKey[(String, Option[File])]("Location of the target JDK version")

  lazy val organizationInfo = settingKey[OrganizationInfo](
      """The characteristics of the organization
        |(artifact groupID, organization name, and optionally, URL)""".stripMargin
    )

  lazy val releaseVersionPrefix = settingKey[String](
      """The version prefix for the next release of the JPL IMCE toolkit (e.g., "1800.02");
        | the version suffix will be generated from the Source Code Management (SCM) system (GIT, SVN)""".stripMargin
    )

  lazy val licenseYearOrRange = settingKey[String](
      """The license copyright year (e.g., "2014", "2015") or year range (e.g., "2011-2014")"""
    )

  lazy val pomRepositoryPathRegex = settingKey[Regex](
    "Regular expression to retrieve the repositoryPath of an artifact POM descriptor")

  lazy val nexusJavadocRepositoryRestAPIURL2RepositoryName = settingKey[Map[String,String]](
    """A map of Nexus repository URLs to corresponding repository names for javadoc lookup and cross-referencing.
      |Artifacts will be queried using the REST API /artifact/maven/resolve.
      |Published javadoc URLs are expected to be of the form /repositories/<repo name>/archive/<path>/!/index.html
      |where <path> will be the repository path matched from the POM result of the /artifact/maven/resolve query
      |for the given artifact coordinates.
    """.stripMargin)

  lazy val additionalProperties = settingKey[Seq[xml.Node]]("Additional entries for the POM's <properties> section")

}

object IMCEKeys extends IMCEKeys

