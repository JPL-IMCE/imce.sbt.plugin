package gov.nasa.jpl.mbee.sbt

import java.nio.file.Path

import sbt._

trait MBEEMagicDrawEclipseClasspathKeys {

  val mdShowMagicDrawEclipseClasspath = taskKey[Unit]("Shows MagicDraw Eclipse Classpath")

  val mdInstallDir = settingKey[Path]("MagicDraw's installation directory")

  val mdBinFolders = settingKey[List[Path]]("List of bin folder paths resolved to MagicDraw's installation directory")

  val mdLibFolders = settingKey[List[Path]]("List of lib folder paths resolved to MagicDraw's installation directory")

  val mdJars = settingKey[List[Attributed[File]]]("List of jar libraries resolved to MagicDraw's installation folder")

}

object MBEEMagicDrawEclipseClasspathKeys extends MBEEMagicDrawEclipseClasspathKeys