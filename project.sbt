sbtPlugin := true

name := "imce.sbt.plugin"

description := "A convenience aggregate of several SBT plugins"

moduleName := name.value

organization := "gov.nasa.jpl.imce"

organizationName := "JPL-IMCE"

homepage := Some(url(s"https://jpl-imce.github.io/${moduleName.value}"))

organizationHomepage := Some(url(s"https://github.com/${organizationName.value}"))

git.remoteRepo := s"git@github.com:${organizationName.value}/${moduleName.value}.git"

scmInfo := Some(ScmInfo(
  browseUrl = url(s"https://github.com/${organizationName.value}/${moduleName.value}"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")))
