// http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#sbt+0.13.8
// addMavenResolverPlugin
//
// Disabling this is necessary to avoid checksum validation problems when executing 'signedArtifacts'
// @see: https://travis-ci.org/JPL-IMCE/imce.sbt.plugin/builds/192477639
// [info] Packaging /home/travis/build/JPL-IMCE/imce.sbt.plugin/target/scala-2.10/sbt-0.13/imce.sbt.plugin-4.18.171-5aa708eabb0750cdeb2f0649aed2f0ec07f818e0-sources.jar ...
// [info] Done packaging.
// [info] Wrote /home/travis/build/JPL-IMCE/imce.sbt.plugin/target/scala-2.10/sbt-0.13/imce.sbt.plugin-4.18.171-5aa708eabb0750cdeb2f0649aed2f0ec07f818e0.pom
// 20:30:25.559 [pool-5-thread-2] WARN  o.e.a.i.i.WarnChecksumPolicy - Could not validate integrity of download from https://repo1.maven.org/maven2/org/scoverage/sbt-scoverage_2.10_0.13/1.3.5/sbt-scoverage-1.3.5.pom: Checksum validation failed, expected 615d91f8723dc66624cebd0bf064502a460446f4 but is da39a3ee5e6b4b0d3255bfef95601890afd80709
// 20:30:25.738 [pool-5-thread-2] WARN  o.e.a.i.i.WarnChecksumPolicy - Could not validate integrity of download from https://repo1.maven.org/maven2/org/scoverage/sbt-
//

