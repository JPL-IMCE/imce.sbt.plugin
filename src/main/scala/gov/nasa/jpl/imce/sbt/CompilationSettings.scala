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