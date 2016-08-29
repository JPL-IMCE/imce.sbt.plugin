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

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import sbt.Keys._
import sbt._

import scala.xml.NodeSeq


trait IMCEGitPlugin extends AutoPlugin {

  val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

  def gitVersioningBuildSettings: Seq[Setting[_]] =
    Seq(
      // Use Jean Helou's rules
      git.gitTagToVersionNumber in ThisBuild := {
        case VersionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")
        case VersionRegex(v,"") => Some(v)
        case VersionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
        case _ => None
      },

      git.gitDescribedVersion in ThisBuild :=
        GitKeys.gitReader.value.withGit(_.describedVersion).flatMap(v =>
          Option(v)
            .map(_.drop(1))
            .orElse(GitKeys.formattedShaVersion.value)
            .orElse(Some(git.baseVersion.value))
        ),

      // turn on version detection
      git.useGitDescribe in ThisBuild := true

    )

  /**
    * Settings for git-based versioning.
    *
    * @note Usage:
    *       ```git.baseVersion := "1.0.0"```
    *       This scheme yields the following versions in order:
    *       0.0.0-SNAPSHOT
    *       0.0.0-xxxxx-SNAPSHOT //with xxxxxx a SHA-1
    *       1.0.0 // for a commit whose SHA-1 has been tagged with v1.0.0
    *       1.0.0-2-yyyyy-SNAPSHOT // for the second commit after the tag
    *
    * @see http://blog.byjean.eu/2015/07/10/painless-release-with-sbt.html
    *
    * @return SBT settings
    */
  def gitVersioningProjectSettings: Seq[Setting[_]] =
    Seq(

      pomExtra := getGitSCMInfo,

      IMCEKeys.additionalProperties := {
        <git.branch>
          {git.gitCurrentBranch.value}
        </git.branch>
        <git.commit>
          {git.gitHeadCommit.value.getOrElse("N/A") + (if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}
        </git.commit>
        <git.tags>
          {git.gitCurrentTags.value}
        </git.tags>
        <md5/>
      },


      pomPostProcess <<= IMCEKeys.additionalProperties { (additions) =>
        new xml.transform.RuleTransformer(new xml.transform.RewriteRule {
          override def transform(n: xml.Node): Seq[xml.Node] =
            n match {
              case <properties>{props @ _*}</properties> =>
                <properties>{props}{additions}</properties>
              case _ =>
                n
            }
        })
      }

    )

  def getGitSCMInfo: NodeSeq =
    try {
      <scm>
        <connection>scm:git:
          {Process("git config --get remote.origin.url").lines.head}
        </connection>
        <tag>
          {Process("git symbolic-ref --short HEAD").lines.head}
        </tag>
        <tag>
          {Process("git log -n 1 HEAD --pretty=format:%H").lines.head}
        </tag>
      </scm>
    } catch {
      case _: Throwable => Seq()
    }
}

object IMCEGitPlugin extends IMCEGitPlugin {

  override def trigger = noTrigger

  override def requires = IMCEPlugin && GitVersioning && GitBranchPrompt

  override def buildSettings: Seq[Setting[_]] =
    gitVersioningBuildSettings ++
      GitVersioning.buildSettings

  override def projectSettings: Seq[Setting[_]] =
    gitVersioningProjectSettings

}