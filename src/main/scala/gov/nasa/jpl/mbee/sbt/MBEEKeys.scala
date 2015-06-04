package gov.nasa.jpl.mbee.sbt

import sbt._

trait MBEEKeys {

    val mbeeOrganizationInfo = settingKey[MBEEOrganizationInfo](
      """The characteristics of the MBEE organization (artifact groupID, organization name, and optionally, URL)"""
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

