package com.github.muuntaja

import nu.xom.{Document, Element, Attribute, Text, Node, ParentNode, ProcessingInstruction, DocType, Comment}
import java.util.regex.Pattern
import scala.collection.mutable.ListBuffer

object DitaComparer {

  private val whitespace = Pattern.compile("\\s+")
  
  def compare(dExp: Document, dAct: Document): Boolean =
    compare(dExp, dAct, Nil, false)
  
  def compare(dExp: Document, dAct: Document, ignoredNs: List[String]): Boolean =
    compare(dExp, dAct, ignoredNs, false)
    
  def compare(dExp: Document, dAct: Document, ignoredNs: List[String], otCompatibility: Boolean): Boolean = {
    val cExp = dExp.copy
    val cAct = dAct.copy
    normalize(cExp, ignoredNs, otCompatibility)
    normalize(cAct, ignoredNs, otCompatibility)
    try {
      comp(cExp, cAct)
    } catch {
      case e: Throwable => {
        Console.err.println("ERROR: " + e.toString)
        return false
      }
    }
    return true
  }


  /** Recursive node normalization. */
  private def normalize(n: Node, ignoredNs: List[String], otCompatibility: Boolean) {
    if (n.isInstanceOf[Text]) {
      if (n.getValue.trim.length == 0) {
        //println("Remove whitespace text")
        n.getParent.removeChild(n)
      }
    } else if (n.isInstanceOf[Element]) {
      val e = n.asInstanceOf[Element]
      val keep = (e.getAttributeValue("ot-compatibility", Preprocessor.MUUNTAJA_NS), otCompatibility) match { 
                   case ("true", false) => false
                   case ("false", true) => false
                   case _ => true
                 }
      if (keep) {
        for (a <- 0.until(e.getAttributeCount).map(e.getAttribute(_)).toList) {
          if (// defaults
              (a.getLocalName == "scope" && a.getValue == "local") ||
              (a.getLocalName == "format" && a.getValue == "dita") ||
              // architectural
              (a.getLocalName == "DITAArchVersion" && a.getNamespaceURI == "http://dita.oasis-open.org/architecture/2005/") ||
              (a.getLocalName == "class") || (a.getLocalName == "domains") ||
              // debug
              (a.getLocalName == "xtrf") || (a.getLocalName == "xtrc") ||
              // ignorable
              ignoredNs.exists(_ == a.getNamespaceURI)) {
            e.removeAttribute(a)
          }
        }
        for(c <- 0.until(e.getChildCount).map(e.getChild(_)).toList) {
          normalize(c, ignoredNs, otCompatibility)
        }
      } else {        
        e.getParent.removeChild(e)
      }
    } else if (n.isInstanceOf[Document]) {
      val d = n.asInstanceOf[Document]
      for(c <- 0.until(d.getChildCount).map(d.getChild(_)).toList) {
        normalize(c, ignoredNs, otCompatibility)
      }
    } else if (n.isInstanceOf[ProcessingInstruction] ||
               n.isInstanceOf[Comment] ||
               n.isInstanceOf[DocType]) {
       n.getParent.removeChild(n)
    }
  }
 
  def comp(nExp: Node, nAct: Node) {
    // check type
    if (nExp.getClass != nAct.getClass) {
      throw new Exception("Type difference " + getPath(nExp) + ": " + nExp.toString)
    }
    // check children
    if (nExp.isInstanceOf[ParentNode]) {
      val pExp = nExp.asInstanceOf[ParentNode]
      val pAct = nAct.asInstanceOf[ParentNode]
      if (pExp.getChildCount != pAct.getChildCount) {
        throw new Exception("Child count difference " + getPath(pExp) + ": " + pExp.getChildCount + " != " + pAct.getChildCount)
      }
      for (i <- 0 until pExp.getChildCount) {
        comp(pExp.getChild(i), pAct.getChild(i))
      }
    }
    // check type specific
    if (nExp.isInstanceOf[Element]) {
      val eExp = nExp.asInstanceOf[Element]
      val eAct = nAct.asInstanceOf[Element]
      // check name and namespace
      if (eExp.getLocalName != eAct.getLocalName) {
        throw new Exception("Element name difference " + getPath(eExp) + ": " + nExp.toString)
      }
      if (eExp.getNamespaceURI != eAct.getNamespaceURI) {
        throw new Exception("Element namespace difference " + getPath(eExp) + ": " + nExp.toString)
      }
      // check attributes
      if (eExp.getAttributeCount != eAct.getAttributeCount) {
        //println(eExp.toXML)
        //println(eAct.toXML)
        throw new Exception("Attribute count difference " + getPath(eExp) + ": " + eExp.getAttributeCount + " != " + eAct.getAttributeCount)
      }
      // FIXME
      for (i <- 0 until eExp.getAttributeCount) {
        val aExp = eExp.getAttribute(i)
        val aAct = eAct.getAttribute(aExp.getLocalName, aExp.getNamespaceURI)
        if (aAct == null) {
          throw new Exception("Attribute not found " + getPath(eExp) + ": "
                              + (if (aExp.getNamespacePrefix != "") aExp.getNamespacePrefix + ":" else "" ) + aExp.getLocalName)
        } else {
          comp(aExp, aAct)
        }
      }
    } else if (nExp.isInstanceOf[Attribute]) {
      val aExp = nExp.asInstanceOf[Attribute]
      val aAct = nAct.asInstanceOf[Attribute]
      // check name and namespace
      if (aExp.getLocalName != aAct.getLocalName) {
        throw new Exception("Attribute name difference " + getPath(aExp) + ": " + nExp.toString)
      }
      if (aExp.getNamespaceURI != aAct.getNamespaceURI) {
        throw new Exception("Attribute namespace difference " + getPath(aExp) + ": " + nExp.toString)
      }
      // check value
      if (whitespace.matcher(aExp.getValue).replaceAll(" ").trim != whitespace.matcher(aAct.getValue).replaceAll(" ").trim) {
        throw new Exception("Attribute value difference " + getPath(aExp) + ": " + nExp.toString)
      }
    } else if (nExp.isInstanceOf[Text]) {
      val tExp = nExp.asInstanceOf[Text]
      val tAct = nAct.asInstanceOf[Text]
      // check value
      if (whitespace.matcher(tExp.getValue).replaceAll(" ").trim != whitespace.matcher(tAct.getValue).replaceAll(" ").trim) {
        throw new Exception("Text value difference " + getPath(tExp) + ": " + nExp.toString)
      }
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
  
  /**
   * Get XPath-like path to a node.
   * 
   * <p>Namespaces are represented using Clark-notation.</p>
   * 
   * @param node no to return path for
   * @return path to node
   */
  private def getPath(node: Node): String = {
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
}
