sbtPlugin := true

name := "imce.sbt.plugin"

moduleName := "imce.sbt.plugin"

organization := "gov.nasa.jpl.imce"

homepage := Some(url("https://github.com/JPL-IMCE/imce.sbt.plugin"))

organizationName := "JPL-IMCE"

organizationHomepage := Some(url("http://www.jpl.nasa.gov"))

git.remoteRepo := "git@github.com:JPL-IMCE/imce.sbt.plugin.git"

startYear := Some(2015)

scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/JPL-IMCE/imce.sbt.plugin"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")))
