package gov.nasa.jpl.mbee.sbt

import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import com.typesafe.sbt.SbtGit.git
import sbt._

object MBEEGitPlugin extends AutoPlugin {

  override def requires = MBEEPlugin && GitVersioning && GitBranchPrompt

  object autoImport {

  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = customizableMBEEGitProjectSettings ++ derivedMBEEGitProjectSettings

  def customizableMBEEGitProjectSettings: Seq[Setting[_]] =
    Seq(
    )


  private def derivedMBEEGitProjectSettings: Seq[Setting[_]] =
    Seq(

      // the prefix for git-based versioning of the published artifacts
      git.baseVersion := MBEEPlugin.autoImport.mbeeReleaseVersionPrefix.value,

      // turn on version detection
      git.useGitDescribe in ThisBuild := true
    )
}
