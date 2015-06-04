package gov.nasa.jpl.mbee.sbt

import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import sbt.Keys._
import sbt._

import scala.xml.NodeSeq

object MBEEGitPlugin extends MBEEGitPlugin {

  override def trigger = noTrigger

  override def requires = MBEEPlugin && GitVersioning && GitBranchPrompt

  override def buildSettings: Seq[Setting[_]] =
    Seq()

  // Somehow, GitVersioning.buildSettings is inconsistently propagated in projects.
  // The telltale sign looks like this:
  // java.lang.RuntimeException: Setting value cannot be null: {file:/Users/rouquett/sbt.tests/A/}/*:version
  // at scala.sys.package$.error(package.scala:27)
  // at sbt.EvaluateSettings$INode.setValue(INode.scala:143)
  // at sbt.EvaluateSettings$MixedNode.evaluate0(INode.scala:175)
  // at sbt.EvaluateSettings$INode.evaluate(INode.scala:135)
  // at sbt.EvaluateSettings$$anonfun$sbt$EvaluateSettings$$submitEvaluate$1.apply$mcV$sp(INode.scala:69)
  // at sbt.EvaluateSettings.sbt$EvaluateSettings$$run0(INode.scala:78)
  // at sbt.EvaluateSettings$$anon$3.run(INode.scala:74)
  // at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
  // at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
  // at java.lang.Thread.run(Thread.java:745)
  // [error] Setting value cannot be null: {file:/Users/rouquett/sbt.tests/A/}/*:version
  // [error] Use 'last' for the full log.
  // Project loading failed: (r)etry, (q)uit, (l)ast, or (i)gnore?

  override def projectSettings: Seq[Setting[_]] =
    GitVersioning.buildSettings ++
      mbeeGitVersioningProjectSettings

}

trait MBEEGitPlugin extends AutoPlugin {


  def mbeeGitVersioningProjectSettings: Seq[Setting[_]] =
    Seq(

      // the prefix for git-based versioning of the published artifacts
      git.baseVersion := MBEEKeys.mbeeReleaseVersionPrefix.value,

      // turn on version detection
      git.useGitDescribe in ThisBuild := true,

      pomExtra := getGitSCMInfo

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
