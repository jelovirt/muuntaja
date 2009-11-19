package com.github.muuntaja

import scala.collection.mutable
import nu.xom.{Element, Document}
import Dita._
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
 * @param id document identifier
 * @param ditaType document DITA type, e.g. concept
 * @param title document title
 * @param desct document description
 */
class DocInfo(val id: Option[String],
              val ditaType: Option[String],
              val title: Option[Element],
              val desc: Option[Element])
              //val topics: List[TopicInfo]

object DocInfo {
  
  //private val topicType = DitaType("- topic/topic ")
  
  import Dita.elementToDitaNodesUtil
  
  val empty = new DocInfo(None, None, None, None)
  
  def apply(doc: Document): DocInfo =
     apply(doc.getRootElement)

  def apply(u: DitaURI, base: URI): DocInfo = {
    XMLUtils.parse(base.resolve(u.uri)) match {
      case Some(doc) => {
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
      case None => empty
    }
  } 
   
  def apply(doc: Document, id: String): DocInfo =
    doc.query("//*[@id = '" + id + "'][1]").toList match {
      case n :: ns => apply(n.asInstanceOf[Element])
      case _ => new DocInfo(None, None, None, None)
    }
   
  def apply(root: Element): DocInfo = {
    val ditaType = Some(root.getLocalName)
    //val ts = root \ Preprocessor.TopicmetaType \ Preprocessor.TitleType
    val ts = root \ Preprocessor.TitleType
    val title = if (ts.size > 0) Some(copy(ts.get(0).asInstanceOf[Element], "title")) else None
    val ds = root \ Preprocessor.ShortdescType
    val desc = if (ds.size > 0) Some(copy(ds.get(0).asInstanceOf[Element], "desc")) else None
    //val topics = new mutable.ArrayBuffer[TopicInfo]()
    //walker(root, topics)
    
    new DocInfo(Some(root.getAttributeValue("id")), ditaType, title, desc)//topics.toList
  }
  
  //private def walker(e: Element, topics: mutable.ArrayBuffer[TopicInfo]) {
  //  topics += TopicInfo(e)
  //  for (c <- DitaElement(e).getChildElements(topicType)) {
  //    walker(c, topics) 
  //  }
  //}
   
  private def copy(e: Element, n: String): Element = {
    val r = new Element(n)
    for (a <- Preprocessor.readMetaAttsr(e, Set.empty))
      r.addAttribute(a)
    for (i <- 0 until e.getChildCount)
      r.appendChild(e.getChild(i).copy)
    return r
  }
}    