name := "dita-ot"

version := "1.7"

scalaVersion := "2.10.3"

libraryDependencies += "xerces" % "xercesImpl" % "2.10.0"

libraryDependencies += "xml-apis" % "xml-apis" % "2.0.2"

libraryDependencies += "xml-resolver" % "xml-resolver" % "1.2"

libraryDependencies += "org.apache.ant" % "ant" % "1.8.4"

//libraryDependencies += "net.sourceforge.saxon" % "saxon" % "9.1.0.8"

//libraryDependencies += "net.sourceforge.saxon" % "saxon" % "9.1.0.8" classifier "dom"

libraryDependencies += "net.sf.saxon" % "Saxon-HE" % "9.4.0.6"

libraryDependencies += "commons-codec" % "commons-codec" % "1.7"
