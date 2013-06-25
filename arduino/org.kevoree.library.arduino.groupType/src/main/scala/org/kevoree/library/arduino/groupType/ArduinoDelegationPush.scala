package org.kevoree.library.arduino.groupType

import org.slf4j.{Logger, LoggerFactory}
import reflect.BeanProperty
import java.lang.String
import org.kevoree.api.service.core.handler.{KevoreeModelHandlerService}
import org.kevoree.api.service.core.script.KevScriptEngineFactory
import org.kevoree.extra.kserial.Utils.KHelpers
import org.kevoree.{Dictionary, Instance, KevoreeFactory, ContainerRoot}
import scala.collection.JavaConversions._
import org.kevoree.api.NodeType
import org.kevoree.impl.DefaultKevoreeFactory

/**
 * User: ffouquet
 * Date: 10/08/11
 * Time: 13:55
 */

class ArduinoDelegationPush (handler: KevoreeModelHandlerService, groupName: String, bs: org.kevoree.api.Bootstraper, kevSFact: KevScriptEngineFactory) {
  protected var logger: Logger = LoggerFactory.getLogger(this.getClass)
  @BeanProperty
  var model: ContainerRoot = _

  def deployAll () {

    if (model == null) {
      model = handler.getLastModel
    }
    model.getGroups.find(g => g.getName == groupName) match {
      case Some(group) => {
        group.getSubNodes.filter(sub => sub.getTypeDefinition.getName.toLowerCase.contains("arduino")).foreach {
          subNode =>
            deployNode(subNode.getName)
        }
      }
      case None => logger.warn("Group Not found")
    }
  }


  private def setProperty (model: ContainerRoot, instance: Instance, name: String, key: String, isFragment: Boolean = false, nodeNameForFragment: String = "", value: Object) = {
    instance.getDictionary match {
      case null => {
        None
      }
      case dictionary:Dictionary => {
        dictionary.getValues.find(dictionaryAttribute =>
          dictionaryAttribute.getAttribute.getName == key &&
            ((isFragment && dictionaryAttribute.getTargetNode!=null && dictionaryAttribute.getTargetNode.getName == nodeNameForFragment) || !isFragment)) match {
          case None => // todo
          case Some(dictionaryAttribute) => dictionaryAttribute.setValue(value.toString)
        }
      }
    }
  }


  def deployNode (targetNodeName: String): Boolean = {
    if (model == null) {
      model = handler.getLastModel
    }
    try {
      val nodeType = bs.bootstrapNodeType(model, targetNodeName, handler, kevSFact)
      nodeType match {
        case gNodeType:NodeType => {
          model.getGroups.find(g => g.getName == groupName) match {
            case Some(group) => {


              if (group.getDictionary== null){
                val newdic = new DefaultKevoreeFactory().createDictionary
                group.setDictionary(newdic)
              }

              val dictionary = group.getDictionary

              val serialPortOption = org.kevoree.framework.KevoreePropertyHelper.instance$.getProperty(group, "serialport", true, targetNodeName)
              var serialPort = "*"
              if ((serialPortOption != null && serialPortOption == "*") || serialPortOption == null) {
                val ports = KHelpers.getPortIdentifiers
                if (ports.size() > 0) {
                  serialPort = ports.get(0)
                }
              } else {
                // update model
                serialPort = serialPortOption
                model.getHubs.foreach(channel => {
                  setProperty(model, channel, channel.getName, "serialport", true, targetNodeName, serialPort.replace("/", ";"))
                })

              }



              var sent = false
              //  val att = dictionary.getValues.find(value => value.getAttribute.getName == "serialport" && value.getTargetNode == targetNodeName).getOrElse(null)
              gNodeType.startNode()
              gNodeType.getClass.getMethods.find(method => method.getName == "push") match {
                case Some(method) => {
                  sent = method.invoke(gNodeType, targetNodeName, model, serialPort).asInstanceOf[Boolean]
                }
                case None => logger.error("No push method in group for name {}", groupName); throw new Exception("No push method in group for name " + groupName)
              }
              gNodeType.stopNode()
              sent

            }
            case None => logger.error("Group not found for name {}", groupName); throw new Exception("Group not found for name " + groupName)
          }
        }
        case null => logger.warn("Can't bootstrap NodeType"); throw new Exception("Can't bootstrap NodeType")
      }
    } catch {
      case _@e => logger.error("Can deploy model to node " + targetNodeName, e); throw e
    }
  }


}