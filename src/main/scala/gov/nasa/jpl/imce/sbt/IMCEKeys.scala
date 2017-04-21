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

import com.typesafe.config.Config
import sbt._

import scala.util.matching.Regex

trait IMCEKeys {

  lazy val buildUTCDate = settingKey[String]("The UDC Date of the build")

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

  lazy val licenseYearOrRange = settingKey[String](
      """The license copyright year (e.g., "2014", "2015") or year range (e.g., "2011-2014")"""
    )

  lazy val pomRepositoryPathRegex = settingKey[Regex](
    "Regular expression to retrieve the repositoryPath of an artifact POM descriptor")

  lazy val nexusJavadocRepositoryRestAPIURL2RepositoryName = settingKey[Map[String,String]](
    """A map of Nexus repository URLs to corresponding repository names for javadoc lookup and cross-referencing.
      |Artifacts will be queried using the REST API /artifact/maven/resolve.
      |Published javadoc URLs are expected to be of the form /repositories/<repo name>/archive/<path>/!/index.md
      |where <path> will be the repository path matched from the POM result of the /artifact/maven/resolve query
      |for the given artifact coordinates.
    """.stripMargin)

  lazy val additionalProperties = settingKey[Seq[xml.Node]]("Additional entries for the POM's <properties> section")

}

object IMCEKeys extends IMCEKeys