package org.kevoree.library.sky.provider.web

import org.kevoree.library.javase.webserver.{KevoreeHttpResponse, KevoreeHttpRequest}
import org.kevoree.library.sky.api.helper.KloudModelHelper
import org.json.JSONStringer
import org.kevoree.api.service.core.script.KevScriptEngine
import org.slf4j.LoggerFactory
import util.matching.Regex
import org.kevoree.{TypeDefinition, ContainerNode, ContainerRoot}
import scala.collection.JavaConversions._

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 24/10/12
 * Time: 16:32
 *
 * @author Erwan Daubert
 * @version 1.0
 */
class IaaSKloudResourceManagerPageGenerator(instance: IaaSKloudResourceManagerPage, pattern: String, parentNodeName: String) extends KloudResourceManagerPageGenerator(instance, pattern) {
  logger = LoggerFactory.getLogger(this.getClass)

  val rootRequest1 = new Regex(pattern.substring(0, pattern.length - 1))
  val rootRequest2 = new Regex(pattern)
  val addChildRequest = new Regex(pattern + "AddChild")
  val removeChildRequest = new Regex(pattern + "RemoveChild")
  val NodeSubRequest = new Regex(pattern + "nodes/(.+)/(.+)")
  // TODO maybe remove nodes on regex
  val NodeHomeRequest = new Regex(pattern + "nodes/(.+)") // TODO maybe remove nodes on regex

  def internalProcess(request: KevoreeHttpRequest, response: KevoreeHttpResponse): PartialFunction[String, KevoreeHttpResponse] = {
    case rootRequest1() => getIaasPage(request, response)
    case rootRequest2() => getIaasPage(request, response)
    case addChildRequest() => addChild(request, response)
    case removeChildRequest() => removeChild(request, response)
    case NodeSubRequest(nodeName, fluxName) => getNodeLogPage(request, response, fluxName, nodeName)
    case NodeHomeRequest(nodeName) => getNodePage(request, response, nodeName)
  }

  private def getNodeLogPage(request: KevoreeHttpRequest, response: KevoreeHttpResponse, fluxName: String, nodeName: String): KevoreeHttpResponse = {
    val htmlContent = HTMLPageBuilder.getNodeLogPage(pattern, parentNodeName, nodeName, fluxName, instance.getModelService.getLastModel)
    response.setStatus(200)
    response.setContent(htmlContent)
    response
  }

  private def getNodePage(request: KevoreeHttpRequest, response: KevoreeHttpResponse, nodeName: String): KevoreeHttpResponse = {
    val htmlContent = HTMLPageBuilder.getNodePage(pattern, parentNodeName, nodeName, instance.getModelService.getLastModel)
    response.setStatus(200)
    response.setContent(htmlContent)
    response
  }


  private def getIaasPage(request: KevoreeHttpRequest, response: KevoreeHttpResponse): KevoreeHttpResponse = {
    val htmlContent = HTMLPageBuilder.getIaasPage(pattern, parentNodeName, instance.getModelService.getLastModel, instance.isPortBinded("delegate"))
    response.setStatus(200)
    response.setContent(htmlContent)
    response
  }

  private def getRedirectionPage(request: KevoreeHttpRequest, response: KevoreeHttpResponse, page : String): KevoreeHttpResponse = {
    val htmlContent = HTMLPageBuilder.getRedirectionPage(request.getCompleteUrl.substring(0, request.getCompleteUrl.indexOf(request.getUrl))+ page)
    response.setStatus(200)
    response.setContent(htmlContent)
    response
  }

