package com.github.muuntaja

//import scala.xml.parsing.NoBindingFactoryAdapter
//import scala.xml.{Elem, Node, TopScope}

import java.io.{File, IOException, BufferedOutputStream, FileOutputStream}
import java.net.URI
import javax.xml.parsers.{SAXParserFactory, SAXParser}

import org.xml.sax.{InputSource, XMLReader, Attributes}
import org.xml.sax.helpers.{XMLFilterImpl, AttributesImpl}

import org.apache.xml.resolver.CatalogManager
import org.apache.xml.resolver.tools.{ResolvingXMLReader, CatalogResolver}

import nu.xom.{Document, ParentNode, Element, Attribute, Elements, Nodes, Node, Builder, Serializer, DocType}


/**
 * Utilities for XOM.
 */
object XOM {

  implicit def elementsToSeq(elems: Elements): List[Element] = 
    for (c <- 0 until elems.size toList)
      yield elems.get(c)
  implicit def nodesToSeq(nodes: Nodes): List[nu.xom.Node] = 
    for (c <- 0 until nodes.size toList)
      yield nodes.get(c)
  
  implicit def nodesToNodesUtil(nodes: Nodes): NodesUtil =
    new NodesUtil(nodes)
  implicit def elementToNodesUtil(elem: Element): NodesUtil =
    new NodesUtil(new Nodes(elem))
  
  class NodesUtil(val nodes: Nodes) {
    def \(that: String): Nodes = {
      find((e) => e.getLocalName == that)
    }
    def find(comp: Element => Boolean): Nodes = {
      val r = new Nodes
      for (i <- 0 until nodes.size) {
        val n = nodes.get(i)
        if (n.isInstanceOf[Element]) {
          find(comp, n.asInstanceOf[Element], r)
        }
      }
      r
    }
    def find(comp: Element => Boolean, e: Element, r: Nodes) {
      val ce = e.getChildElements()
      for (j <- 0 until ce.size) {
        val e = ce.get(j)
        if (comp(e)) {
          r.append(e)
        }
      }
    }
  }
  
}

/**
 * XML Utilities
 */
class XMLUtils() {
  private val manager: CatalogManager = new CatalogManager
  manager.setIgnoreMissingProperties(true)
  manager.setPreferPublic(true)
  manager.setVerbosity(0)
  
  /**
   * Resolving XML factory adapter
   * 
   * @param manager catalog manager
   */
  /*
  class ResolvingFactoryAdapter(manager: CatalogManager) extends NoBindingFactoryAdapter {
    val resolver = new CatalogResolver(manager)
    
    override def resolveEntity(publicId: String, systemId: String): InputSource =
      resolver.resolveEntity(publicId, systemId)
  
  }
  */
  
  def catalogFiles(catalogFiles: File*) {
      manager.setCatalogFiles(catalogFiles.map(_.toURI.toString) mkString ";")
  }
  
  /**
   * Parse XML using catalog resolution.
   * 
   * @param f file parse
   * @return 
   */
  /*
  def loadXML(f: URI): Option[Node] = {
    val a = new ResolvingFactoryAdapter(manager)
    try {
      Some(a.loadXML(new InputSource(f.toString)))
    } catch {
      case e => {
        Console.err.println("ERROR: Failed to parse " + f + ": " + e.getMessage)
        None
      }
    }
  }
  */
    
  /**
   * Parse XML document
   * 
   * @param f document to parse
   * @return parsed document
   */
  def xparse(f: URI): Option[Document] = {
    if (XMLUtils.getFile(f).exists) {
      val b = new Builder(XMLUtils.getParser)
      try {
        Some(b.build(f.toString))  
      } catch {
        case _ => None
      }
    } else {
      Console.err.println("ERROR: " + f + " does not exist")
      None
    }
  }
  
  /**
   * Parse XML document with catalog
   * 
   * @param f document to parse
   * @return parsed document
   */
  def parseResolving(f: URI, validate: Boolean): Option[Document] = {
    if (XMLUtils.getFile(f).exists) {
      val xmlReader = getResolvingParser
      xmlReader.setFeature("http://xml.org/sax/features/validation", validate)
      val b = new Builder(new InfoAdder(xmlReader, f))
      try {
        Some(b.build(f.toString))  
      } catch {
        case _ => None
      }
    } else {
      println("ERROR: " + f + " does not exist")
      None
    }
  }
  
  /**
   * XML filter that adds debug attributes
   * 
   * @param parser parent parser
   * @param base URI of the files being parsed
   */
  private class InfoAdder(parser: XMLReader, base: URI) extends XMLFilterImpl(parser) {
    var first = true
    var isDita = false
    override def startElement(uri: String, localName: String, qName: String, atts: Attributes) {
      if (first) {
        isDita = atts.getIndex(Dita.Namespace, "DITAArchVersion") != -1
        first = false
      }
      val a = if (isDita) {
          val ai = new AttributesImpl(atts)
          ai.addAttribute("", "xtrf", "xtrf", "CDATA", base.toString) 
          ai
        } else atts
      getContentHandler.startElement(uri, localName, qName, a)
    }
  }

  /**
   * Get XML parser
   * 
   * @return XML parser
   */
  private def xgetParser: XMLReader = SAXParserFactory.newInstance.newSAXParser.getXMLReader

  /**
   * Get resolving XML parser
   * 
   * @return XML parser
   */
  private def getResolvingParser: XMLReader = new ResolvingXMLReader(manager)
  
}
object XMLUtils {
  
  /**
   * Parse XML document
   * 
   * @param f document to parse
   * @return parsed document
   */
  def parse(f: URI): Option[Document] = {
    if (getFile(f).exists) {
      val b = new Builder(getParser)
      try {
        Some(b.build(f.toString))  
      } catch {
        case e => {
        	e.printStackTrace()
        	None
        }
      }
    } else {
      Console.err.println("ERROR: " + f + " does not exist")
      None
    }
  }
  
  /**
   * Serialize document without doctype.
   * 
   * @param d document to serialize
   * @param f target URI
   */
  def serialize(d: Document, f: URI) {
    val of = getFile(f)
    if (!of.getParentFile.exists && !of.getParentFile.mkdirs) {
      throw new IOException("Failed to make directory " + of.getParentFile.getAbsolutePath)
    }
    val o = new BufferedOutputStream(new FileOutputStream(of))
    try {
      val s = new Serializer(o)
      for (i <- 0 until d.getChildCount) {
        if (d.getChild(i).isInstanceOf[DocType]) d.removeChild(i)
      }
      s.write(d)
    } finally {
      o.close()
    }
  }
  
  /**
   * Get XML parser
   * 
   * @return XML parser
   */
  private def getParser: XMLReader = SAXParserFactory.newInstance.newSAXParser.getXMLReader

  private def getFile(u: URI): File =
    new File(new URI(u.getScheme(),
                     u.getUserInfo(), u.getHost(), u.getPort(),
                     u.getPath(), u.getQuery(),
                     null))

  
}
class URIUtils(u: URI) {
  def setFragment(fragment: String): URI = 
    new URI(u.getScheme(),
            u.getUserInfo(), u.getHost(), u.getPort(),
            u.getPath(), u.getQuery(),
            fragment)
}
object URIUtils {
  implicit def URI2URIUtils(u: URI) =
    new URIUtils(u)
}
