import com.typesafe.sbt.license.{LicenseInfo, DepModuleInfo}

// Used to name the report file, and in the HTML/Markdown as the
// title.
licenseReportTitle := "LicenseReportOfAggregatedSBTPluginsAndLibraries"

licenseSelection += LicenseCategory("EPL", Seq("Eclipse Public License"))

// Add style rules to the report.
licenseReportStyleRules := Some("table, th, td {border: 1px solid black;}")

// The ivy configurations we'd like to grab licenses for.
licenseConfigurations := Set("compile", "provided")

// Override the license information from ivy, if it's non-existent or wrong
licenseOverrides := {
  case DepModuleInfo("com.jsuereth", _, _) =>
    LicenseInfo(LicenseCategory.BSD, "BSD-3-Clause", "http://opensource.org/licenses/BSD-3-Clause")
}

licenseReportDir := target.value / "site"

licenseReportTypes := Seq(Html)