# SBT Plugin for Java, Scala, AspectJ projects

The [Simple Build Tool](http://www.scala-sbt.org "SBT") is an extensible system
for building packaged artifacts (typically jars, zips, ...) from un-packaged 
artifacts (typically sources, including configuration-managed and generated)
with support for resolving, fetching and publishing artifacts (packaged or unpackaged) 
from/to artifact repositories (local or remote).

[![Build Status](https://travis-ci.org/JPL-IMCE/imce.sbt.plugin.svg?branch=IMCEI-283)](https://travis-ci.org/JPL-IMCE/imce.sbt.plugin)

# Configuration

Create a file: `local.credentials.sbt`:

```
pgpSigningKey := Some(0x<longID>L)

pgpPassphrase := Some("<passphrase>".toArray[Char])
```

replacing `<longID>` with the 64-bit hexadecimal key on the 'pub' line of `gpg --list-keys --with-colon`
and replacing `<passphrase>` with the GPG key passphrase.

This file should be encrypted on github.com and decrypted when running locally or on public CI.

## Building on public CI infrastructure (e.g. travis-ci)

sbt-pgp supports signing artifacts via either the GPG command-line utility or the BountyCastle library (BCP).
However, when running on public CI, it is necessary to specify the GPG key passphrase somehow.
- With the GPG command-line configuration:
  
  Since the sbt-pgp plugin doesn't use gpg's "--batch --passphrase-fd 0" options, 
  it would be necessary to setup a `gpg-agent` configured with the passphrase.
  
- With the BCP library configuration:

  Make sure that `pgpSecretRing` and `pgpPublicRing` point to iles with a single key.
  If there are multiple keys, BCP only loads the first one (see this [issue](https://github.com/sbt/sbt-pgp/issues/47))

# Old Notes.

## Usage

The `java.net.URI` API does not currently support the GIT URI protocol.
To work around this, add a URI remapping in the user's global GIT configuration.

At the terminal:

```git config --global url."git@".insteadOf github://git@```

With the above redirection, add the following to an SBT `project/plugins.sbt` file:

```
addSbtPlugin("gov.nasa.jpl.imce.sbt", "imce-sbt-plugin", "<version>")
```

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

