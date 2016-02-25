name := "spark-jms-receiver"

version := "0.1.1"

scalaVersion := "2.11.6"

//scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4","2.11.6")

spName := "tbfenet/spark-jms-receiver"

val sparkVer = "1.6.0"

sparkVersion := sparkVer

sparkComponents ++= Seq("streaming")


credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials") 

licenses += "Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0")

spIncludeMaven := true

spAppendScalaVersion := true


libraryDependencies ++= Seq(
  "javax.jms" % "jms-api" % "1.1-rev-1",
  "org.apache.activemq" % "activemq-core" % "5.7.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"

)
