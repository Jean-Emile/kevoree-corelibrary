/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.library.arduinoNodeType

import org.kevoreeadaptation.AdaptationModel
import org.kevoree.DeployUnit
import org.kevoree.kompare.JavaSePrimitive
import org.kevoree.framework.AbstractNodeType
import scala.collection.JavaConversions._

object TypeBundleBootstrap {

  def bootstrapTypeBundle(adaptationModel : AdaptationModel,nodeType : AbstractNodeType){
    //Add All ThirdParty
    adaptationModel.getAdaptations.filter(adaptation => adaptation.getPrimitiveType.getName == JavaSePrimitive.instance$.getAddDeployUnit).foreach{adaptation=>
      val cmd = AddThirdPartyCommand(adaptation.getRef.asInstanceOf[DeployUnit],nodeType.getBootStrapperService)
      cmd.execute()
    }
    //Add All TypeDefinitionBundle
    adaptationModel.getAdaptations.filter(adaptation => adaptation.getPrimitiveType.getName == JavaSePrimitive.instance$.getAddDeployUnit).foreach{adaptation=>
      val cmd = AddThirdPartyCommand(adaptation.getRef.asInstanceOf[DeployUnit],nodeType.getBootStrapperService)
      cmd.execute()
    }

  }

  def deployUnitKey(dp : DeployUnit) : String = {
    dp.getGroupName+dp.getUnitName+dp.getVersion+dp.getName
  }
  
}
