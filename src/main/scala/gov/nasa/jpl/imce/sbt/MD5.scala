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