  private def addChild(request: KevoreeHttpRequest, response: KevoreeHttpResponse): KevoreeHttpResponse = {
    if (request.getMethod.equalsIgnoreCase("GET")) {
      val model = instance.getModelService.getLastModel
      model.findByPath("nodes[" + parentNodeName + "]", classOf[ContainerNode]) match {
        case null => {
          response.setStatus(500)
          response.setContent("Unable to find the IaaS node: " + parentNodeName)
        }
        case parent: ContainerNode => {
          val paasNodeTypes = model.getTypeDefinitions.filter(nt => KloudModelHelper.isPaaSNodeType(model, nt) &&
            nt.getDeployUnits.find(dp => KloudModelHelper.isASubType(parent.getTypeDefinition, dp.getTargetNodeType.getName)).isDefined).toList
          val htmlContent = HTMLPageBuilder.addNodePage(pattern, paasNodeTypes)
          response.setStatus(200)
          response.setContent(htmlContent)
        }
      }
    } else {
      var jsonString: String = null
      if (request.getResolvedParams.get("request") == "add") {
        jsonString = addChildNode(request)
      } else if (request.getResolvedParams.get("request") == "list") {
        jsonString = getNodeTypeList
      }
      if (jsonString != null) {
        logger.debug(jsonString)
        response.setStatus(200)
        response.setContent(jsonString)
      } else {
        response.setStatus(500)
      }
    }
    response
  }

  private def addChildNode(request: KevoreeHttpRequest): String = {
    val kengine = instance.getKevScriptEngineFactory.createKevScriptEngine(initializeModel(instance.getModelService.getLastModel))
    val jsonresponse = new JSONStringer().`object`()
    if (request.getResolvedParams.get("type") != null) {
      // find the corresponding node type
      val typeName = request.getResolvedParams.get("type")
      instance.getModelService.getLastModel.findByPath("typeDefinitions[" + typeName + "]", classOf[TypeDefinition]) match {
        case null => jsonresponse.key("code").value("-1").key("message").value("There is no type named " + typeName)
        case nodeType: TypeDefinition => {
          var mustBeAdded = true
          if (request.getResolvedParams.get("name") != null) {
            kengine.addVariable("nodeName", request.getResolvedParams.get("name"))
            kengine.addVariable("nodeTypeName", typeName)
            kengine append "addNode {nodeName} : {nodeTypeName}"
            // check attributes
            if (nodeType.getDictionaryType != null) {
              nodeType.getDictionaryType.getAttributes.foreach {
                attribute => {
                  val value = request.getResolvedParams.get(attribute.getName)
                  if (value == null && !attribute.getOptional) {
                    nodeType.getDictionaryType.getDefaultValues.find(defaulValue => defaulValue.getAttribute.getName == attribute.getName) match {
                      case None => jsonresponse.key("code").value("-1").key("message").value("The attribute " + attribute.getName + " must be defined")
                      case Some(defaultValue) => {
                        kengine addVariable("attributeName", attribute.getName)
                        kengine addVariable("defaultValue", defaultValue.getValue)
                        kengine append "updateDictionary {nodeName} { {attributeName} = '{defaultValue}' }"
                      }
                    }
                  } else if (value != null) {
                    kengine addVariable("attributeName", attribute.getName)
                    kengine addVariable("value", value)
                    kengine append "updateDictionary {nodeName} { {attributeName} = '{value}' }"
                  }
                }
              }
            }
            try {
              new Thread() {
                override def run() {
                  instance.addToNode(kengine.interpret(), parentNodeName)
                }
              }.start()
              jsonresponse.key("code").value("0")
            } catch {
              case e: Throwable => logger.warn("Unable to add the child", e); jsonresponse.key("code").value("-2").key("message").value(e.getMessage)
            }
          } else {
            jsonresponse.key("code").value("-2").key("message").value("The name of the node must be defined")
            mustBeAdded = false
          }
        }
      }
    }
    jsonresponse.endObject().toString
  }

