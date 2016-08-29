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