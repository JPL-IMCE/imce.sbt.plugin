# SECAE SBT Plugin for MBEE projects in Scala, Java, AspectJ.

The [Simple Build Tool](http://www.scala-sbt.org "SBT") is an extensible system
for building packaged artifacts (typically jars, zips, ...) from un-packaged 
artifacts (typically sources, including configuration-managed and generated)
with support for resolving, fetching and publishing artifacts (packaged or unpackaged) 
from/to artifact repositories (local or remote).

## Usage

The `java.net.URI` API does not currently support the GIT URI protocol.
To work around this, add a URI remapping in the user's global GIT configuration.

At the terminal:

```git config --global url."git@".insteadOf github://git@```

With the above redirection, add the following to an SBT `project/plugins.sbt` file:

```
addSbtPlugin("gov.nasa.jpl.mbee.sbt", "sbt-mbee-plugin", "1800.02")
```



## Related work

The JPL MBEE SBT Plugin is an SBT Archetype AutoPlugin in the sense of Mark Schaake's concept of
[SBT Archetype AutoPlugin](http://www.slideshare.net/MarkSchaake/archetype-autoplugins).

The motivations for developing the JPL MBEE SBT Plugin are the same as Mark Schaake's.
The techniques involved are similar.

## Ant, Maven, Gradle, SBT: A comparison

See [SBT in Action](http://www.manning.com/suereth2/SBTinA_meap_ch1.pdf "Chapter 1")

## Why SBT for SECAE MBEE projects?

SECAE has been distributing to the JPL MBSE community the SECAE-packaged MagicDraw toolkit,
which integrates several MagicDraw extensions mostly developed 
at JPL (plugins, profiles, transformations, scripts, examples, ...).
Recent advances push for integrating standards-based capabilities (e.g., OMG's UML/SysML, W3C's OWL).
This requires revisiting the MagicDraw-centric SECAE integration process model.

The MagicDraw-centric SECAE integration process model is orchestrated via Ant scripts.
Each integration process step begins with a MagicDraw package (initially, NoMagic's), 
builds an extension (typically a plugin) resulting in a MagicDraw zip resource 
that is installed into the input MagicDraw package resulting in a new MagicDraw package 
with the extension already integrated. This process is not appropriate for building standards-based
capabilities.

Two noteworthy standards-based capabilities to be integrated include JPL's IMCE Ontological Modeling Framework
(OMF/Core) and JPL's OMG Tool Neutral Interface (OTI/Core) API for OMG-compliant UML 2.5 modeling tools.
Both capabilities separate a standards-based API (OMF/Core, OTI/Core) 
from technology-specific bindings (e.g., OMF/OWL, OTI/MagicDraw). Which build process technology can
accommodate building these capabilities without introducing artificial dependencies on SECAE's MagicDraw package
while supporting integrating their MagicDraw-specific technology bindings into SECAE's MagicDraw package?

### Requirements for an integration/build process technology

For the scope of SECAE's MBSE tool integration charter, a suitable build technology must accommodate support for
distinguishing two kinds of artifacts:

1. Unpackaged artifacts

    #### What?

    Typically, an unpackaged artifact is a "source" project under Source Configuration Management (SCM), 
    preferably GIT or SVN for legacy projects. Some unpackaged artifacts are the result of a "generation" process 
    (e.g., code generation, model transformation, ...)

    #### Role?

    An unpackaged artifact is typically the input of a build process step; 
    in some cases, an unpackaged artifact may be used as a dependency for a build process step.

2. Packaged artifact

    #### What?

    Typically, this is an assembly of some kind: library (e.g. .jar, .lib, .so, .dll, ...), executable, 
    archive (e.g., javadoc, sources), digest (e.g., Yaml, json, ...), models (e.g., profiles), ...

    #### Role?

    A packaged artifact is typically the output of a build process step or a dependency for a build process step.

For the scope of SECAE's MBSE tool integration charter, a suitable build technology must accommodate support for
managing artifacts according to current best practices in agile software engineering; that is:
 
* artifacts (both unpackaged and packaged) are units stored in artifact repositories 
 
* build systems fetch input build dependencies from artifact repositories
 
* build systems publish output packaged artifacts to artifact repositories

These requirements imply selecting a standard for specifying artifacts.
Maven is currently the de-facto standard in the software engineering world for this purpose.
In Maven terminology, an artifact is identified via [Maven Coordinates](http://maven.apache.org/pom.html#Maven_Coordinates).
Artifact repositories can be queried for artifacts based on Maven coordinates 
whereas artifacts are published to artifact repositories according to their Maven coordinates.

Two coordinates two play a special role for identifying a unique collection of artifacts published by an organization:

* `groupId` should be a globally unique identifier for the publishing organization.

* `artifactId` should be a unique artifact identifier within the publishing organization.
 
The combination of `groupId` and `artifactId` uniquely identifies a collection of artifacts that Maven calls a `project`.
The remaining Maven coordinates help identify a unique variant of a `project`; some of the commonly used coordinates include:

* `version` should be unique within a `project` identified by the `groupId` and `artifactId` coordinates.

* `type` specifies the structure of the artifact when it is a build dependency (e.g., jar, zip, ...)

* `scope` specified the restricted purpose of the artifact for a particular kind of build task (e.g., compile, test, ...)

### Evaluation Criteria

There are several Maven-compatible build systems that support the two requirements above.
Some of  most well-known systems include those listed on 
the [Maven Dependency Information](http://maven.apache.org/ref/3.3.3/maven-repository-metadata/dependency-info.html),
page with the addition of two popular systems: `Apache Ant` (widely used in SECAE) and `Gradle`. 
These systems are evaluated relative to criteria that,
based on experience, are important for transitioning SECAE's integration build process 
to a more agile and effective process:

1. **DSL integration in a programming language**

    Building MagicDraw plugins is a good example of a build process that involves steps 
    that are specific to the MagicDraw plugin architecture. It is very useful to have support
    in the build system for an extension mechanism so that specialized process steps can be reused 
    across multiple build projects. If the build system is a separate language than the programming language
    used for developing extensions of that build system, then developing extensions of the build system
    becomes more complicated than for build systems designed as DSL extensions of a programming language
    used for working with and extending the build DSL.
 
2. **Functional Build Language**

    `Apache Ant` has been the build system workhorse for integrating SECAE's MagicDraw package for several years.
    In that time frame, reusing integration scripts has been surprisingly difficult because of the procedural nature 
    of the 'Apache Ant' language. SECAE's experiences with `Apache Ant` are not unique; in fact, similar experiences
    drove the development of all of the alternatives to `Apache Ant` evaluated below. Organizing a build system
    according to pure functional programming principles is a non-trivial challenge; however, it is one that is 
    made considerably easier if the build language is defined as a DSL extension of a programming language with good
    support for functional programming.

3. **Statically-typed Language**

    Types are fundamental to all programming languages in the sense that all programming languages operate in terms
    of executing functions on values. The distinction between statically-typed and dynamically-typed
    languages is about checking that the type of values passed to a function as actual arguments is consistent 
    with the type of the formal arguments of that function. In a dynamically-typed language, this checking happens 
    at runtime whereas in a statically-typed language, this checking happens at compile time. This distinction has
    important implications for build systems: given that a build process typically executes in the order of minutes, 
    it is objectively useful to have better support for catching errors at compile time with a statically-typed language
    than at runtime with a dynamically-typed language. 
    
### Scoring Criteria

The three criteria above are organized in to the scoring table below for comparing the build systems available.

<p>
<table>
<tr>
<td align="center">Criteria</td>
<td align="center">Possible Values</td>
<td align="center">Value</td>
</tr>
<tr>
<td align="center" rowspan="3">Build Language Design</td>
<td align="center"><span align="center"><p>DSL for a Programming Language</p><p>(can be the same language used for other development)</p></span></td>
<td align="center">2</td>
</tr>
<tr>
<td align="center"><span align="center"><p>DSL for a Programming Language</p><p>(different language than that used for other development)</p></span></td>
<td align="center">1</td>
</tr>
<tr>
<td align="center">Build and/or Dependency Language</td>
<td align="center">0</td>
</tr>
<tr>
<td align="center" rowspan="2">Artifact Support?</td>
<td align="center">Yes</td>
<td align="center">1</td>
</tr>
<tr>
<td align="center">No</td>
<td align="center">0</td>
</tr>
<tr>
<td align="center" rowspan="2">Functional Language?</td>
<td align="center">Yes</td>
<td align="center">1</td>
</tr>
<tr>
<td align="center">No</td>
<td align="center">0</td>
</tr>
<tr>
<td align="center" rowspan="2">Statically-typed Language?</td>
<td align="center">Yes</td>
<td align="center">1</td>
</tr>
<tr>
<td align="center">No</td>
<td align="center">0</td>
</tr>
</table>
</p>

### Evaluation Results

The following table summarizes the evaluation of several build system technologies with respect 
to the support for artifact-based management requirements and the scoring table above.

<p>
<table>
<tr>
<td align="center">System</td>
<td align="center">Example</td>
<td align="center">Build Language Design: <span><p align="left">- Build and/or Dependency Language</p><p align="left">- DSL for a Programming Language</p></span></td>
<td align="center"><span align="middle"><p>Native Maven</p>
<p>Artifact Support?</p><span></td>
<td align="center"><span align="middle"><p>Functional</p>
<p>Language?</p><span></td>
<td align="center"><span align="middle"><p>Static</p>
<p>Typing?</p><span></td>
<td align="center">Score</td>
</tr>
<tr>
<td align="center"><a href="http://maven.apache.org/">Apache Maven</a></td>
<td><div class="source">
    <pre>
&lt;dependency&gt;
  &lt;groupId&gt;org.apache.maven&lt;/groupId&gt;
  &lt;artifactId&gt;maven-repository-metadata&lt;/artifactId&gt;
  &lt;version&gt;3.3.3&lt;/version&gt;
&lt;/dependency&gt;</pre></div></td>
<td align="center">Build & Dependency</td>
<td align="center">Yes</td>
<td align="center">Yes</td>
<td align="center">No</td>
<td align="center">2</td>
</tr>
<tr>
<td align="center"><a href="http://maven.apache.org/">Apache Ivy</a></td>
<td><div class="source">
    <pre>
&lt;dependency org=&quot;org.apache.maven&quot; name=&quot;maven-repository-metadata&quot; rev=&quot;3.3.3&quot;&gt;
  &lt;artifact name=&quot;maven-repository-metadata&quot; type=&quot;jar&quot; /&gt;
&lt;/dependency&gt;</pre></div></td>
<td align="center">Dependency-only</td>
<td align="center">Yes</td>
<td align="center">N/A</td>
<td align="center">N/A</td>
<td align="center">1</td>
</tr>
<tr>
<td align="center"><a href="http://ant.apache.org">Apache Ant</a></td>
<td><div class="source">
    <pre>
&lt;target name=&quot;compile&quot; depends=&quot;init&quot;
    description=&quot;compile the source &quot; &gt;
  &lt;!-- Compile the java code from ${src} into ${build} --&gt;
  &lt;javac srcdir=&quot;${src}&quot; destdir=&quot;${build}&quot;/&gt;
&lt;/target&gt;

&lt;target name=&quot;dist&quot; depends=&quot;compile&quot;
    description=&quot;generate the distribution&quot; &gt;
  &lt;!-- Create the distribution directory --&gt;
  &lt;mkdir dir=&quot;${dist}/lib&quot;/&gt;

  &lt;!-- Put everything in ${build} into the MyProject-${DSTAMP}.jar file --&gt;
  &lt;jar jarfile=&quot;${dist}/lib/MyProject-${DSTAMP}.jar&quot; basedir=&quot;${build}&quot;/&gt;
&lt;/target&gt;</pre></div></td>
<td align="center">Build-only</td>
<td align="center">No</td>
<td align="center">No</td>
<td align="center">No</td>
<td align="center">0</td>
</tr>
<tr>
<td align="center"><a href="http://maven.apache.org/">Apache Buildr</a></td>
<td><div class="source">
    <pre>'org.apache.maven:maven-repository-metadata:jar:3.3.3'</pre></div></td>
<td align="center">Ruby-based DSL</td>
<td align="center">Yes</td>
<td align="center">Yes</td>
<td align="center">No</td>
<td align="center">3</td>
</tr>
<tr>
<td align="center"><a href="http://docs.groovy-lang.org/latest/html/documentation/grape.html">Groovy Grape</a></td>
<td><div class="source">
    <pre>
@Grapes(
  @Grab(group='org.apache.maven', module='maven-repository-metadata', version='3.3.3')
)</pre></div></td>
<td rowspan="3" align="center">Groovy-based DSL</td>
<td align="center">Yes</td>
<td rowspan="3" align="center">Yes</td>
<td rowspan="3" align="center">No</td>
<td rowspan="3" align="center">3</td>
</tr>
<tr>
<td align="center"><a href="https://grails.org">Grails</a></td>
<td><div class="source">
    <pre>compile 'org.apache.maven:maven-repository-metadata:3.3.3'</pre></div></td>
<td align="center">Yes</td>
</tr>
<tr>
<td align="center"><a href="https://docs.gradle.org/">Gradle</a></td>
<td><div class="source">
    <pre>
dependencies {
  compile group: 'org.hibernate', name: 'hibernate-core', version: '3.6.7.Final'
  testCompile group: 'junit', name: 'junit', version: '4.+'
}</pre></div></td>
<td align="center">Yes</td>
</tr>
<tr>
<td align="center"><a href="http://leiningen.org">Leiningen</a></td>
<td><div class="source">
    <pre>[org.apache.maven/maven-repository-metadata &quot;3.3.3&quot;]</pre></div></td>
<td align="center">Clojure-based DSL</td>
<td align="center">Yes</td>
<td align="center">Yes</td>
<td align="center">No</td>
<td align="center">3</td>
</tr>
<tr>
<td align="center"><a href="http://www.scala-sbt.org">SBT</a></td>
<td><div class="source">
    <pre>libraryDependencies += &quot;org.apache.maven&quot; % &quot;maven-repository-metadata&quot; % &quot;3.3.3&quot;</pre></div></td>
<td align="center"><b>Scala-based DSL</b></td>
<td align="center"><b>Yes</b></td>
<td align="center"><b>Yes</b></td>
<td align="center"><b>Yes</b></td>
<td align="center"><b>5</b></td>
</tr>
</table>
</p>

Based on the objective evaluation criteria and ranking, SBT is a better choice than 
all other Maven-based build language systems available today.

Note that, by definition, this objective evaluation excludes subjective factors such as training.
