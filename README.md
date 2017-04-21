# JPL's IMCE SBT Plugin for Java, Scala, AspectJ projects

This [sbt plugin](http://www.scala-sbt.org/0.13/docs/Using-Plugins.html) aggregates a few
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

```scala
resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce")

addSbtPlugin("gov.nasa.jpl.imce.sbt", "imce.sbt.plugin", "<version>")
```

## Two-stage publication to [Bintray](https://bintray.com)

### Bintray package version upload (aka `sbt publish` or `sbt publishSigned`)

1) in `build.sbt`, add:

- configuration for publishing to bintray:

```scala
// publish SBT and non-SBT artifacts as Maven artifacts (i.e. with *.pom)
publishMavenStyle := true

// publish to bintray.com via: `sbt publish`
publishTo := Some(
  "JPL-IMCE" at
    s"https://api.bintray.com/content/jpl-imce/gov.nasa.jpl.imce/imce.sbt.plugin/${version.value}")
```

This requires specifying the Bintray credentials:

```scala
credentials += Credentials("Bintray API Realm", "api.bintray.com", "<user>", "<bintray API key>")
```

- configuration for resolving from bintray:

```scala
resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce")
```

For resolving unpublished artifacts, resolution requires credentials:

```scala
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

## Additional Information

### [Rationale for using SBT](doc/Evaluation.md)

### [How to find available updates for versioned dependencies?](doc/DependencyUpdates.md)

## Useful links

- [Requirements for releasing to maven central](http://central.sonatype.org/pages/requirements.html)
