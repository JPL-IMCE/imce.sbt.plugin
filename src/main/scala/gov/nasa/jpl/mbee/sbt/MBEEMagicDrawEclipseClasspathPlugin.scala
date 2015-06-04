package gov.nasa.jpl.mbee.sbt

import java.io.File
import java.nio.file.{Files, Path, Paths}

import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._

object MBEEMagicDrawEclipseClasspathKeys {

  val mdShowMagicDrawEclipseClasspath = taskKey[Unit]("Shows MagicDraw Eclipse Classpath")

  val mdInstallDir = settingKey[Path]("MagicDraw's installation directory")

  val mdFolders = settingKey[List[Path]]("List of folders paths resolved to MagicDraw's installation directory")

  val mdJars = settingKey[List[Attributed[File]]]("List of jar libraries resolved to MagicDraw's installation folder")

}

import gov.nasa.jpl.mbee.sbt.MBEEMagicDrawEclipseClasspathKeys._

object MBEEMagicDrawEclipseClasspathPlugin extends MBEEMagicDrawEclipseClasspathPlugin {

  override def trigger = noTrigger

  override def requires = MBEEPlugin

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  override def projectSettings: Seq[Setting[_]] =
    Seq(

      mdInstallDir <<= mdInstallDir or {
        Option.apply(System.getenv("MD_INSTALL_DIR")) match {
          case Some(dir) => initialize { Unit => Paths.get(dir) }
          case None => Option.apply(System.getProperty("MD_INSTALL_DIR")) match {
            case Some(dir) => initialize { Unit => Paths.get(dir) }
            case None => sys.error("Set environment variable MD_INSTALL_DIR=<path> or add -DMD_INSTALL_DIR=<path>")
          }
        }
      },

      mdFolders := mdClasspathFolders(baseDirectory.value, mdInstallDir.value),

      mdJars := mdClasspathJars(mdFolders.value),

      unmanagedJars in Compile <++= mdJars map identity
    )
}

trait MBEEMagicDrawEclipseClasspathPlugin extends AutoPlugin {


  val MD_CLASSPATH = "^gov.nasa.jpl.magicdraw.CLASSPATH_LIB_CONTAINER/(.*)$".r

  def mdClasspathFolders(eclipseProjectDir: File, mdInstallRoot: Path): List[Path] = {
    println(s"dir=$eclipseProjectDir")
    val top = scala.xml.XML.loadFile(IO.resolve(eclipseProjectDir, file(".classpath")))
    val folders = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      variables: List[String] <- MD_CLASSPATH.unapplySeq(entry.text)
      if 1 == variables.size
    } yield for {path <- MD_PATH.findAllIn(variables.head)} yield mdInstallRoot.resolve(path.drop(1))
    folders.flatten.toList
  }

  val MD_PATH = ",([^,]*)".r

  def mdClasspathJars(mdFolders: List[Path]): List[Attributed[File]] =
    for {
      folder <- mdFolders
      jar <- Files.walk(folder).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
    } yield Attributed.blank(jar)

}
