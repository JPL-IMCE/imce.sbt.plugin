package gov.nasa.jpl.mbee.sbt

import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.SbtGit.git
import sbt._
import Keys._
import scala.xml.NodeSeq

object MBEEGitPlugin extends AutoPlugin {

  override def trigger = noTrigger
  
  override def requires = MBEEPlugin && GitVersioning && GitBranchPrompt

  object autoImport {

  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] =
      mbeeGitVersioningProjectSettings

  def mbeeGitVersioningProjectSettings: Seq[Setting[_]] =
    Seq(

      // the prefix for git-based versioning of the published artifacts
      git.baseVersion := MBEEPlugin.autoImport.mbeeReleaseVersionPrefix.value,

      // turn on version detection
      git.useGitDescribe in ThisBuild := true,

      pomExtra := getGitSCMInfo

    )

  def getGitSCMInfo: NodeSeq =
    try {
      <scm>
        <connection>scm:git:{Process("git config --get remote.origin.url").lines.head}</connection>
        <tag>{Process("git symbolic-ref --short HEAD").lines.head}</tag>
        <tag>{Process("git log -n 1 HEAD --pretty=format:%H").lines.head}</tag>
      </scm>
    } catch {
      case _: Throwable => Seq()
    }
}
