name := "muuntaja"

version := "2.0"

scalaVersion := "2.10.4"

mainClass := Some("org.dita.dost.invoker.Cli")

libraryDependencies += "xerces" % "xercesImpl" % "2.11.0"

libraryDependencies += "xml-apis" % "xml-apis" % "1.4.01"

libraryDependencies += "xml-resolver" % "xml-resolver" % "1.2"

libraryDependencies += "org.apache.ant" % "ant" % "1.9.4"

//libraryDependencies += "net.sourceforge.saxon" % "saxon" % "9.1.0.8"

//libraryDependencies += "net.sourceforge.saxon" % "saxon" % "9.1.0.8" classifier "dom"

libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.5.1-5"

libraryDependencies += "commons-codec" % "commons-codec" % "1.9"

libraryDependencies += "com.ibm.icu" % "icu4j" % "54.1"

libraryDependencies += "nu.validator.htmlparser" % "htmlparser" % "1.4"

libraryDependencies += "junit" % "junit" % "4.11"

libraryDependencies += "xmlunit" % "xmlunit" % "1.5"
