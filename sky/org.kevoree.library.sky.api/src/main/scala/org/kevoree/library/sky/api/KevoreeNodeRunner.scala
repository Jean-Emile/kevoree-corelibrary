package org.kevoree.library.sky.api

import org.slf4j.{LoggerFactory, Logger}
import org.kevoree.{ContainerNode, TypeDefinition, ContainerRoot}
import java.io._
import scala.collection.JavaConversions._
import org.kevoree.impl.DefaultKevoreeFactory
import org.kevoree.framework.AbstractNodeType

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


/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/09/11
 * Time: 11:46
 *
 * @author Erwan Daubert
 * @version 1.0
 */
abstract class KevoreeNodeRunner(var nodeName: String) {

  private val logger: Logger = LoggerFactory.getLogger(classOf[KevoreeNodeRunner])

  def addNode(iaasModel: ContainerRoot, childBootStrapModel: ContainerRoot) : Boolean

  def startNode(iaasModel: ContainerRoot, childBootStrapModel: ContainerRoot): Boolean

  def stopNode(): Boolean

  def removeNode() : Boolean

  //def updateNode (modelPath: String): Boolean


  var outFile: File = null

  def getOutFile = outFile

  var errFile: File = null

  def getErrFile = errFile

  /**
   * configure the ssh server
   * @param path
   * @param ips
   */
  def configureSSHServer(path: String, ips: List[String]) {
    if (ips.size > 0) {
      logger.debug("configure ssh server ip")
      try {
        replaceStringIntoFile("ListenAddress <ip_address>", "ListenAddress " + ips.mkString("\nListenAddress "), path + File.separator + "etc" + File.separator + "ssh" + File.separator + "sshd_config")
      } catch {
        case _@e =>
          logger.debug("Unable to configure ssh server", e)
      }
    }
  }

  private def isASubType(nodeType: TypeDefinition, typeName: String): Boolean = {
    nodeType.getSuperTypes.find(td => td.getName == typeName || isASubType(td, typeName)) match {
      case None => false
      case Some(typeDefinition) => true
    }
  }

  @throws(classOf[Exception])
  private def copyStringToFile(data: String, outputFile: String) {
    if (data != null && data != "") {
      if (new File(outputFile).exists()) {
        new File(outputFile).delete()
      }
      val writer = new DataOutputStream(new FileOutputStream(new File(outputFile)))

      writer.write(data.getBytes)
      writer.flush()
      writer.close()
    }
  }

  def copyFile(inputFile: String, outputFile: String): Boolean = {
    if (new File(inputFile).exists()) {
      try {
        if (new File(outputFile).exists()) {
          new File(outputFile).delete()
        }
        val reader = new DataInputStream(new FileInputStream(new File(inputFile)))
        val writer = new DataOutputStream(new FileOutputStream(new File(outputFile)))

        val bytes = new Array[Byte](2048)
        var length = reader.read(bytes)
        while (length != -1) {
          writer.write(bytes, 0, length)
          length = reader.read(bytes)

        }
        writer.flush()
        writer.close()
        reader.close()
        true
      } catch {
        case _@e => logger.error("Unable to copy " + inputFile + " on " + outputFile, e); false
      }
    } else {
      logger.debug("Unable to find {}", inputFile)
      false
    }
  }

  @throws(classOf[java.lang.StringIndexOutOfBoundsException])
  private def replaceStringIntoFile(dataToReplace: String, newData: String, file: String) {
    if (dataToReplace != null && dataToReplace != "" && newData != null && newData != "") {
      if (new File(file).exists()) {
        val stringBuilder = new StringBuilder
        val reader = new DataInputStream(new FileInputStream(new File(file)))
        val writer = new ByteArrayOutputStream()

        val bytes = new Array[Byte](2048)
        var length = reader.read(bytes)
        while (length != -1) {
          writer.write(bytes, 0, length)
          length = reader.read(bytes)

        }
        writer.flush()
        writer.close()
        reader.close()
        stringBuilder append new String(writer.toByteArray)
        if (stringBuilder.indexOf(dataToReplace) == -1) {
          logger.debug("Unable to find {} on file {} so replacement cannot be done", Array[String](dataToReplace, file))
        } else {
          stringBuilder.replace(stringBuilder.indexOf(dataToReplace), stringBuilder.indexOf(dataToReplace) + dataToReplace.length(), newData)

          copyStringToFile(stringBuilder.toString(), file)
        }
      } else {
        logger.debug("The file {} doesn't exist, nothing can be replace.", file)
      }
    }
  }


  def findVersionForChildNode(nodeName: String, model: ContainerRoot, iaasNode: ContainerNode): String = {
    val factory = new DefaultKevoreeFactory
    logger.debug("looking for Kevoree version for node {}", nodeName)
    model.getNodes.find(n => n.getName == nodeName) match {
      case None => factory.getVersion
      case Some(node) => {
        logger.debug("looking for deploy unit")
        node.getTypeDefinition.getDeployUnits.find(d => d.getTargetNodeType.getName == iaasNode.getTypeDefinition.getName ||
          isASubType(iaasNode.getTypeDefinition, d.getTargetNodeType.getName)) match {
          case None => factory.getVersion
          case Some(d) => {
            logger.debug("looking for version of kevoree framework for the found deploy unit")
            d.getRequiredLibs.find(rd => rd.getUnitName == "org.kevoree" && rd.getGroupName == "org.kevoree.framework") match {
              case None => factory.getVersion
              case Some(rd) => rd.getVersion
            }
          }
        }

      }
    }
  }

  def configureLogFile(iaasNode: AbstractNodeType, process: Process) {
    var logFolder = System.getProperty("java.io.tmpdir")
    if (iaasNode.getDictionary.get("log_folder") != null && new File(iaasNode.getDictionary.get("log_folder").toString).exists()) {
      logFolder = iaasNode.getDictionary.get("log_folder").toString
    }
    val logFile = logFolder + File.separator + nodeName + ".log"
    outFile = new File(logFile + ".out")
    logger.info("writing logs about {} on {}", Array[String](nodeName, outFile.getAbsolutePath))
    new Thread(new ProcessStreamFileLogger(process.getInputStream, outFile)).start()
    errFile = new File(logFile + ".err")
    logger.info("writing logs about {} on {}", Array[String](nodeName, errFile.getAbsolutePath))
    new Thread(new ProcessStreamFileLogger(process.getErrorStream, errFile)).start()
  }
}

