// See: https://github.com/sbt/sbt-site

enablePlugins(PreprocessPlugin)

enablePlugins(SiteScaladocPlugin)

import com.typesafe.sbt.SbtGhPages._

preprocessVars in Preprocess := Map(
  "CONTRIBUTORS" -> {
    val p1 = Process("git shortlog -sne --no-merges master")
    val p2 = Process(
      Seq("sed",
        "-e",
        """s|^\s*\([0-9][0-9]*\)\s*\(\w.*\w\)\s*<\([a-zA-Z].*\)>.*$|<tr><td align='right'>\1</td><td>\2</td><td>\3</td></tr>|"""))
    val whoswho = p1 #| p2
    whoswho.lines.mkString("\n")
  },
  "VERSION" -> {
    git.gitCurrentTags.value match {
      case Seq(tag) =>
        s"""<a href="https://github.com/JPL-IMCE/imce.sbt.plugin/tree/$tag">$tag</a>"""
      case _ =>
        val v = version.value
        git.gitHeadCommit.value.fold[String]("CASE1-" + v) { sha =>
          if (git.gitUncommittedChanges.value)
            v
          else
            s"""<a href="https://github.com/JPL-IMCE/imce.sbt.plugin/tree/$sha">$v</a>"""
        }
    }
  }
)

target in preprocess := (target in makeSite).value

ghpages.settings

makeSite <<= makeSite.dependsOn(dumpLicenseReport)

previewFixedPort := Some(4004)

previewLaunchBrowser := false