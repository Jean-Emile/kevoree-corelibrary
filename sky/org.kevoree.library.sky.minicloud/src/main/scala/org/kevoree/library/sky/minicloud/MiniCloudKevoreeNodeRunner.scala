package org.kevoree.library.sky.minicloud

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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io._
import java.lang.Thread
import util.matching.Regex
import org.kevoree.ContainerRoot
import org.kevoree.framework.{AbstractNodeType, KevoreeXmiHelper}
import org.kevoree.library.sky.api.KevoreeNodeRunner
import org.kevoree.impl.DefaultKevoreeFactory
import scala.collection.JavaConversions._

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/09/11
 * Time: 11:46
 *
 * @author Erwan Daubert
 * @version 1.0
 */
class MiniCloudKevoreeNodeRunner(nodeName: String, iaasNode: AbstractNodeType) extends KevoreeNodeRunner(nodeName) {
  private val logger: Logger = LoggerFactory.getLogger(classOf[MiniCloudKevoreeNodeRunner])

  val factory = new DefaultKevoreeFactory
  private var nodePlatformProcess: Process = null

  val backupRegex = new Regex(".*<saveRes(.*)/>.*")
  val deployRegex = new Regex(".*<deployRes(.*)/>.*")
  val errorRegex = new Regex(".*Error while update.*")
  val starting = true

  case class DeployResult(uuid: String)

  case class BackupResult(uuid: String)

  case class ErrorResult()

  def startNode(iaasModel: ContainerRoot, childBootstrapModel: ContainerRoot): Boolean = {
    try {
      logger.debug("Start " + nodeName)
      val version = findVersionForChildNode(nodeName, childBootstrapModel, iaasModel.getNodes.find(n => n.getName == iaasNode.getNodeName).get)

      if (version == factory.getVersion) {
        logger.debug("try to start child node with the same Kevoree version")
        // find the classpath of the current node
        // if classpath is null then we download the jar with aether
        // else we start the child node with the same classpath as its parent.
        // main class  = org.kevoree.platform.standalone.app.Main
        logger.debug(System.getProperty("java.class.path"))
        if (System.getProperty("java.class.path") != null) {
          logger.debug("trying to start child node with parent's classpath")
          if (!startWithClassPath(childBootstrapModel)) {
            logger.debug("Unable to start child node with parent's classpath, try to use aether bootstrap")
            startWithAether(childBootstrapModel, version)
          } else {
            true
          }
        } else {
          logger.debug("trying to start child node with aether bootstrap")
          startWithAether(childBootstrapModel, version)
        }
      } else {
        logger.debug("try to start child node with {} as Kevoree version", version)
        startWithAether(childBootstrapModel, version)
      }
    } catch {
      case e: IOException => {
        logger.error("Unexpected error while trying to start " + nodeName, e)
        false
      }
    }
  }

  def stopNode(): Boolean = {
    logger.debug("Kill " + nodeName)
    try {
      if (nodePlatformProcess != null) {
        nodePlatformProcess.destroy()
      }
      true
    }
    catch {
      case _@e => {
        logger.debug(nodeName + " cannot be killed. Try to force kill...")
        if (nodePlatformProcess != null) {
          nodePlatformProcess.destroy()
        }
        logger.debug(nodeName + " has been forcibly killed")
        true
      }
    }
  }

  private def getJava: String = {
    val java_home: String = System.getProperty("java.home")
    java_home + File.separator + "bin" + File.separator + "java"
  }

  private def startWithClassPath(childBootStrapModel: ContainerRoot): Boolean = {
    try {
      val java: String = getJava
      val tempFile = File.createTempFile("bootModel" + nodeName, ".kev")
      KevoreeXmiHelper.instance$.save(tempFile.getAbsolutePath, childBootStrapModel)


      /*if (System.getProperty("java.class.path").contains("plexus-classworlds")) {
        //whe maven is used, it dybnamically loads some libraries which are not on the classpath and  "org.kevoree.platform.standalone.App" may be available but is not defined on the classpath properties
        return false //maven use case
      }*/

      //TRY THE CURRENT CLASSLOADER
      try {
        Thread.currentThread().getContextClassLoader.loadClass("org.kevoree.platform.standalone.App")
      } catch {
        case _@e => {
          logger.info("Can't find bootstrap class {}", "org.kevoree.platform.standalone.App")
          return false
        }
      }


      val vmargsObject = iaasNode.getDictionary.get("VMARGS")
      var exec = Array[String](java)
      if (vmargsObject != null) {
        exec = Array[String](java, vmargsObject.toString)
      }
      exec = exec ++ List[String]("-Dnode.headless=true", "-Dnode.bootstrap=" + tempFile.getAbsolutePath, "-Dnode.name=" + nodeName, "-classpath", System.getProperty("java.class.path"),
        "org.kevoree.platform.standalone.App")
      logger.info("Start node with command: {}", exec.mkString(" "))
      nodePlatformProcess = Runtime.getRuntime.exec(exec)

      configureLogFile(iaasNode, nodePlatformProcess)

      Thread.sleep(2000) // waiting the jvm is loaded
      val exitValue = nodePlatformProcess.exitValue
      logger.debug("Unable to start platform from current classloader. ExitValue={}", exitValue)
      false
    } catch {
      case e: IllegalThreadStateException => {
        logger.info("platform " + nodeName + " is started")
        true
      }
      case _@e => {
        logger.debug("Unable to start platform from current classloader", e)
        false
      }
    }
  }

  private def startWithAether(childBootStrapModel: ContainerRoot, kevoreeVersion: String): Boolean = {
    try {
      val platformFile = iaasNode.getBootStrapperService.resolveKevoreeArtifact("org.kevoree.platform.standalone", "org.kevoree.platform", kevoreeVersion)
      val java: String = getJava
      if (platformFile != null) {

        val tempFile = File.createTempFile("bootModel" + nodeName, ".kev")
        KevoreeXmiHelper.instance$.save(tempFile.getAbsolutePath, childBootStrapModel)

        val vmargsObject = iaasNode.getDictionary.get("VMARGS")
        var exec = Array[String](java)
        if (vmargsObject != null) {
          exec = Array[String](java, vmargsObject.toString)
        }
        exec = exec ++ List[String]("-Dnode.headless=true", "-Dnode.bootstrap=" + tempFile.getAbsolutePath, "-Dnode.name=" + nodeName, "-jar", platformFile.getAbsolutePath)


        logger.info("Start node with command: {}", exec.mkString(" "))
        nodePlatformProcess = Runtime.getRuntime.exec(exec)

        configureLogFile(iaasNode, nodePlatformProcess)

        Thread.sleep(2000) // waiting the jvm is loaded
        nodePlatformProcess.exitValue
        false
      } else {
        logger.error("Unable to start node because the platform jar file is not available")
        false
      }
    } catch {
      case e: IllegalThreadStateException => {
        logger.info("platform " + nodeName + " is started")
        true
      }
    }
  }

  def addNode(iaasModel: ContainerRoot, childBootStrapModel: ContainerRoot): Boolean = true

  def removeNode(): Boolean = true
}

