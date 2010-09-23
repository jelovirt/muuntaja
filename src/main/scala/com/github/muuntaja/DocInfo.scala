package com.github.muuntaja

import scala.collection.mutable
import nu.xom.{Element, Document}
import Dita._
import Dita.{Topic}
import XOM.nodesToSeq
import java.net.URI

//class TopicInfo(val id: String)
//object TopicInfo {
//  def apply(e: Element) = {
//    new TopicInfo(e.getAttributeValue("id"))
//  }
//}

/**
 * Document info.
 * 
 * The class is intended to be used during preprocessing
 * so that the initial normalization parse process can collect as much
 * information as possible for other preprocessing steps so that reparsing
 * is not required.
 * 
 * @todo Add DITA URI or URI
 * @todo Add list of links
 * 
 * @param id document identifier
 * @param ditaType document DITA type, e.g. concept
 * @param title document title
 * @param navTitle navigation title
 * @param desct document description
 * @param cls DITA class
 */
class DocInfo(val id: Option[String],
              val ditaType: Option[String],
              val title: Option[Element],
              val navTitle: Option[Element],
              val desc: Option[Element],
              val cls: Option[DitaType])

object DocInfo {
  
  import Dita.elementToDitaNodesUtil
  
  val empty = new DocInfo(None, None, None, None, None, None)
  
  /**
   * Collection document info from a document.
   * 
   * @param doc topic document
   */
  def apply(doc: Document): DocInfo =
     apply(doc.getRootElement)
  
  //def apply(u: DitaURI, base: URI): DocInfo =
  //  apply(new DitaURI(base.resolve(u.uri), u.topic, u.element))
  
  /**
   * Collect document info from a DITA URI.
   * 
   * @param u DITA URI of a topic document
   */
  def apply(u: DitaURI): DocInfo = {
    XMLUtils.parse(u.uri) match {
      case Some(doc) => apply(doc, u)
      case None => empty
    }
  } 
  
  /**
   * Collection document info from a nested topic. Convenience method.
   * 
   * @param doc topic document
   * @param id nested topic identifier
   */
  def apply(doc: Document, id: String): DocInfo =
    apply(doc, new DitaURI(new URI(""), Some(id), None))
  
  /**
   * Collection document info from a nested topic.
   * 
   * @param doc topic document
   * @param u DITA URI to a nested topic
   */
  def apply(doc: Document, u: DitaURI): DocInfo = {
    u match {
      case DitaURI(_, Some(topic), Some(element)) => {
        doc.query("//*[@id = '" + topic + "'][1]//*[@id = '" + element + "'][1]").toList match {
          case n :: ns => apply(n.asInstanceOf[Element])
          case _ => empty
        }
      }
      case DitaURI(_, Some(topic), None) => {
        doc.query("//*[@id = '" + topic + "'][1]").toList match {
          case n :: ns => apply(n.asInstanceOf[Element])
          case _ => empty
        }
      }
      case DitaURI(_, None, None) => {
        DocInfo(doc.getRootElement)
      }
      case _ => empty
    }
  } 
  
  /**
   * Read document info from an element.
   * 
   * @param root DITA topic element
   */
  def apply(root: Element): DocInfo = {
	val id = root.getAttributeValue("id") match {
		case null => None
		case a => Some(a)
	}
	val cls = Some(DitaType(root))
    val ditaType = Some(cls.get.localName)
    val title = root \ Topic.Title headOption match {
    	case Some(e) => Some(createElement(Topic.Title, e))
    	case _ => None
    }
	val navTitle = root \ Topic.Titlealts \ Topic.Navtitle headOption match {
    	case Some(e) => Some(createElement(Topic.Title, e))
    	case _ => None
    } 
    val desc = root \ Topic.Shortdesc headOption match {
    	case Some(e) => Some(createElement(Topic.Desc, e))
    	case _ => None
    }
    
    new DocInfo(id, ditaType, title, navTitle, desc, cls)//topics.toList
  }

  /**
   * @deprecated Use createElement(DitaType, Node) instead
   */
  @Deprecated
  private def copy(e: Element, n: String): Element = {
    val r = new Element(n)
    for (a <- Preprocessor.readMetaAttsr(e, Set.empty))
      r.addAttribute(a)
    for (i <- 0 until e.getChildCount)
      r.appendChild(e.getChild(i).copy)
    return r
  }
}    