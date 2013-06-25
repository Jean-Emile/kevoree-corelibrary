/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.library.arduinoNodeType.generator

import org.kevoreeadaptation.AdaptationModel
import org.kevoree.library.arduinoNodeType.{PMemory, ArduinoBoardType}
import org.kevoree._
import framework.AbstractNodeType
import kompare.JavaSePrimitive
import scala.collection.JavaConversions._

class KevoreeCGenerator
  extends KevoreeComponentTypeClassGenerator
  with KevoreeCFrameworkGenerator
  with KevoreeChannelTypeClassGenerator
  with KevoreeCRemoteAdminGenerator
  with KevoreeCSchedulerGenerator
  with KevoreePersistenceGenerator {

  def generate(
                adaptModel: AdaptationModel,
                nodeName: String,
                outputDir: String,
                boardName: String,
                pmem: PMemory,
                pmax: String,
                anodeType : AbstractNodeType
                ) {

    val componentTypes = adaptModel.getAdaptations.filter(adt => adt.getPrimitiveType.getName == JavaSePrimitive.instance$.getAddType && adt.getRef.isInstanceOf[ComponentType])
    val channelTypes = adaptModel.getAdaptations.filter(adt => adt.getPrimitiveType.getName == JavaSePrimitive.instance$.getAddType && adt.getRef.isInstanceOf[ChannelType])
    var ktypes: List[TypeDefinition] = List()
    componentTypes.foreach {
      ctype => ktypes = ktypes ++ List(ctype.getRef.asInstanceOf[TypeDefinition])
    }
    channelTypes.foreach {
      ctype => ktypes = ktypes ++ List(ctype.getRef.asInstanceOf[TypeDefinition])
    }




    context.getGenerator.addLibrary("QueueList.h",this.getClass.getClassLoader.getResourceAsStream("arduino/library/QueueList/QueueList.h"))

    generateKcFrameworkHeaders(ktypes, ArduinoBoardType.getFromTypeName(boardName), pmax)
    generateKcConstMethods(nodeName,anodeType.getClass.getSimpleName,ktypes)
    generateConstCheckSum(nodeName,anodeType.getClass.getSimpleName,ktypes)
    generateKcFramework

    componentTypes.foreach {
      componentTypeAdaptation =>
        context.getGenerator.setTypeModel(componentTypeAdaptation.getRef.asInstanceOf[ComponentType])
        generateComponentType(componentTypeAdaptation.getRef.asInstanceOf[ComponentType], nodeName,anodeType)
    }
    channelTypes.foreach {
      channelTypeAdaptation =>
        context.getGenerator.setTypeModel(channelTypeAdaptation.getRef.asInstanceOf[ChannelType])
        generateChannelType(channelTypeAdaptation.getRef.asInstanceOf[ChannelType], nodeName,anodeType)
    }


    generateDestroyInstanceMethod(ktypes)
    generateParamMethod(ktypes)

    generateGlobalInstanceFactory(ktypes)
    generateRunInstanceMethod(ktypes)

    val instancesAdaption = adaptModel.getAdaptations.filter(adt => adt.getPrimitiveType.getName == JavaSePrimitive.instance$.getAddInstance).filter(adt => !adt.getRef.asInstanceOf[Instance].getTypeDefinition.isInstanceOf[GroupType])
    var instances: List[Instance] = List()
    instancesAdaption.foreach {
      instanceAdaption =>
        instances = instances ++ List(instanceAdaption.getRef.asInstanceOf[Instance])
    }

    generatePMemoryPrimitives(pmem);
    generateBindMethod(ktypes)
    generateUnBindMethod(ktypes)
    generatePushToChannelMethod(ktypes)

    generatePeriodicExecutionMethod(ktypes)
    generatePortQueuesSizeMethod(ktypes)
    generateNameToIndexMethod()
    generateCheckForAdminMsg()
    generateConcatKevscriptParser()
    generatePrimitivesPersistence();
    generatePMemInit(pmem);
    generateSavePropertiesMethod(ktypes)
    generateCompressEEPROM()
    generateSaveInstancesBindings(ktypes)
    generateParseCAdminMsg()
    generateSetup(instances, nodeName)
    generateNextExecutionGap(ktypes)
    generateTimeMethods()
    generateLoop
    generateFreeRamMethod()


    //GENERATE OUTPUT FILE
    context.toFile(outputDir, nodeName)

  }
}
