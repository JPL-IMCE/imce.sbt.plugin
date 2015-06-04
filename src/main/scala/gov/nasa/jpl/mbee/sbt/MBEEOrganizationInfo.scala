package gov.nasa.jpl.mbee.sbt

import sbt._

/**
 *
 * @param groupId Must be all lowercase! Can have "."
 * @param name For human consumption
 * @param url
 */
case class MBEEOrganizationInfo(groupId: String, name: String, url: Option[URL] = None)
