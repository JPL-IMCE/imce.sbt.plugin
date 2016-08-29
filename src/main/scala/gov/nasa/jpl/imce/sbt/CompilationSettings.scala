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

import sbt.Keys._
import sbt._

trait CompilationSettings {

  /**
    * Aspectj settings for load-time weaving of aspects (i.e. compile aspects only; no compile-time weaving)
    *
    * @return SBT settings
    */
  def aspectJSettings: Seq[Setting[_]] = {

    import com.typesafe.sbt.SbtAspectj.AspectjKeys._
    import com.typesafe.sbt.SbtAspectj._

    aspectjSettings ++
    debugSymbolsSettings ++
    Seq(
      extraAspectjOptions in Aspectj := Seq("-g"),

      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj
    )

  }

  /**
    * Javac & Scalac compiler options to include debug symbols.
    *
    * @return SBT settings
    */
  def debugSymbolsSettings: Seq[Setting[_]] =
    Seq(
      scalacOptions in (Compile, compile) += "-g:vars",

      scalacOptions in (Compile, test) += "-g:vars",

      javacOptions in (Compile, compile) += "-g:vars",

      javacOptions in (Compile, test) += "-g:vars"
    )

  /**
    * Strict Scala compiler flags:
    *
    * - all warnings are errors
    * - no dead code
    * - no implicit numeric widening
    * - no discarded value
    * - no unused imports
    * - no adapted arguments (e.g. auto-tupling)
    * - no unchecked types
    * - no deprecated APIs
    *
    * @see https://tpolecat.github.io/2014/04/11/scalac-flags.html
    *      http://blog.threatstack.com/useful-scalac-options-for-better-scala-development-part-1
    *
    * @return SBT settings
    */
  def strictScalacFatalWarningsSettings: Seq[Setting[_]] =
    Seq(
      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",     // yes, this is 2 args
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-unchecked",
        "-Xfatal-warnings",
        "-Xlint",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",       // N.B. doesn't work well with the ??? hole
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Xfuture",
        "-Ywarn-unused-import",   // 2.11 only
        "-Yno-imports"            // no automatic imports at all; all symbols must be imported explicitly
      ))
}