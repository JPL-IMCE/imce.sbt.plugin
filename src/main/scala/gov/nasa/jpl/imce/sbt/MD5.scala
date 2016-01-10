package gov.nasa.jpl.imce.sbt

import sbt._
import spray.json._
import DefaultJsonProtocol._

case class MD5Directory
(name: String, md5: String)

case class MD5File
(name: String, md5: String)

case class MD5SubDirectory
(name: String, sub: Seq[MD5SubDirectory] = Seq(), files: Seq[MD5File] = Seq(), dirs: Seq[MD5Directory] = Seq())

object MD5JsonProtocol extends DefaultJsonProtocol {

  implicit val md5DirectoryFormat: JsonFormat[MD5Directory] =
    jsonFormat2(MD5Directory)

  implicit val md5FileFormat: JsonFormat[MD5File] =
    jsonFormat2(MD5File)

  implicit val md5SubDirectoryFormat: JsonFormat[MD5SubDirectory] =
    lazyFormat(jsonFormat(MD5SubDirectory, "name", "sub", "files", "dirs"))

}

object MD5 {

  val mDigest = {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    digest.clone match {
      case d: java.security.MessageDigest =>
        d
      case _ =>
        digest
    }
  }

  def hashBytes(bytes: Array[Byte]): String =
    mDigest.digest(bytes).map("%02x".format(_)).mkString


  def hashFile(file: java.io.File): String =
    hashBytes(IO.readBytes(file))

  def hashDirectory(dir: java.io.File): String =
    hashDirectory(Seq(dir), "")

  @annotation.tailrec
  protected def hashDirectory(queue: Seq[java.io.File], hashes: String): String =
    if (queue.isEmpty)
      hashBytes(hashes.getBytes)
    else {
      val f = queue.head
      if (f.isDirectory)
        hashDirectory(queue.tail ++ IO.listFiles(f).sorted, hashes)
      else
        hashDirectory(queue.tail, hashes + hashFile(f))
  }

  def md5File(base: java.io.File)(file: java.io.File): MD5File =
    IO.relativize(base, file) match {
      case Some(path) =>
        MD5File(name=path, md5=hashFile(file))
      case None =>
        throw new IllegalArgumentException(s"md5File(base=$base, file=$file) have no relative path!")
    }

  def md5Directory(base: java.io.File)(dir: java.io.File): MD5Directory =
    IO.relativize(base, dir) match {
      case Some(path) =>
        MD5Directory(name=path, md5=hashDirectory(dir))
      case None =>
        throw new IllegalArgumentException(s"md5Directory(base=$base, file=$dir) have no relative path!")
    }

}