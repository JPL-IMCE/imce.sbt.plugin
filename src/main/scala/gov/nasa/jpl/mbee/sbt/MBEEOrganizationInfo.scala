package gov.nasa.jpl.mbee.sbt

import sbt._

/**
 *
 * @param groupId Must be all lowercase! Can have "."
 * @param name For human consumption
 * @param url
 */
case class MBEEOrganizationInfo(groupId: String, name: String, url: Option[URL] = None) {

  /**
   * Construct an SBT ModuleID for libraryDependencies for a git-versioned jar artifact published by this organization
   *
   * @param artifactId The name of an artifact published by this organization
   * @param versionPrefix The version prefix, typically, MBEEKeys.mbeeReleaseVersionPrefix.value
   * @param versionSuffix The version suffix, typically, the 40-character GIT hash
   * @return an sbt.ModuleID that can be added to libraryDependencies
   */
  def mbeeArtifactVersion(artifactId: String, versionPrefix: String, versionSuffix: String): ModuleID = {
    val version: String = versionPrefix+"-"+versionSuffix
    groupId %% artifactId % version
  }

  /**
   * Construct an SBT ModuleID for libraryDependencies for a git-versioned zip artifact published by this organization
   *
   * @param artifactId The name of an artifact published by this organization
   * @param versionPrefix The version prefix, typically, MBEEKeys.mbeeReleaseVersionPrefix.value
   * @param versionSuffix The version suffix, typically, the 40-character GIT hash
   * @return an sbt.ModuleID that can be added to libraryDependencies
   */
  def mbeeZipArtifactVersion(artifactId: String, versionPrefix: String, versionSuffix: String): ModuleID = {
    val version: String = versionPrefix+"-"+versionSuffix
    groupId %% artifactId % version artifacts Artifact(artifactId, "zip", "zip")
  }

}
