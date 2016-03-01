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

import com.typesafe.config.{Config, ConfigFactory}
import scala.util.{Failure, Success, Try}

import sbt.Keys._
import sbt._

trait JVMSettings {

  def defaultJdkSettings: Seq[Setting[_]] =
    Seq(
      IMCEKeys.jdk18 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.8"),
      IMCEKeys.jdk17 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.7"),
      IMCEKeys.jdk16 := getJRERuntimeLib(IMCEKeys.sbtConfig.value, "jdk_locations.1.6"),

      scalacOptions in (Compile, compile) <++= getScalacOptionsForJDKIfAvailable(IMCEKeys.targetJDK),
      javacOptions in (Compile, compile) <++= getJavacOptionsForJDKIfAvailable(IMCEKeys.targetJDK)
    )

  /**
    * Lookup the location of the runtime library for a specific version of the JDK
    *
    * @param config The Config to lookup the JDK installation location
    * @param versionProperty The Config property path of the form 'jdk_locations.1.<N>' used to lookup the JDK location
    *                        or to map to environment property of the form 'jdk_locations_1_<N>'
    * @return A pair of the JDK version (1.<N>) and, optionally,
    *         the location of the jre/lib/rt.jar in the JDK installation folder
    */
  def getJRERuntimeLib(config: Config, versionProperty: String): (String, Option[File]) = {
    val versionEnv = versionProperty.replace('.', '_')
    val versionKey = versionProperty.stripPrefix("jdk_locations.").replace('_', '.')
    Try(config.getString(versionProperty))
    .orElse(Try(config.getString(versionEnv))) match {
      case Success(location) =>
        val rtLib = Path(location) / "jre" / "lib" / "rt.jar"
        if (rtLib.exists && !rtLib.isDirectory && rtLib.asFile.canRead)
          (versionKey, Some(rtLib.asFile))
        else
          (versionKey, None)
      case Failure(_)        =>
        (versionKey, None)
    }
  }

  /**
    * Computes the additional JDK version-specific content to append to javacOptions
    *
    * @see https://blogs.oracle.com/darcy/entry/bootclasspath_older_source
    * @see https://blogs.oracle.com/darcy/entry/how_to_cross_compile_for
    * @param jdk JDK version & installation location, if available
    * @return content to append to javacOptions
    */
  def getJavacOptionsForJDKIfAvailable(jdk: SettingKey[(String, Option[File])])
  : Def.Initialize[Task[Seq[String]]] =
    Def.task[Seq[String]] {
      jdk.value match {
        case (v, Some(loc)) =>
          Seq(
            "-source", v,
            "-target", v,
            "-bootclasspath", loc.absolutePath)
        case (v, None) =>
          sLog.value.warn(
            "No configuration or property information for "+
            jdk.key.description.getOrElse(jdk.key.label))
          Seq(
            "-source", v,
            "-target", v)
      }
    }

  /**
    * Computes the additional JDK version-specific content to append to scalacOptions
    *
    * @see http://stackoverflow.com/questions/32419353/
    * @see https://blogs.oracle.com/darcy/entry/bootclasspath_older_source
    * @see https://blogs.oracle.com/darcy/entry/how_to_cross_compile_for
    * @param jdk JDK version & installation location, if available
    * @return content to append to scalacOptions
    */
  def getScalacOptionsForJDKIfAvailable(jdk: SettingKey[(String, Option[File])]) = Def.task[Seq[String]] {
    jdk.value match {
      case (v, Some(loc)) =>
        Seq(
          "-target:jvm-"+v,
          "-javabootclasspath", loc.absolutePath)
      case (v, None) =>
        sLog.value.warn(
          "No configuration or property information for "+
          jdk.key.description.getOrElse(jdk.key.label))
        Seq(
          "-target:jvm-"+v)
    }
  }

}