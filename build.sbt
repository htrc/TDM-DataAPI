import sbtbuildinfo.BuildInfoPlugin

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "tdm",
  organizationName := "Text and Data Mining (TDM) initiative involving HathiTrust/HTRC, JSTOR, and Portico",
  scalaVersion := "2.12.6",
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-target:jvm-1.8"
  ),
  resolvers ++= Seq(
    "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
    Resolver.mavenLocal
  ),
  buildInfoOptions ++= Seq(BuildInfoOption.BuildTime),
  buildInfoPackage := "utils",
  buildInfoKeys ++= Seq[BuildInfoKey](
    "gitSha" -> git.gitHeadCommit.value.getOrElse("N/A"),
    "gitBranch" -> git.gitCurrentBranch.value,
    "gitVersion" -> git.gitDescribedVersion.value.getOrElse("N/A"),
    "gitDirty" -> git.gitUncommittedChanges.value
  ),
  packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  )
)

lazy val dockerSettings = Seq(
    maintainer in Docker := "Boris Capitanu <capitanu@illinois.edu>",
    dockerBaseImage := "anapsix/alpine-java:8",
    dockerExposedPorts := Seq(9000),
    dockerUpdateLatest := true
)

lazy val `tdm-data-api` = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin, GitVersioning, GitBranchPrompt, JavaAppPackaging, DockerPlugin)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(
    name := "TDM-DataAPI",
    libraryDependencies ++= Seq(
      guice,
      filters,
      "com.typesafe.play"             %% "play-iteratees"                   % "2.6.1",
      "com.typesafe.play"             %% "play-iteratees-reactive-streams"  % "2.6.1",
      "org.reactivemongo"             %% "reactivemongo-iteratees"          % "0.16.0",
      "org.reactivemongo"             %% "play2-reactivemongo"              % "0.16.0-play26",
      "org.scalatestplus.play"        %% "scalatestplus-play"               % "3.1.2"   % Test
    )
  )


