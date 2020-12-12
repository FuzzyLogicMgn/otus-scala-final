scalaVersion := "2.13.3"
libraryDependencies ++= ProjectConfig.projectDependencies
scalacOptions += "-Ymacro-annotations"
enablePlugins(AkkaGrpcPlugin)