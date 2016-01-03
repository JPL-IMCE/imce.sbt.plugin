package gov.nasa.jpl.imce.sbt

import com.typesafe.sbt.SbtGit._
import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import sbt.Keys._
import sbt._

import scala.xml.NodeSeq


trait IMCEGitPlugin extends AutoPlugin {

  val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r

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

      // turn on version detection
      git.useGitDescribe in ThisBuild := true,

      // Use Jean Helou's rules
      git.gitTagToVersionNumber := {
        case VersionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")
        case VersionRegex(v,"") => Some(v)
        case VersionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
        case _ => None
      },

      git.gitDescribedVersion := GitKeys.gitReader.value.withGit(_.describedVersion).flatMap(v =>
        Option(v).map(_.drop(1)).orElse(GitKeys.formattedShaVersion.value).orElse(Some(git.baseVersion.value))
      ),

      pomExtra := getGitSCMInfo,

      IMCEKeys.additionalProperties :=
        <git.branch>{git.gitCurrentBranch.value}</git.branch>
        <git.commit>{git.gitHeadCommit.value.getOrElse("N/A")+(if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
        <git.tags>{git.gitCurrentTags.value}</git.tags>,


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
      gitVersioningProjectSettings

}
