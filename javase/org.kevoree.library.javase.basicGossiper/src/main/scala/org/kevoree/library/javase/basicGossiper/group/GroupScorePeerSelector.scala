package org.kevoree.library.javase.basicGossiper.group

import scala.collection.JavaConversions._
import org.kevoree.api.service.core.handler.KevoreeModelHandlerService
import java.lang.Math
import actors.Actor
import collection.mutable
import org.kevoree.library.javase.basicGossiper.PeerSelector
import java.util.concurrent.atomic.AtomicReference
import org.kevoree.ContainerRoot

class GroupScorePeerSelector (timeout: Long, modelRef : AtomicReference[ContainerRoot], nodeName: String) extends PeerSelector {

  private val peerCheckMap = new mutable.HashMap[String, (Long, Int)]
  private val peerNbFailure = new mutable.HashMap[String, Int]

  def updateNodeScore (nodeName1: String, failure: Boolean) {
    this.modifyNodeScore(nodeName1, failure)
  }

  def resetAll () {
    reset();
  }

  def resetNodeFailureManagement (nodeName1: String) {
    this.resetNodeFailure(nodeName1)
  }

  def selectPeer (groupName: String): String = {
    this.selectPeerInternal(groupName)
  }

  private def selectPeerInternal (groupName: String): String = {
    val model = modelRef.get()

    model.getGroups.find(group => group.getName == groupName) match {
      case Some(group) => {
        //Found minima score node name
        var foundNodeName = List[String]()
        var minScore = Long.MaxValue
        group.getSubNodes.filter(node => !node.getName.equals(nodeName))/*.filter(node => model.getNodeNetworks
          .exists(nn => nn.getInitBy.get.getName == nodeName && nn.getTarget.getName == node.getName
          || nn.getInitBy.get.getName == node.getName && nn.getTarget.getName == node.getName))  */
          .foreach {
          subNode => {
            if (getScore(subNode.getName) <= minScore) {
              minScore = getScore(subNode.getName)
            }
          }
        }
        group.getSubNodes.filter(node => node.getName!=nodeName)
          /*.filter(node => model.getNodeNetworks
          .exists(nn => nn.getInitBy.getName == nodeName && nn.getTarget.getName == node.getName))*/
          .foreach {
          subNode => {
            if (getScore(subNode.getName) == minScore) {
              foundNodeName = foundNodeName ++ List(subNode.getName)
            }
          }
        }
        if (foundNodeName.size > 0) {
          // select randomly a peer between all potential available nodes which have a good score
          val nodeName1 = foundNodeName.get((Math.random() * foundNodeName.size).asInstanceOf[Int])
          //Init node score
          //initNodeScore(nodeName)
          modifyNodeScore(nodeName1, failure = false)

          org.kevoree.log.Log.debug("return a peer between connected nodes: " + nodeName1)
          nodeName1
        } else {
          org.kevoree.log.Log.debug("Unable to select a peer")
          ""
        }
      }
      case None => org.kevoree.log.Log.debug(groupName + " not Found so we are unable to select a peer"); ""
    }
  }

  private def getScore (nodeName: String): Long = {
    peerCheckMap.get(nodeName) match {
      case Some(nodeTuple) => nodeTuple._1
      case None => 0l //default
    }
  }

  /*private def initNodeScore(nodeName: String) {
    peerCheckMap.get(nodeName) match {
      case Some(nodeTuple) => {
            peerCheckMap.put(nodeName,Tuple2(System.currentTimeMillis,nodeTuple._2+1))
      }
      case None => peerCheckMap.put(nodeName,Tuple2(System.currentTimeMillis,0))
    }
  }*/

  private def modifyNodeScore (nodeName: String, failure: Boolean) {
    if (failure) {
      org.kevoree.log.Log.debug("increase node score of " + nodeName + " due to communication failure")
      peerNbFailure.get(nodeName) match {
        case Some(nodeTuple) => {
          peerNbFailure.put(nodeName, nodeTuple + 1)
          peerCheckMap.get(nodeName) match {
            case Some(nodeTuple1) => {
              peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, nodeTuple1._2 + 2 * (nodeTuple + 1)))
              org.kevoree.log.Log.debug("Node score of " + nodeName + " is now " + nodeTuple + 2 * (nodeTuple + 1))
            }
            case None => peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, 2)) // must not appear
          }
        }
        case None => {
          peerNbFailure.put(nodeName, 2)
          peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, 2))
        }
      }
    } else {
      peerCheckMap.get(nodeName) match {
        case Some(nodeTuple) => {
          peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, nodeTuple._2 + 1))
        }
        case None => peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, 0))
      }
    }
  }

  private def resetNodeFailure (nodeName: String) {
    peerNbFailure.get(nodeName) match {
      case Some(nodeTuple) => {
        peerNbFailure.put(nodeName, 0)
      }
      case None => {
        peerNbFailure.put(nodeName, 0)
      }
    }
  }

  private def reset () {
    peerCheckMap.keySet.foreach {
      nodeName =>
        peerCheckMap.put(nodeName, Tuple2(System.currentTimeMillis, 0))
        peerNbFailure.put(nodeName, 0)
        org.kevoree.log.Log.debug("spam to say that scores are reinitiliaze")
    }
  }

}