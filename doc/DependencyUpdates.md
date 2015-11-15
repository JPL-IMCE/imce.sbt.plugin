# How to find available updates for versioned dependencies?

## Why should you care?

See this talk: https://www.youtube.com/watch?v=W71BTkUbdqE

Forward to 17:50 where she focuses on a "Single source of truth" as one of the multiple advantages of working with a monolithic source code repository. In particular, she illustrates her point with the diamond dependency problem at 19:15. She mentions the risk of accumulating technical debt when developers postpone updating the versions of their library dependencies.

This plugin won't solve the diamond dependency problem but it helps adopt a rigorous modular software development discipline where each module is a versioned artifact (on a Maven repository somewhere) that fundamentally depends on other versioned artifacts (from Maven repositories somewhere).

## How does it work?

Suppose we have a `build.sbt` with:

```SBT
lazy val jpl_omg_oti_magicdraw_integration = Project("jpl-omg-oti-magicdraw-integration", file(".")).
  settings(GitVersioning.buildSettings). // in principle, unnecessary; in practice: doesn't work without this
  enablePlugins(MBEEGitPlugin).
  settings(MBEEPlugin.mbeePackageLibraryDependenciesWithoutSourcesSettings).
  settings(
    MBEEKeys.mbeeLicenseYearOrRange := "2014-2015",
    MBEEKeys.mbeeOrganizationInfo := MBEEPlugin.MBEEOrganizations.imce,
    libraryDependencies ++= Seq(
      MBEEPlugin.MBEEOrganizations.imce.mbeeZipArtifactVersion(
        "jpl-mbee-common-owlapi-libraries",
        MBEEKeys.mbeeReleaseVersionPrefix.value, Versions.jpl_mbee_common_scala_libraries_revision
      ) intransitive(),
      MBEEPlugin.MBEEOrganizations.oti.mbeeArtifactVersion(
        "oti-core",
        Versions.oti_core_prefix, Versions.oti_core_suffix
      ) artifacts Artifact("oti-core", "resource") intransitive(),
      MBEEPlugin.MBEEOrganizations.oti.mbeeArtifactVersion(
        "oti-change-migration",
        Versions.oti_changeMigration_prefix, Versions.oti_changeMigration_suffix
      ) artifacts Artifact("oti-change-migration", "resource") intransitive(),
      MBEEPlugin.MBEEOrganizations.oti.mbeeArtifactVersion(
        "oti-trees",
        Versions.oti_trees_prefix, Versions.oti_trees_suffix
      ) artifacts Artifact("oti-trees", "resource") intransitive(),
      MBEEPlugin.MBEEOrganizations.oti.mbeeArtifactVersion(
        "oti-magicdraw",
        Versions.oti_magicdraw_prefix, Versions.oti_magicdraw_suffix
      ) artifacts Artifact("oti-magicdraw", "resource") intransitive()
    )
  )
```

And a `project/Versions.scala` with:

```scala
object Versions {

  // JPL MBEE Common Scala Libraries
  val jpl_mbee_common_scala_libraries_revision="576ddfdcdb17e9078f75a9ec8eef1336b82c5954"

  // OTI Core version
    
  val oti_core_prefix = "0.14.0"
  val oti_core_suffix = "866"

  // OTI Change Migration version
    
  val oti_changeMigration_prefix = "0.14.0"
  val oti_changeMigration_suffix = "867"

  // OTI Trees version
    
  val oti_trees_prefix = "0.14.0"
  val oti_trees_suffix = "868"

  // OTI MagicDraw version
    
  val oti_magicdraw_prefix = "0.14.0"
  val oti_magicdraw_suffix = "869"

}
```

Then, finding available updates works like this:

```bash
jpl-omg-oti-magicdraw-integration(feature/SSCAES-1323)> dependencyUpdates
[info] Found 3 dependency updates for jpl-omg-oti-magicdraw-integration
[info]   gov.nasa.jpl.mbee.omg.oti:oti-change-migration : 0.14.0-867 -> 0.14.0-874
[info]   gov.nasa.jpl.mbee.omg.oti:oti-core             : 0.14.0-866 -> 0.14.0-872
[info]   gov.nasa.jpl.mbee.omg.oti:oti-trees            : 0.14.0-868 -> 0.14.0-873
[success] Total time: 1 s, completed Jun 15, 2015 3:51:04 PM
```