  private def getNodeTypeList: String = {
    val jsonresponse = new JSONStringer().`object`()
    val model = instance.getModelService.getLastModel
    model.getNodes.find(n => n.getName == parentNodeName) match {
      case None => {
        null
      }
      case Some(parent) => {
        val paasNodeTypes = model.getTypeDefinitions.filter(nt => KloudModelHelper.isPaaSNodeType(model, nt) &&
          nt.getDeployUnits.find(dp => KloudModelHelper.isASubType(parent.getTypeDefinition, dp.getTargetNodeType.getName)).isDefined)
        var types = List[String]()
        jsonresponse.key("request").value("list")

        paasNodeTypes.foreach {
          nodeType =>
            var msg = new JSONStringer().`object`().key("name").value("name").key("optional").value(false).endObject()
            var attributes = List[String](msg.toString)
            if (nodeType.getDictionaryType != null) {
              nodeType.getDictionaryType.getAttributes.foreach {
                attribute => {
                  msg = new JSONStringer().`object`().key("name").value(attribute.getName).key("optional").value(attribute.getOptional)
                  if (attribute.getDatatype.startsWith("enum")) {
                    val vals = attribute.getDatatype.substring("enum=".length).split(",")
                    msg.key("values").value(vals)
                  }

                  val defaultValue = nodeType.getDictionaryType.getDefaultValues.find(v => v.getAttribute.getName == attribute.getName) match {
                    case None => ""
                    case Some(v) => v.getValue
                  }
                  msg.key("defaultValue").value(defaultValue)
                  msg.endObject()
                  attributes = attributes ++ List[String](msg.toString)
                }
              }
            }
            jsonresponse.key(nodeType.getName).value(attributes.toArray)
            types = types ++ List[String](nodeType.getName)

        }
        jsonresponse.key("types").value(types.toArray).endObject()
        logger.debug(types.mkString(", "))
        jsonresponse.toString
      }
    }
  }

  private def removeChild(request: KevoreeHttpRequest, response: KevoreeHttpResponse): KevoreeHttpResponse = {
    val nodeName = request.getResolvedParams.get("name")


    new Thread() {
      override def run() {
        try {
          instance.remove(cleanModel(instance.getModelService.getLastModel, List[String](nodeName)))
        } catch {
          case e: Throwable => logger.warn("Unable to clean the current model to remove the child " + nodeName, e)
        }
      }
    }.start()

    getRedirectionPage(request, response, pattern)
  }

  private def initializeModel(currentModel: ContainerRoot): ContainerRoot = {
    val kengine: KevScriptEngine = instance.getKevScriptEngineFactory.createKevScriptEngine(currentModel)
    currentModel.getNodes.foreach {
      node =>
        kengine addVariable("nodeName", node.getName)
        kengine append "removeNode {nodeName}"
    }
    currentModel.getHubs.foreach {
      channel =>
        kengine addVariable("channelName", channel.getName)
        kengine append "removeChannel {channelName}"
    }
    currentModel.getGroups.foreach {
      group =>
        kengine addVariable("groupName", group.getName)
        kengine append "removeGroup {groupName}"
    }

    kengine.interpret()
  }

  private def cleanModel(currentModel: ContainerRoot, nodesToKeep: List[String]): ContainerRoot = {
    val kengine: KevScriptEngine = instance.getKevScriptEngineFactory.createKevScriptEngine(currentModel)
    currentModel.getNodes.filter(n => !nodesToKeep.contains(n.getName)).foreach {
      node =>
        kengine addVariable("nodeName", node.getName)
        // extract child node to avoid their deletion
        node.getHosts.foreach{
          child =>
            kengine addVariable("childName", child.getName)
            kengine append("removeChild {childName}@{nodeName}")
        }
        kengine append "removeNode {nodeName}"
    }
    currentModel.getHubs.foreach {
      channel =>
        kengine addVariable("channelName", channel.getName)
        kengine append "removeChannel {channelName}"
    }
    currentModel.getGroups.foreach {
      group =>
        kengine addVariable("groupName", group.getName)
        kengine append "removeGroup {groupName}"
    }

    kengine.interpret()
  }
}
