package org.kevoree.library.sky.provider

import org.kevoree._
import org.kevoree.api.service.core.script.KevScriptEngine
import org.kevoree.library.sky.api.helper.KloudModelHelper
import org.slf4j.{LoggerFactory, Logger}
import scala.collection.JavaConversions._
import scala.Some

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 02/11/12
 * Time: 11:44
 *
 * @author Erwan Daubert
 * @version 1.0
 */
object PaaSKloudReasoner extends KloudReasoner {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def appendCreatePaaSManagerScript(iaasModel: ContainerRoot, id: String, nodeName: String, kloudManagerName: String, kloudManagerNodeName: String, portName: String, kengine: KevScriptEngine) {
    // FIXME add parameters for channelType and componentPortName and ComponentTypeName
    val componentName = id + "Manager"
    kengine.addVariable("componentName", componentName)
    kengine.addVariable("nodeName", nodeName)
    kengine append "addComponent {componentName}@{nodeName} : PaaSManagerMaster"
    bindComponents(iaasModel, kengine, kloudManagerName, kloudManagerNodeName, portName, componentName, nodeName, "submit")
  }

  def appendCreateGroupScript(iaasModel: ContainerRoot, id: String, nodeName: String, paasModel: ContainerRoot, kengine: KevScriptEngine) {

    paasModel.findGroupsByID(id) match {
      case null => {
        // if the paasModel doesn't contain a Kloud group, then we add a default one
        /*val ipOption = NetworkHelper.getAccessibleIP(KevoreePropertyHelper.getNetworkProperties(iaasModel, nodeName, Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP))
        var ip = "127.0.0.1"
        if (ipOption.isDefined) {
          ip = ipOption.get
        }*/
        /* Warning This method try severals Socket to determine available port */
        // FIXME
        /*val portNumber = KloudNetworkHelper.selectPortNumber(ip, Array[Int]())
        kengine.addVariable("groupName", id)
        kengine.addVariable("nodeName", nodeName)
        kengine.addVariable("port", portNumber.toString)
        kengine.addVariable("ip", ip)
        kengine.addVariable("groupType", "KloudPaaSNanoGroup")

        kengine append "addGroup {groupName} : KloudPaaSNanoGroup {masterNode='{nodeName}={ip}:{port}'}"
        kengine append "addToGroup {groupName} {nodeName}"
        kengine append "updateDictionary {groupName} {port='{port}', ip='{ip}'}@{nodeName}"*/
      }
      /*case group: Group => {
        val ipOption = NetworkHelper.getAccessibleIP(KevoreePropertyHelper.getNetworkProperties(iaasModel, nodeName, Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP))
        var ip = "127.0.0.1"
        if (ipOption.isDefined) {
          ip = ipOption.get
        }
        /* Warning This method try severals Socket to determine available port
        // FIXME
        val portNumber = KloudNetworkHelper.selectPortNumber(ip, Array[Int]())
        kengine.addVariable("groupName", id)
        kengine.addVariable("nodeName", nodeName)
        kengine.addVariable("port", portNumber.toString)
        kengine.addVariable("ip", ip)
        kengine.addVariable("groupType", group.getTypeDefinition.getName)

        kengine append "addGroup {groupName} : {groupType}"
        kengine append "addToGroup {groupName} {nodeName}"
        kengine append "updateDictionary {groupName} {port='{port}', ip='{ip}'}@{nodeName}"

        if (group.getDictionary != null) {
          val defaultAttributes = getDefaultNodeAttributes(iaasModel, group.getTypeDefinition.getName)
          group.getDictionary.getValues
            .filter(value => value.getAttribute.getName != "ip" && value.getAttribute.getName != "port" && defaultAttributes.find(a => a.getName == value.getAttribute.getName).isDefined).foreach {
            value =>
              kengine.addVariable("attributeName", value.getAttribute.getName)
              kengine.addVariable("attributeValue", value.getValue)
              kengine append "updateDictionary {groupName} {{attributeName} = '{attributeValue}'}"
          }
        }
      }*/*/
    }
  }

