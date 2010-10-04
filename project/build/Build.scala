import sbt._

class MuuntajaProject(info: ProjectInfo) extends DefaultProject(info) {
  
  val snapshots = ScalaToolsSnapshots
  
  //val mavenLocal = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"
  val ScalaLibrary = "org.scala-lang" % "scala-library" % "2.8.0"
  val XercesImpl = "xerces" % "xercesImpl" % "2.9.1" // 2.10.0 
  val XmlResolver = "xml-resolver" % "xml-resolver" % "1.2" 
  val Xom = "xom" % "xom" % "1.2.5" // 1.2.6 
  val Scalatest = "org.scalatest" % "scalatest" % "1.2" % "test" 
  val CommonsIo = "commons-io" % "commons-io" % "1.4" 
  val Junit = "junit" % "junit" % "4.8.1" // 2.8.2
  //val WoodstoxCoreAsl = "org.codehaus.woodstox" % "woodstox-core-asl" % "4.0.7" 
  val Ant = "org.apache.ant" % "ant" % "1.8.1" % "provided"
  val FindBugs = "com.google.code.findbugs" % "findbugs" % "1.3.9" % "provided"
  
  override def compileOrder = CompileOrder.ScalaThenJava

// http://gracelessfailures.com/2009/11/24/build-package-zip-sbt.html
  
  /**
 * In the classpath:
 *  - all dependencies (via Ivy/Maven and in lib)
 *  - package classes
 * On the filesystem:
 *  - scripts
 *  - config
 */
def distPath = (
  // NOTE the double hashes (##) hoist the files in the preceeding directory
  // to the top level - putting them in the "base directory" in sbt's terminology
  ((outputPath ##) / defaultJarName) +++
  //mainResources +++
  mainDependencies.scalaJars +++
  descendents(info.projectPath / "test" / "dita-ot" / "com.github.muuntaja", "*.xml") +++
  //descendents(info.projectPath, "*.rb") +++
  //descendents(info.projectPath, "*.conf") +++
  //descendents(info.projectPath / "lib" ##, "*.jar") +++
  descendents(managedDependencyRootPath ** "compile" ##, "*.jar")
)

// creates a sane classpath including all JARs and populates the manifest with it
override def manifestClassPath = Some(
  distPath.getFiles
  .filter(_.getName.endsWith(".jar"))
  .map(_.getName).mkString(" ")
)

def distName = "com.github.muuntaja_%s.zip".format(version)

lazy val plugin = zipTask(distPath, "dist", distName) dependsOn (`package`) describedAs("Zips up the project.")
  
}