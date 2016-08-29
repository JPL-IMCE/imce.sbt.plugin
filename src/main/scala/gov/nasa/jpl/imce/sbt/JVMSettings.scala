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