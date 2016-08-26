# JPL's IMCE SBT Plugin for Java, Scala, AspectJ projects

This [sbt plugin]http://www.scala-sbt.org/0.13/docs/Using-Plugins.html) aggregates a few
 [community plugins](http://www.scala-sbt.org/0.13/docs/Community-Plugins.html) used across several projects in
 [JPL's Integrated Model-Centric Engineering initiative](https://github.com/JPL-IMCE) and in 
 [OMG's Tool Infrastructure Working Group](https://github.com/TIWG).

There is an inherent tradeoff between maintaining slight variations 
of similar build configurations across multiple projects (i.e. no common sbt plugin) and 
defining a common sbt plugin reused across multiple projects to simplify their build configurations.
  
Conceptually, the scope and purpose of this sbt plugin is very similar to the elegant
  [nice-sbt-settings](https://github.com/ohnosequences/nice-sbt-settings). 
  Practically, [nice-sbt-settings](https://github.com/ohnosequences/nice-sbt-settings) is indeed nicer.
  
[![Build Status](https://travis-ci.org/JPL-IMCE/imce.sbt.plugin.svg?branch=IMCEI-283)](https://travis-ci.org/JPL-IMCE/imce.sbt.plugin)
[ ![Download](https://api.bintray.com/packages/jpl-imce/gov.nasa.jpl.imce/imce.sbt.plugin/images/download.svg) ](https://bintray.com/jpl-imce/gov.nasa.jpl.imce/imce.sbt.plugin/_latestVersion)

# Usage

## sbt configuration

in `project/plugins.sbt`, add:

```
resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce")

addSbtPlugin("gov.nasa.jpl.imce.sbt", "imce-sbt-plugin", "<version>")
```

## Two-stage publication to [Bintray](https://bintray.com)

### Bintray package version upload (aka `sbt publish` or `sbt publishSigned`)

1) in `build.sbt`, add:

- configuration for publishing to bintray:

```
// publish SBT and non-SBT artifacts as Maven artifacts (i.e. with *.pom)
publishMavenStyle := true

// publish to bintray.com via: `sbt publish`
publishTo := Some(
  "JPL-IMCE" at
    s"https://api.bintray.com/content/jpl-imce/gov.nasa.jpl.imce/imce.sbt.plugin/${version.value}")
```

This requires specifying the Bintray credentials:

```
credentials += Credentials("Bintray API Realm", "api.bintray.com", "<user>", "<bintray API key>")
```

- configuration for resolving from bintray:

```
resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce")
```

For resolving unpublished artifacts, resolution requires credentials:

```
credentials += Credentials("Bintray", "dl.bintray.com", "<user>", "<bintray API key")
```

2) Phase 1: Upload to bintray

In Bintray, when a user uploads artifact files to a package version,
the uploaded artifacts are available (i.e. resolvable in Maven/SBT terminology)
only for that authenticated user.
 
- Note: this will upload the sbt artifact files to bintray.
 
```
sbt publish
```

or:

```
sbt publishSigned
```

3) Phase 2: Discard or Publish the uploaded artifact files for a package version

In Bintray, the uploaded artifact files for a project version can be:
 - discarded => delete the uploaded artifact files: nobody can resolve them. 
 - published => make the uploaded artifact files publicly available: everyone can resolve them.
  
Note that bintray keeps unpublished uploaded artifact files for 
a short period (it seems to be ~ 6 days), after which they are discarded.

During this period, unpublished artifact files for a package version
can be tested (only with properly authenticated access).

- Via the JFrog CLI:

  - Install the [JFrog CLI](https://www.jfrog.com/getcli/)

  - publish: `jfrog bt vp <subject>/<organization>/<package>/<version>`
  - disacard: `jfrog bt vd <subject>/<organization>/<package>/<version>`

- Via the bintray.com web UI:

  - Go to: `https://bintray.com/<subject>/<organization>/<package>/<version>`
  
  - There are separate buttons for publishing & discarding.
  

# Older notes for JPL's internal workflow

## Using the imce.sbt.plugin

Note: This was for the internal version at JPL.

There are 2 required properties:

1. Resolve repository (used for dependency resolution)

   Set either:
   - `-DPL_LOCAL_RESOLVE_REPOSITORY=<dir>`
   - `-DJPL_REMOTE_RESOLVE_REPOSITORY=<url>`

2. Either normal or managed-staging

  - normal

    Set either:
    - `-DJPL_LOCAL_PUBLISH_REPOSITORY=<dir>`
    - `-DJPL_REMOTE_PUBLISH_REPOSITORY=<url>`

    Set:
    - `-DJPL_NEXUS_REPOSITORY_HOST=<address>`

    This usage is for creating a staging repository, e.g:
    [imce-ci ciStagingRepositoryCreate](https://github.jpl.nasa.gov/imce/imce-ci#sbt-cistagingrepositorycreate-descriptionstring-filepath).

    In this usage, publishing will use server-managed staging repository creation.

  - managed-staging

    Set:
    - `-DJPL_STAGING_CONF_FILE=<*.conf>`

    In this usage, publishing will be directed to the staging repository identified by the config file.

## User configuration

  - Edit `~/.gnupg/gpg.conf`:

    ```
    use-agent
    no-tty
    ```

  - Edit `~/.gitconfig` to add:

    ```
    [commit]
	  gpgsign = true
    ```

    or:

    ```
    git config --global commit.gpgsign true
    ```

  - create a file: `project/maven.sbt` with the following:

     ```addMavenResolverPlugin```

    See: [Maven Resolver Plugin](http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Maven+resolver+plugin)

## [Support for IMCE Build & Release Workflows](doc/Workflows.md)

## Support for the IMCE CI & Release process

1. Create a staging repository with [imce-ci ciStagingRepositoryCreate](https://github.jpl.nasa.gov/imce/imce-ci#sbt-cistagingrepositorycreate-descriptionstring-filepath)

  ```
  sbtJPLBeta ciStagingRepositoryCreate profile=<name> description=<string_without_space> file=<path>
  ```

2. Build/Release projects

  ```
  sbt \
    ... \
    -DJPL_REMOTE_PUBLISH_REPOSITORY=https://cae-nexuspro.jpl.nasa.gov/nexus/service/local/staging/deploy/maven2 \
    -DJPL_STAGING_CONF_FILE=<*.conf>
  > release with-defaults
  > git push origin --tags
  ```

## Additional Information

### [Rationale for using SBT](doc/Evaluation.md)

### [How to find available updates for versioned dependencies?](doc/DependencyUpdates.md)

## Useful links

- [Requirements for releasing to maven central](http://central.sonatype.org/pages/requirements.html)

  Brief summary of key requirements agreed to by the open source software community.

- [Releasing to the Open-Source Software Repository Hosting, OSSRH](http://central.sonatype.org/pages/releasing-the-deployment.html)

  Brief overview of the repository staging process involved in publishing releases.

- [Publishing scala libraries to Sonatype](http://www.loftinspace.com.au/blog/publishing-scala-libraries-to-sonatype.html)

  Explains the signing requirements, including creating & publishing a signed key to meet the requirements for publishing to the OSSRH.

- [Painless release with SBT](http://blog.byjean.eu/2015/07/10/painless-release-with-sbt.html)

  Explanation about several SBT plugins used to automate the release process.

