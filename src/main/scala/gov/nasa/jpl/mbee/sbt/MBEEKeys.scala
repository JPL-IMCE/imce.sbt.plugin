package gov.nasa.jpl.mbee.sbt

import com.typesafe.config.{ConfigFactory, Config}
import sbt._

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

}

object MBEEKeys extends MBEEKeys

