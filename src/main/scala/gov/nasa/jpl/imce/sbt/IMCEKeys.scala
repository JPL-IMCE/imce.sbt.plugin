/*
 *
 * License Terms
 *
 * Copyright (c) 2015-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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