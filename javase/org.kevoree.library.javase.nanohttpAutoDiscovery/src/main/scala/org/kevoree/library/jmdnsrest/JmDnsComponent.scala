package org.kevoree.library.jmdnsrest

/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.jmdns.{ServiceEvent, ServiceListener, ServiceInfo, JmDNS}
import org.kevoree.api.service.core.handler.KevoreeModelHandlerService
import org.kevoree.impl.DefaultKevoreeFactory
import org.slf4j.LoggerFactory
import java.net.InetAddress
import org.kevoree.{ContainerRoot, KevoreeFactory}
import java.util.{ArrayList, HashMap}
import org.kevoree.framework.KevoreePlatformHelper
import scala.collection.JavaConversions._

/**
 * User: ffouquet
 * Date: 13/09/11
 * Time: 17:42
 */

class JmDnsComponent(nodeName: String, groupName: String, modelPort: Int, modelHandler: KevoreeModelHandlerService, groupTypeName: String, interface: InetAddress /*, group : AbstractGroupType*/) {
  val logger = LoggerFactory.getLogger(this.getClass)
  var servicelistener: ServiceListener = null
  final val REMOTE_TYPE: String = "_kevoree-remote._tcp.local."
  val nodeAlreadydiscovery = new HashMap[String, ArrayList[String]]

  val factory = new DefaultKevoreeFactory


  def updateGroupProperty(currentModel: ContainerRoot, nodeName: String, nodeType: String, groupName: String, groupType: String, groupPort: String): Option[ContainerRoot] = {
    val groupTypeDef = currentModel.getTypeDefinitions.find(td => td.getName == groupType)
    val nodeTypeDef = currentModel.getTypeDefinitions.find(td => td.getName == nodeType)
    if (groupTypeDef.isEmpty || nodeTypeDef.isEmpty) {
      return None
    }
    //CREATE GROUP IF NOT EXIST
    val currentGroup = currentModel.getGroups.find(group => group.getName == groupName).getOrElse {
      val newgroup = factory.createGroup
      newgroup.setName(groupName)
      newgroup.setTypeDefinition(groupTypeDef.get)
      currentModel.addGroups(newgroup)
      newgroup
    }
    val remoteNode = currentModel.getNodes.find(n => n.getName == nodeName).getOrElse {
      val newnode = factory.createContainerNode
      newnode.setName(nodeName)
      currentModel.getTypeDefinitions.find(td => td.getName == nodeType).map {
        nodeType => newnode.setTypeDefinition(nodeType)
      }
      currentModel.addNodes(newnode)
      newnode
    }

    val dicTypeDef = currentGroup.getTypeDefinition().getDictionaryType
    if (dicTypeDef != null) {
      dicTypeDef.getAttributes.find(att => att.getName == "port").map {
        attPort =>
          val dic = currentGroup.getDictionary match {
            case null => {
              val dic = factory.createDictionary
              currentGroup.setDictionary(dic)
              dic
            }
            case d:org.kevoree.Dictionary => d
          }
          val dicValue = dic.getValues.find(dicVal => dicVal.getAttribute == attPort && dicVal.getTargetNode()!=null && dicVal.getTargetNode.getName == nodeName).getOrElse {
            val newDicVal = factory.createDictionaryValue
            newDicVal.setAttribute(attPort)
            newDicVal.setTargetNode(remoteNode)
            dic.addValues(newDicVal)
            newDicVal
          }
          dicValue.setValue(groupPort)
      }
    }
    if (currentGroup.getSubNodes.find(subNode => subNode.getName == nodeName).isEmpty) {
      currentGroup.addSubNodes(remoteNode)
    }
    Some(currentModel)
  }


  /**
   * add a node found in the model and request an update
   */
  def addNodeDiscovered(p1: ServiceInfo) {
    if (p1.getInet4Addresses.length != 0 && p1.getPort != 0) {

      if (nodeAlreadydiscovery.containsKey(groupName) == false) {
        val row = new java.util.ArrayList[String]()
        nodeAlreadydiscovery.put(groupName, row)
      }
      if (nodeAlreadydiscovery.get(groupName).contains(p1.getName.trim()) == false) {
        val typeNames = new String(p1.getTextBytes, "UTF-8")
        val typeNamesArray = typeNames.split("/")

        val uuidModel = modelHandler.getLastUUIDModel
        val model = uuidModel.getModel

        val resultModel = updateGroupProperty(model, p1.getName.trim(), typeNamesArray(1), groupName, typeNamesArray(0), p1.getPort.toString)
        resultModel.map {
          goodModel => {
            val model = goodModel
            nodeAlreadydiscovery.get(groupName).add(p1.getName.trim())

            if (p1.getName != nodeName) {
              KevoreePlatformHelper.instance$.updateNodeLinkProp(model, nodeName, p1.getName.trim(), org.kevoree.framework.Constants.instance$.getKEVOREE_PLATFORM_REMOTE_NODE_IP, p1.getInet4Addresses()(0).getHostAddress,
                "LAN-" + p1.getInet4Addresses()(0).getHostAddress, 100)
            }
            modelHandler.compareAndSwapModel(uuidModel, model)
            logger.debug("add node <" + p1.getName.trim() + "> on " + interface.getHostAddress)
          }
        }
        logger.debug("List of discovered nodes <" + nodeAlreadydiscovery.get(groupName) + ">")
      }
    }
  }

  /*
  Request from the user to scan the network
  */
  def requestUpdateList(time: Int) {
    for (ser <- jmdns.list(REMOTE_TYPE, time)) {
      addNodeDiscovered(ser)
    }
  }

  // Create JmDNS on all interfaces
  val jmdns = JmDNS.create(interface, nodeName + "." + interface.getAddress.toString)
  logger.debug(" JmDNS listen on " + jmdns.getInterface)

  servicelistener = new ServiceListener() {

    def serviceAdded(p1: ServiceEvent) {
      jmdns.requestServiceInfo(p1.getType, p1.getName, 1)
      addNodeDiscovered(p1.getInfo)
    }

    def serviceResolved(p1: ServiceEvent) {

      logger.debug("Service resolved: " + p1.getInfo.getQualifiedName + " port:" + p1.getInfo.getPort)
      addNodeDiscovered(p1.getInfo)
    }

    def serviceRemoved(p1: ServiceEvent) {
      logger.debug("Service removed " + p1.getName)
      // REMOVE NODE FROM JMDNS GROUP INSTANCES SUBNODES
      if (nodeAlreadydiscovery.containsKey(groupName) == true) {
        nodeAlreadydiscovery.get(groupName).remove(p1.getName.trim())
      }
    }
  }

  jmdns.addServiceListener(REMOTE_TYPE, servicelistener)


  new Thread() {
    override def run() {
      val nodeType = modelHandler.getLastModel.getNodes.find(n => n.getName == nodeName).get.getTypeDefinition.getName
      val pairservice: ServiceInfo = ServiceInfo.create(REMOTE_TYPE, nodeName, groupName, modelPort, "")
      pairservice.setText((groupTypeName + "/" + nodeType).getBytes("UTF-8"))
      jmdns.registerService(pairservice)
    }
  }.start()

  def close() {
    new Thread() {
      override def run() {
        if (servicelistener != null) {
          jmdns.removeServiceListener(REMOTE_TYPE, servicelistener)
        }
        jmdns.close()

      }
    }.start()
  }
}