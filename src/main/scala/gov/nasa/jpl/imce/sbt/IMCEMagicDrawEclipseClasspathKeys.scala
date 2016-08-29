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

import java.nio.file.Path

import sbt._

trait IMCEMagicDrawEclipseClasspathKeys {

  val mdShowMagicDrawEclipseClasspath = taskKey[Unit]("Shows MagicDraw Eclipse Classpath")

  val mdInstallDir = settingKey[Path]("MagicDraw's installation directory")

  val mdBinFolders = settingKey[List[Path]]("List of bin folder paths resolved to MagicDraw's installation directory")

  val mdLibFolders = settingKey[List[Path]]("List of lib folder paths resolved to MagicDraw's installation directory")

  val mdJars = settingKey[List[Attributed[File]]]("List of jar libraries resolved to MagicDraw's installation folder")

}

object IMCEMagicDrawEclipseClasspathKeys extends IMCEMagicDrawEclipseClasspathKeys