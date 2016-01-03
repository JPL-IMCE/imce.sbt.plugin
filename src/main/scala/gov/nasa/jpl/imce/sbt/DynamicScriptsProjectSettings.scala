package gov.nasa.jpl.imce.sbt

import sbt.Keys._
import sbt._

import scala.language.postfixOps

trait DynamicScriptsProjectSettings {

  /**
    * Generates SBT settings for the UniversalPlugin such that `univeral:packageBin`
    * will create a '*-resource.zip' archive consisting of
    * he jar, source, javadoc for Compile & Test, if available,
    * any *.md documentation and any models/\*.mdzip MD models
    *
    * @example Example usage in *.sbt or *.scala SBT file (OK to use this with Jenkins CI)
    *
    *          {{{
    *           lazy val core = Project("<sbt name, '-' separated>", file(".")).
    *             settings(GitVersioning.buildSettings). // should be unnecessary but it doesn't work without this
    *             enablePlugins(IMCEGitPlugin).
    *             settings(dynamicScriptsProjectResourceSettings(Some("<java-compatible project qualified name>")).
    *             ...
    *          }}}
    * @example Example usage in *.sbt or *.scala SBT file (don't use this with Jenkins CI!)
    *
    *          {{{
    *           lazy val core = Project("<sbt name, '-' separated>", file(".")).
    *             settings(GitVersioning.buildSettings). // should be unnecessary but it doesn't work without this
    *             enablePlugins(IMCEGitPlugin).
    *             settings(dynamicScriptsProjectResourceSettings).
    *             ...
    *          }}}
    * @param dynamicScriptsProjectName override the default dynamicScripts project name calculated
    *                                  from SBT's baseDirectory
    * @return SBT settings for the UniversalPlugin
    */
  def dynamicScriptsProjectResourceSettings(dynamicScriptsProjectName: Option[String] = None): Seq[Setting[_]] = {

    import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

    def addIfExists(f: File, name: String): Seq[(File, String)] =
      if (!f.exists) Seq()
      else Seq((f, name))

    val QUALIFIED_NAME = "^[a-zA-Z][\\w_]*(\\.[a-zA-Z][\\w_]*)*$".r

    val resourceArtifact = (name in Universal) { n =>
      Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map())
    }

    Seq(
      // the '*-resource.zip' archive will start from: 'dynamicScripts/<dynamicScriptsProjectName>'
      com.typesafe.sbt.packager.Keys.topLevelDirectory in Universal := {
        val projectName = dynamicScriptsProjectName.getOrElse(baseDirectory.value.getName)
        require(
          QUALIFIED_NAME.pattern.matcher(projectName).matches,
          s"The project name, '$projectName` is not a valid Java qualified name")
        Some("dynamicScripts/" + projectName)
      },

      // name the '*-resource.zip' in the same way as other artifacts
      com.typesafe.sbt.packager.Keys.packageName in Universal :=
      normalizedName.value + "_" + scalaBinaryVersion.value + "-" + version.value + "-resource",

      // contents of the '*-resource.zip' to be produced by 'universal:packageBin'
      mappings in Universal <++= (baseDirectory,
                                   packageBin in Compile, packageSrc in Compile, packageDoc in Compile,
                                   packageBin in Test, packageSrc in Test, packageDoc in Test) map {
                                   (dir, bin, src, doc, binT, srcT, docT) =>
                                     (dir ** "*.dynamicScripts").pair(relativeTo(dir)) ++
                                     ((dir ** "*.md") --- (dir / "sbt.staging" ***)).pair(relativeTo(dir)) ++
                                     (dir / "models" ** "*.mdzip").pair(relativeTo(dir)) ++
                                     com.typesafe.sbt.packager.MappingsHelper.directory(dir / "resources") ++
                                     addIfExists(bin, "lib/" + bin.name) ++
                                     addIfExists(binT, "lib/" + binT.name) ++
                                     addIfExists(src, "lib.sources/" + src.name) ++
                                     addIfExists(srcT, "lib.sources/" + srcT.name) ++
                                     addIfExists(doc, "lib.javadoc/" + doc.name) ++
                                     addIfExists(docT, "lib.javadoc/" + docT.name)
                                 },

      // add the '*-resource.zip' to the list of artifacts to publish; note that '.zip' will change to '.jar'
      artifacts <+= (name in Universal) { n => Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map()) },
      packagedArtifacts <+= (packageBin in Universal, name in Universal) map { (p, n) =>
        Artifact(n, "jar", "jar", Some("resource"), Seq(), None, Map()) -> p
      }
    )
  }

}
