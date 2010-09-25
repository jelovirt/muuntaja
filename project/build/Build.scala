import sbt._

class MuuntajaProject(info: ProjectInfo) extends DefaultProject(info) {
  
  val snapshots = ScalaToolsSnapshots
    
  val XercesImpl = "xerces" % "xercesImpl" % "2.9.1" // 2.10.0 
  val XmlResolver = "xml-resolver" % "xml-resolver" % "1.2" 
  val Xom = "xom" % "xom" % "1.2.5" // 1.2.6 
  val Scalatest = "org.scalatest" % "scalatest" % "1.2" 
  val CommonsIo = "commons-io" % "commons-io" % "1.4" 
  val ScalaLibrary = "org.scala-lang" % "scala-library" % "2.8.0" 
  val Junit = "junit" % "junit" % "4.8.1" // 2.8.2
  val WoodstoxCoreAsl = "org.codehaus.woodstox" % "woodstox-core-asl" % "4.0.7" 
  val Ant = "org.apache.ant" % "ant" % "1.8.1" 
  
  override def compileOrder = CompileOrder.ScalaThenJava

}