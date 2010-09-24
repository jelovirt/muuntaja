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

import nu.xom.{Document, ParentNode, Element, Attribute, Elements, Nodes, Node, ProcessingInstruction, Text, Builder, Serializer, DocType}
import scala.collection.mutable.ListBuffer

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
  
  /**
   * Get XPath-like path to a node.
   * 
   * <p>Namespaces are represented using Clark-notation.</p>
   * 
   * @param node no to return path for
   * @return path to node
   */
  def getPath(node: Node): String = {
    val l = new ListBuffer[String]()
    var n = node
    while (n != null && !n.isInstanceOf[Document]) {
      if (n.isInstanceOf[Text]) {
        l += "text()"
      } else if (n.isInstanceOf[Attribute]) {
        val a = n.asInstanceOf[Attribute] 
        l += "@%s%s".format(if (a.getNamespaceURI != "") "{" + a.getNamespaceURI + "}" else "", a.getLocalName)
      } else if (n.isInstanceOf[Element]) {
        val e = n.asInstanceOf[Element]
        var i = 1
        val parent = n.asInstanceOf[Element].getParent
        var r = parent.indexOf(n) - 1
        while (r >= 0) {
          val sibling = parent.getChild(r)
          if (sibling.isInstanceOf[Element] &&
              sibling.asInstanceOf[Element].getLocalName == e.getLocalName &&
              sibling.asInstanceOf[Element].getNamespaceURI == e.getNamespaceURI) {
            i = i + 1
          }
          r = r - 1
        }
        //l += n.asInstanceOf[Element].getLocalName
        l += "%s%s[%d]".format(if (e.getNamespaceURI != "") "{" + e.getNamespaceURI + "}" else "", e.getLocalName, i)
      } else if (n.isInstanceOf[ProcessingInstruction]) {
        l += "processing-instruction(%s)".format(n.asInstanceOf[ProcessingInstruction].getTarget)
      }
      n = n.getParent
    }
    ("" /: l.reverse)(_ + "/" + _)
  }
  /*
  # Element
  # Document
  # Text
  # Comment
  # Attribute
  # ProcessingInstruction
  # DocType
  # Namespace
  */
  
}