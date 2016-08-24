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

# Usage

## sbt configuration

in `project/plugins.sbt`, add:

```
resolvers += Resolver.url(
  "jpl-imce",
  url("http://dl.bintray.com/jpl-imce/gov.nasa.jpl.imce"))(Resolver.ivyStylePatterns)

addSbtPlugin("gov.nasa.jpl.imce.sbt", "imce-sbt-plugin", "<version>")
```

## Two-stage publication to [Bintray](https://bintray.com) with [Travis-CI](https://travis-ci.org/)

1. Install the [JFrog CLI](https://www.jfrog.com/getcli/) locally and in your `.travys.yml`

   ```
   before_install:   
   ...
   - if [ ! -d ~/bin ]; then mkdir ~/bin; fi
   - if [ ! -x ~/bin/jfrog ]; then (cd ~/bin; curl -fL https://getcli.jfrog.io | sh); fi
   ```

2. Install the [Travis CLI](https://github.com/travis-ci/travis.rb#installation)

3. Setup your Travis-CI for encrypting files and for GPG
 
   This is needed for signing artifacts with [sbt-pgp](https://github.com/sbt/sbt-pgp)
   
   Follow this [guide](https://www.theguardian.com/info/developer-blog/2014/sep/16/shipping-from-github-to-maven-central-and-s3-using-travis-ci).
   
4. Create a configuration for your bintray account:
  
    ```
    jfrog bt c --user <bintray user> --key <bintray API key> --li
    ```
    
  - Encrypt your bintray configuration:
  
    Assuming that `$ENCRYPTION_PASSWORD` is set 
     
    ```
    openssl aes-256-cbc -in ~/.jfrog/jfrog-cli.conf -out .jfrog-cli.conf.enc -pass pass:$ENCRYPTION_PASSWORD
    ```
    
    Add .jfrog-cli.conf.enc to your github repo.
    
  - Add to your `.travis.yml`:
  
   ```
   before_install:   
   ...
   - mkdir ~/.jfrog
   - openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in .jfrog-cli.conf.enc -out ~/.jfrog/jfrog-cli.conf -d
   
   script:
   - if [ "x$TRAVIS_TAG" = "x" ]; then sbt -jvm-opts travis/jvmopts.compile signedArtifacts; else sbt -jvm-opts travis/jvmopts.compile uploadToBintrayPackage; fi
   ```
   
5. In your `build.sbt`, configure the relevant settings, e.g.:

   ```
   jfrogCliPath := Path.userHome.toPath.resolve("bin/jfrog").toFile.absolutePath

   bintrayPackageVersion := Option.apply(System.getenv("TRAVIS_TAG")).getOrElse(version.value)

   bintrayPackagePath := "jpl-imce/gov.nasa.jpl.imce/imce.sbt.plugin"

   bintrayPackageFiles := PgpKeys.signedArtifacts.value.values
   ```

6. Locally, to upload signed artifacts to a new version for your bintray package:

   ```
   sbt uploadToBintray
   ```

   Notes:
   - The uploaded version will be accessible to you only until you publish it.
   - Bintray rejects uploading snapshots.
   
7. Locally, to publish a bintray package version:

   ```
   sbt publishBintrayPackage
   ```

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

