package gov.nasa.jpl.mbee.sbt

import java.io.File
import java.nio.file.{Files, Path, Paths}

import sbt.Keys._
import sbt._

import scala.collection.JavaConversions._


object MBEEMagicDrawEclipseClasspathPlugin extends MBEEMagicDrawEclipseClasspathPlugin {

  object autoImport extends MBEEMagicDrawEclipseClasspathKeys

  import autoImport._

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

      mdBinFolders := mdClasspathBinFolders(baseDirectory.value, mdInstallDir.value),

      mdLibFolders := mdClasspathLibFolders(baseDirectory.value, mdInstallDir.value),

      mdJars := mdClasspathJars(mdBinFolders.value, mdLibFolders.value),

      unmanagedJars in Compile <++= mdJars map identity
    )
}

trait MBEEMagicDrawEclipseClasspathPlugin extends AutoPlugin {


  val MD_CLASSPATH = "^gov.nasa.jpl.magicdraw.CLASSPATH_LIB_CONTAINER/(.*)$".r

  def mdClasspathBinFolders(eclipseProjectDir: File, mdInstallRoot: Path): List[Path] = {
    val top = scala.xml.XML.loadFile(IO.resolve(eclipseProjectDir, file(".classpath")))
    val projects = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      projectName = entry.text
      if projectName.startsWith("/")
      projectPath = mdInstallRoot.resolve("dynamicScripts"+projectName)
      projectBinPath <- (projectPath.toFile ** "bin*").get
    } yield projectBinPath.toPath

    projects.toList
  }

  def mdClasspathLibFolders(eclipseProjectDir: File, mdInstallRoot: Path): List[Path] = {
    val top = scala.xml.XML.loadFile(IO.resolve(eclipseProjectDir, file(".classpath")))
    val folders = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      variables: List[String] <- MD_CLASSPATH.unapplySeq(entry.text)
      if 1 == variables.size
    } yield for {path <- MD_PATH.findAllIn(variables.head)} yield mdInstallRoot.resolve(path.drop(1))

    val projects = for {
      cp <- top \ "classpathentry"
      entry = cp \ "@path"
      if entry.nonEmpty
      projectName = entry.text
      if projectName.startsWith("/")
      projectPath = mdInstallRoot.resolve("dynamicScripts"+projectName)
      projectLibPath = projectPath.resolve("lib")
    } yield projectLibPath

    folders.flatten.toList ++ projects.toList
  }

  val MD_PATH = ",([^,]*)".r

  def mdClasspathJars(mdBinFolders: List[Path], mdLibFolders: List[Path]): List[Attributed[File]] = {
    val jars = for {
      folder <- mdLibFolders
      if folder.toFile.exists
      jar <- Files.walk(folder).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
    } yield Attributed.blank(jar)

    val bins = for {
      folder <- mdBinFolders
    } yield Attributed.blank(folder.toFile)

    jars ++ bins
  }

}
