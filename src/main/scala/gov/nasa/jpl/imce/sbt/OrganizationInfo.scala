package gov.nasa.jpl.imce.sbt

import sbt._

/**
 *
 * @param groupId Must be all lowercase! Can have "."
 * @param name For human consumption
 * @param url
 */
case class OrganizationInfo(groupId: String, name: String, url: Option[URL] = None) {

  /**
   * Construct an SBT ModuleID for libraryDependencies for a git-versioned jar artifact published by this organization
   *
   * @param artifactId The name of an artifact published by this organization
   * @param version The version
   * @return an sbt.ModuleID that can be added to libraryDependencies
   */
  def artifactVersion(artifactId: String, version: String): ModuleID = {
    groupId %% artifactId % version
  }

  /**
   * Construct an SBT ModuleID for libraryDependencies for a git-versioned zip artifact published by this organization
   *
   * @param artifactId The name of an artifact published by this organization
   * @param version The version
   * @return an sbt.ModuleID that can be added to libraryDependencies
   */
  def zipArtifactVersion(artifactId: String, version: String): ModuleID = {
    groupId %% artifactId % version artifacts Artifact(artifactId, "zip", "zip")
  }

}
