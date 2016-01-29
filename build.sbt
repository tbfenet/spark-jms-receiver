name := "spark-jms-receiver"

version := "0.1"

//scalaVersion := "2.11.6"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4","2.11.6")

val sparkVersion = "1.6.0"




libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "test",
  "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
  "javax.jms" % "jms-api" % "1.1-rev-1",
  "org.apache.activemq" % "activemq-core" % "5.7.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"

)


