
// publish to bintray.com via: `sbt publish`
publishTo := Some(
  "JPL-IMCE" at
    s"https://api.bintray.com/content/jpl-imce/${organization.value}/${moduleName.value}/${version.value}")

PgpKeys.useGpg := true

PgpKeys.useGpgAgent := true

pgpSecretRing := file("local.secring.gpg")

pgpPublicRing := file("local.pubring.gpg")

// include *.pom as an artifact
publishMavenStyle := true

// do not include all repositories in the POM
pomAllRepositories := false

// make sure no repositories show up in the POM file
pomIncludeRepository := { _ => false }

pomPostProcess := {
  new xml.transform.RuleTransformer(new xml.transform.RewriteRule {
    val additions: Seq[xml.Node] = {
      <git.branch>{git.gitCurrentBranch.value}</git.branch>
      <git.commit>{git.gitHeadCommit.value.getOrElse("N/A") + (if (git.gitUncommittedChanges.value) "-SNAPSHOT" else "")}</git.commit>
    } ++ {
      git.gitCurrentTags.value.map(tag => <git.tag>{tag}</git.tag>)
    }

    override def transform(n: xml.Node): Seq[xml.Node] =
      n match {
        case <properties>{props@_*}</properties> =>
          <properties>{props}{additions}</properties>
        case _ =>
          n
      }
  })
}

git.baseVersion := Versions.version

git.useGitDescribe := true

versionWithGit