  def selectIaaSNodeAsMaster(model: ContainerRoot): String = {
    val iaasNodes = model.getNodes.filter(n => KloudModelHelper.isIaaSNode(model, n))

    var minNbSlaves = Int.MaxValue
    var iaasNode: ContainerNode = null
    iaasNodes.foreach {
      node => {
        val nbSlaves = countSlaves(node.getName, model)
        if (minNbSlaves > nbSlaves) {
          minNbSlaves = nbSlaves
          iaasNode = node
        }
      }
    }
    iaasNode.getName
  }

  private def countSlaves(nodeName: String, iaasModel: ContainerRoot): Int = {
    /*iaasModel.getNodes.find(n => n.getName == nodeName)*/
    iaasModel.findNodesByID(nodeName) match {
      case null => logger.warn("The node {} doesn't exist !", nodeName); Int.MaxValue
      case node: ContainerNode => {
        // TODO replace when the nature will be added and managed on the model
        //        node.getComponents.filter(c => KloudModelHelper.isASubType(c.getTypeDefinition, "")).size
        node.getComponents.filter(c => KloudModelHelper.isASubType(c.getTypeDefinition, "PaaSManager")).size
      }
    }
  }

  def addNodes(addedNodes: java.util.List[ContainerNode], iaasModel: ContainerRoot, kengine: KevScriptEngine, masterComponentName: String, masterComponentTypeName: String, masterNodeName: String,
               masterPortName: String, slaveComponentTypeName: String, slavePortName: String): Boolean = {
    if (!addedNodes.isEmpty) {
      logger.debug("Try to add all user nodes into the Kloud")

      // create new node using PJavaSENode as type for each user node
      addedNodes.foreach {
        node =>
          kengine.addVariable("nodeName", node.getName)
          kengine.addVariable("nodeType", node.getTypeDefinition.getName)
          // TODO maybe we need to merge the deploy unit that offer this type if it is not one of our types
          // add node
          logger.debug("addNode {} : {}", Array[String](node.getName, node.getTypeDefinition.getName))
          kengine append "addNode {nodeName} : {nodeType}"
          // set dictionary attributes of node
          if (node.getDictionary != null) {
            //            scriptBuilder append "{"
            val defaultAttributes = getDefaultNodeAttributes(iaasModel, node.getTypeDefinition.getName)
            node.getDictionary.getValues
              .filter(value => defaultAttributes.find(a => a.getName == value.getAttribute.getName) match {
              case Some(attribute) => true
              case None => false
            }).foreach {
              value =>
                kengine.addVariable("attributeName", value.getAttribute.getName)
                kengine.addVariable("attributeValue", value.getValue)
                kengine append "updateDictionary {nodeName} {{attributeName} = '{attributeValue}'}"
            }
          }
          val slaveComponentName = node.getName + "Slave"
          val slaveNodeName = node.getName
          /*kengine addVariable("masterComponentName", masterComponentName)
                              kengine addVariable("masterComponentTypeName", masterComponentTypeName)
                              kengine addVariable("masterNodeName", masterNodeName)
                              kengine addVariable("masterPortName", masterPortName)*/
          kengine addVariable("slaveComponentName", slaveComponentName)
          kengine addVariable("slaveComponentTypeName", slaveComponentTypeName)
          kengine addVariable("slaveNodeName", slaveNodeName)
          kengine append "addComponent {slaveComponentName}@{slaveNodeName} : {slaveComponentTypeName}"
          bindComponents(iaasModel, kengine, masterComponentName, masterNodeName, masterPortName, slaveComponentName, slaveNodeName, slavePortName)
      }
      true
    } else {
      true
    }
  }

