name := "akka-streams"

version := "1.0"

scalaVersion := "2.12.3"

resolvers += Resolver.typesafeRepo("releases")

libraryDependencies ++= Seq(
  "org.specs2"          %%  "specs2-core"         % "3.8.6"   % "test",
  "com.typesafe.akka"   %% "akka-actor"           % "2.5.3",
  "com.typesafe.akka"   %% "akka-slf4j"           % "2.5.3",
  "com.typesafe.akka"   %% "akka-stream"          % "2.5.3",
  "ch.qos.logback"      % "logback-classic"       % "1.1.7",
  "org.scalactic"       %% "scalactic"            % "3.0.1",
  "org.twitter4j"       % "twitter4j-stream"      % "4.0.6",
  "com.typesafe.akka"   %% "akka-testkit"         % "2.5.3"   % "test",
  "com.typesafe.akka"   %% "akka-stream-testkit"  % "2.5.3"   % "test",
  "org.scalatest"       %% "scalatest"            % "3.0.1"   % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos")
