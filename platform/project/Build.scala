import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
// import play.twirl.sbt.Import._

object ApplicationBuild extends Build {

  val appName         = "hdc"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here
    javaCore,
    javaJdbc,
    javaEbean,
    "org.mongodb" % "mongo-java-driver" % "2.12.2",
    "org.elasticsearch" % "elasticsearch" % "1.2.1"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayJava).settings(
  	// Add your own project settings here
  	version := appVersion,
  	libraryDependencies ++= appDependencies
  )

}