  def releasePlatform(id: String, iaasModel: ContainerRoot, kengine: KevScriptEngine): Boolean = {
    /*iaasModel.getGroups.find(g => g.getName == id)*/
    iaasModel.findGroupsByID(id) match {
      case null =>
      case group: Group => {
        group.getSubNodes.foreach {
          node =>
            kengine addVariable("nodeName", node.getName)
            kengine append "removeNode {nodeName}"
        }
        kengine addVariable("groupName", group.getName)
        kengine append "removeGroup {groupName}"
      }
    }
    true
  }

  private def bindComponents(model: ContainerRoot, kengine: KevScriptEngine, masterComponentName: String, masterNodeName: String, masterPortName: String, slaveComponentName: String,
                             slaveNodeName: String, slavePortName: String) {
    kengine addVariable("masterComponentName", masterComponentName)
    kengine addVariable("masterNodeName", masterNodeName)
    kengine addVariable("masterPortName", masterPortName)
    kengine addVariable("slaveComponentName", slaveComponentName)
    kengine addVariable("slaveNodeName", slaveNodeName)
    kengine addVariable("slavePortName", slavePortName)


    val channelOption = findChannel(masterComponentName, masterPortName, masterNodeName, model)
    if (channelOption.isEmpty) {
      kengine.addVariable("channelName", "channel" + System.currentTimeMillis())
      kengine.addVariable("channelType", "SocketChannel")

      kengine append "addChannel {channelName} : {channelType}" // FIXME channel type and dictionary
      kengine append "bind {masterComponentName}.{masterPortName}@{masterNodeName} => {channelName}"

    } else {
      kengine.addVariable("channelName", channelOption.get)
    }
    kengine append "bind {slaveComponentName}.{slavePortName}@{slaveNodeName} => {channelName}"

    // TODO set dictionary attribute specific for each nodes which hosts a fragment
    kengine append "updateDictionary {channelName} {{attributeName} = '{attributeValue}'}"
  }

  private def findChannel(componentName: String, portName: String, nodeName: String, model: ContainerRoot): Option[String] = {
    model.findByPath("nodes[" + nodeName + "]/components[" + componentName + "]", classOf[ComponentInstance]) match {
      case null => None
      case component: ComponentInstance =>
        component.getProvided.find(p => p.getPortTypeRef.getName == portName) match {
          case None =>
            component.getRequired.find(p => p.getPortTypeRef.getName == portName) match {
              case None => None
              case Some(port) =>
                if (port.getBindings.size > 0) {
                  Some(port.getBindings.get(0).getHub.getName)
                } else {
                  None
                }
            }
          case Some(port) =>
            if (port.getBindings.size > 0) {
              Some(port.getBindings.get(0).getHub.getName)
            } else {
              None
            }
        }
    }
    /*model.getMBindings.find(b => b.getPort.getPortTypeRef.getName == portName && b.getPort.eContainer.asInstanceOf[ComponentInstance].getName == componentName &&
      b.getPort.eContainer.eContainer.asInstanceOf[ContainerNode].getName == nodeName) match {
      case None => None
      case Some(binding) => {
        Some(binding.getHub.getName)
      }
    }*/
  }

  // this method is specific to the type of the channel we use to link PaaS components
  private def updateSocketChannelDictionary(model: ContainerRoot, channelName: String, nodeName: String, kengine: KevScriptEngine) {

    kengine addVariable("channelName", channelName)
    kengine addVariable("nodeName", nodeName)
    model.findHubsByID(channelName) match {
      case null => logger.warn("Unable to find channel '{}', unable to update its properties", channelName)
      case channel: Channel => {
        if (channel.getTypeDefinition.getName == "SocketChannel") {
          // looking for node specific dictionary values which correspond to the node define by nodeName

          // if the dictionary attributes don't exist, we try to add them
          kengine append "updateDictionary {channelName} {port = '{attributeValue}'}@{nodeName}"
        } else {
          logger.warn("Unable to fix channel parameters for the type '{}'", channel.getTypeDefinition.getName)
        }
      }
    }
  }
}
