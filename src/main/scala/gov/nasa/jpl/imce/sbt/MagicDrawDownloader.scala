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

import java.io.{File, FileOutputStream, SequenceInputStream}
import java.nio.charset.StandardCharsets

import sbt._
import Keys._

import scala.xml._
import scala.collection._
import scala.{Boolean,Console,Double,Unit}
import scala.language.postfixOps
import scala.math._

object MagicDrawDownloader {

  def fetchMagicDraw
  (log: Logger,
   showDownloadProgress: Boolean,
   up: UpdateReport,
   credentials: Seq[Credentials],
   mdInstallDir: File,
   mdZip: File): Unit = {

    IO.createDirectory(mdInstallDir)

    val tfilter: DependencyFilter = new DependencyFilter {
      def apply(c: String, m: ModuleID, a: Artifact): Boolean =
        a.extension == "pom" &&
          m.organization.startsWith("org.omg.tiwg.vendor.nomagic") &&
          m.name.startsWith("com.nomagic.magicdraw.package")
    }

    up
      .matching(tfilter)
      .headOption
      .fold[Unit](log.warn("No MagicDraw POM artifact found!")) {
      pom =>
        // Use unzipURL to download & extract
        //val files = IO.unzip(zip, mdInstallDir)
        val mdNoInstallZipDownloadURL = new URL(((XML.load(pom.absolutePath) \\ "properties") \ "md.core").text)

        log.info(
          s"=> found: ${pom.getName} at $mdNoInstallZipDownloadURL")

        // Get the credentials based on host
        credentials
          .flatMap {
            case dc: DirectCredentials if dc.host == mdNoInstallZipDownloadURL.getHost =>
              Some(dc)
            case _ =>
              None
          }
          .headOption
          .fold[Unit](log.error(s"=> failed to get credentials for downloading MagicDraw no_install zip")) {
          mdCredentials =>

            // 1. If no credentials are found, attempt a connection without basic authorization
            // 2. If username and password cannot be extracted (e.g., unsupported FileCredentials),
            //    then throw error
            // 3. If authorization wrong, ensure that SBT aborts

            val connection = mdNoInstallZipDownloadURL.openConnection()

            connection
              .setRequestProperty(
                "Authorization",
                "Basic " + java.util.Base64.getEncoder.encodeToString(
                  (mdCredentials.userName + ":" + mdCredentials.passwd)
                    .getBytes(StandardCharsets.UTF_8))
              )

            // Download the file into /target
            val size = connection.getContentLengthLong
            val input = connection.getInputStream
            val output = new FileOutputStream(mdZip)

            log.info(s"=> Downloading $size bytes (= ${size / 1024 / 1024} MB)...")

            val bytes = new Array[Byte](1024 * 1024)
            var totalBytes: Double = 0
            Iterator
              .continually(input.read(bytes))
              .takeWhile(-1 != _)
              .foreach { read =>
                totalBytes += read
                output.write(bytes, 0, read)

                if (showDownloadProgress) {
                  Console.printf(
                    "    %.2f MB / %.2f MB (%.1f%%)\r",
                    totalBytes / 1024 / 1024,
                    size * 1.0 / 1024.0 / 1024.0,
                    (totalBytes / size) * 100)
                }
              }

            output.close()

            // Use unzipURL to download & extract
            val files = IO.unzip(mdZip, mdInstallDir)
            log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip located at: $mdNoInstallZipDownloadURL")
        }
    }

  }

  def fetchSysMLPlugin
  (log: Logger,
   showDownloadProgress: Boolean,
   up: UpdateReport,
   credentials: Seq[Credentials],
   mdInstallDir: File,
   mdZip: File): Unit = {

    val tfilter: DependencyFilter = new DependencyFilter {
      def apply(c: String, m: ModuleID, a: Artifact): Boolean =
        a.extension == "pom" &&
          m.organization.startsWith("org.omg.tiwg.vendor.nomagic") &&
          m.name.startsWith("com.nomagic.magicdraw.sysml.plugin")
    }

    up
      .matching(tfilter)
      .headOption
      .fold[Unit](log.warn("No MagicDraw SysML Plugin POM artifact found!")) {
      pom =>
        // Use unzipURL to download & extract
        //val files = IO.unzip(zip, mdInstallDir)
        val mdSysMLPluginZipDownloadURL = new URL(((XML.load(pom.absolutePath) \\ "properties") \ "md.core").text)

        log.info(
          s"=> found: ${pom.getName} at $mdSysMLPluginZipDownloadURL")

        // Get the credentials based on host
        credentials
          .flatMap {
            case dc: DirectCredentials if dc.host == mdSysMLPluginZipDownloadURL.getHost =>
              Some(dc)
            case _ =>
              None
          }
          .headOption
          .fold[Unit](log.error(s"=> failed to get credentials for downloading MagicDraw SysML plugin zip")) {
          mdCredentials =>

            // 1. If no credentials are found, attempt a connection without basic authorization
            // 2. If username and password cannot be extracted (e.g., unsupported FileCredentials),
            //    then throw error
            // 3. If authorization wrong, ensure that SBT aborts

            val connection = mdSysMLPluginZipDownloadURL.openConnection()

            connection
              .setRequestProperty(
                "Authorization",
                "Basic " + java.util.Base64.getEncoder.encodeToString(
                  (mdCredentials.userName + ":" + mdCredentials.passwd)
                    .getBytes(StandardCharsets.UTF_8))
              )

            // Download the file into /target
            val size = connection.getContentLengthLong
            val input = connection.getInputStream
            val output = new FileOutputStream(mdZip)

            log.info(s"=> Downloading $size bytes (= ${size / 1024 / 1024} MB)...")

            val bytes = new Array[Byte](1024 * 1024)
            var totalBytes: Double = 0
            Iterator
              .continually(input.read(bytes))
              .takeWhile(-1 != _)
              .foreach { read =>
                totalBytes += read
                output.write(bytes, 0, read)

                if (showDownloadProgress) {
                  Console.printf(
                    "    %.2f MB / %.2f MB (%.1f%%)\r",
                    totalBytes / 1024 / 1024,
                    size * 1.0 / 1024.0 / 1024.0,
                    (totalBytes / size) * 100)
                }
              }

            output.close()

            // Use unzipURL to download & extract
            val files = IO.unzip(mdZip, mdInstallDir)
            val pluginFiles = files
              .filter(
                f => f.isFile && f.getName.endsWith("zip"))
              .flatMap(
                f => IO.unzip(f, mdInstallDir))

            log.info(
              s"=> copied SysML plugin into md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip (containing ${pluginFiles.size}) located at: $mdSysMLPluginZipDownloadURL")
        }
    }

  }

}
