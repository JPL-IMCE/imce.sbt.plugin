package gov.nasa.jpl.mbee.sbt

import com.typesafe.config.{ConfigFactory, Config}
import sbt._
import scala.util.matching.Regex

trait MBEEKeys {

  lazy val mbeeSBTConfig = settingKey[Config](
    "SBT Config properties (defaults to application.{conf,json,properties}," +
    " override with -Dconfig.file=<path>")

  lazy val jdk18 = settingKey[(String, Option[File])]("Location of the JDK 1.8")

  lazy val jdk17 = settingKey[(String, Option[File])]("Location of the JDK 1.7")

  lazy val jdk16 = settingKey[(String, Option[File])]("Location of the JDK 1.6")

  lazy val targetJDK = settingKey[(String, Option[File])]("Location of the target JDK version")

  val mbeeOrganizationInfo = settingKey[MBEEOrganizationInfo](
      """The characteristics of the MBEE organization
        |(artifact groupID, organization name, and optionally, URL)""".stripMargin
    )

  val mbeeReleaseVersionPrefix = settingKey[String](
      """The version prefix for the next release of the JPL MBEE toolkit (e.g., "1800.02");
        | the version suffix will be generated from the Source Code Management (SCM) system (GIT, SVN)""".stripMargin
    )

  val mbeeLicenseYearOrRange = settingKey[String](
      """The license copyright year (e.g., "2014", "2015") or year range (e.g., "2011-2014")"""
    )

  val mbeePOMRepositoryPathRegex = settingKey[Regex](
    "Regular expression to retrieve the repositoryPath of an artifact POM descriptor")

  val mbeeNexusJavadocRepositoryRestAPIURL = settingKey[String](
    """The URL for the REST API of a Nexus repository for both javadoc artifacts and published javadoc.
      |Artifacts will be queried using the REST API /artifact/maven/resolve.
      |Published javadoc URLs are expected to be of the form /repositories/<repo name>/archive/<path>/!/index.html
      |where <path> will be the repository path matched from the POM result of the /artifact/maven/resolve query
      |for the given artifact coordinates.
    """.stripMargin)

  val mbeeNexusJavadocRepositoryName = settingKey[String](
    "The name of the repository to resolve javadoc artifacts and access their published javadoc")

}

object MBEEKeys extends MBEEKeys

