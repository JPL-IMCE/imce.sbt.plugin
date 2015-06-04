package gov.nasa.jpl.mbee.sbt

import java.nio.file.Path

import sbt._

trait MBEEMagicDrawEclipseClasspathKeys {

  val mdShowMagicDrawEclipseClasspath = taskKey[Unit]("Shows MagicDraw Eclipse Classpath")

  val mdInstallDir = settingKey[Path]("MagicDraw's installation directory")

  val mdFolders = settingKey[List[Path]]("List of folders paths resolved to MagicDraw's installation directory")

  val mdJars = settingKey[List[Attributed[File]]]("List of jar libraries resolved to MagicDraw's installation folder")

}

object MBEEMagicDrawEclipseClasspathKeys extends MBEEMagicDrawEclipseClasspathKeys