kantanProject in ThisBuild := "dtables"
startYear in ThisBuild     := Some(2019)

// - root projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val root = Project(id = "kantan-dtables", base = file("."))
  .settings(moduleName := "root")
  .enablePlugins(UnpublishedPlugin)
  .aggregate(core, csv, properties, samples)

// - core projects -----------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
lazy val core = project
  .settings(
    moduleName := "kantan.dtables",
    name       := "core"
  )
  .enablePlugins(PublishedPlugin)

lazy val csv = project
  .settings(
    moduleName := "kantan.dtables-csv",
    name       := "csv"
  )
  .enablePlugins(PublishedPlugin, spray.boilerplate.BoilerplatePlugin)
  .settings(libraryDependencies += "com.nrinaudo" %% "kantan.csv" % Versions.kantanCsv)
  .dependsOn(core)

lazy val properties = project
  .settings(
    moduleName := "kantan.dtables-properties",
    name       := "properties"
  )
  .enablePlugins(PublishedPlugin, spray.boilerplate.BoilerplatePlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.nrinaudo"   %% "kantan.codecs" % Versions.kantanCodecs,
      "org.scalacheck" %% "scalacheck"    % Versions.scalacheck
    )
  )
  .dependsOn(core)

lazy val scalatest = project
  .settings(
    moduleName := "kantan.dtables-scalatest",
    name       := "scalatest"
  )
  .enablePlugins(PublishedPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % Versions.scalatest
    )
  )
  .dependsOn(properties)

lazy val samples = project
  .settings(
    moduleName := "kantan.dtables-samples",
    name       := "samples"
  )
  .enablePlugins(UnpublishedPlugin, spray.boilerplate.BoilerplatePlugin)
  .dependsOn(core, scalatest, csv)
