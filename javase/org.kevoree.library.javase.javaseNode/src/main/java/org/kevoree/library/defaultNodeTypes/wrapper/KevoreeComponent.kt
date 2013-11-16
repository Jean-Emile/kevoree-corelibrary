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
package org.kevoree.library.defaultNodeTypes.wrapper;

import org.kevoree.ComponentInstance
import org.kevoree.ContainerRoot
import org.kevoree.api.service.core.handler.KevoreeModelHandlerService
import org.kevoree.api.Bootstraper
import org.kevoree.api.service.core.script.KevScriptEngineFactory
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.library.defaultNodeTypes.reflect.FieldAnnotationResolver
import org.kevoree.log.Log
import java.lang.reflect.InvocationTargetException
import org.kevoree.api.dataspace.DataSpaceService
import org.kevoree.framework.AbstractComponentType
import org.kevoree.framework.KInstance
import org.kevoree.framework.ComponentType
import org.kevoree.framework.KevoreePort
import org.kevoree.framework.ModelHandlerServiceProxy

public class KevoreeComponent(val ct: AbstractComponentType, val nodeName: String, val name: String, val modelService: KevoreeModelHandlerService, val bootService: Bootstraper, val kevsEngine: KevScriptEngineFactory, val dataSpace: DataSpaceService?, val tg : ThreadGroup) : KInstance {

    fun getKevoreeComponentType(): ComponentType {
        return ct
    }

    private var ct_started: Boolean = false

    fun isStarted(): Boolean {
        return ct_started
    }

    public fun initPorts(modelElement: ComponentInstance) {
        /* Init Required and Provided Port */
        val bean = modelElement.typeDefinition!!.bean!!
        for(providedPort in modelElement.provided){
            val newPortClazz = ct.javaClass.getClassLoader()!!.loadClass(buildPortBean(bean, providedPort.portTypeRef!!.name!!))
            //TODO inject pure reflexif port, if class not found exception
            val newPort = newPortClazz!!.getConstructor(ct.getClass()).newInstance(ct) as KevoreePort
            newPort.startPort(tg)
            ct.getHostedPorts()!!.put(newPort.getName()!!, newPort)
        }
        for(requiredPort in modelElement.required){
            val newPortClazz = ct.javaClass.getClassLoader()!!.loadClass(buildPortBean(bean, requiredPort.portTypeRef!!.name!!))
            //TODO inject pure reflexif port, if class not found exception
            val newPort = newPortClazz!!.getConstructor(ct.getClass()).newInstance(ct) as KevoreePort
            newPort.startPort(tg)
            ct.getNeededPorts()!!.put(newPort.getName()!!, newPort)
        }
        /* End reflexive injection */
    }


    private val resolver = MethodAnnotationResolver(ct.javaClass);

    private val fieldResolver = FieldAnnotationResolver(ct.javaClass);

    private fun buildPortBean(bean: String, portName: String): String {
        val packName = bean.subSequence(0, bean.lastIndexOf("."))
        val clazzName = bean.subSequence(bean.lastIndexOf(".") + 1, bean.length())
        return packName.toString() + ".kevgen." + clazzName + "PORT" + portName
    }

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!ct_started){
            try {
                val injector = KInject(ct, modelService, bootService, kevsEngine, dataSpace)
                injector.kinject()

                ct.setName(name)
                ct.setNodeName(nodeName)

                (ct.getModelService() as ModelHandlerServiceProxy).setTempModel(tmodel)
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Start>())
                met?.invoke(ct)
                (ct.getModelService() as ModelHandlerServiceProxy).unsetTempModel()

                for(hp in ct.getHostedPorts()) {
                    val port = hp.value as KevoreePort
                    if (port.isInPause()) {
                        port.resume()
                    }
                }
                ct_started = true
                return true
            } catch(e: InvocationTargetException){
                Log.error("Kevoree Component Instance Start Error !", e.getCause())
                ct_started = true //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false
            } catch(e: Exception) {
                Log.error("Kevoree Component Instance Start Error !", e)
                ct_started = true //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false
            }
        } else {
            return false
        }
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (ct_started){
            try {
                for(hp in ct.getHostedPorts()) {
                    val port = hp.value as KevoreePort
                    if (!port.isInPause()) {
                        port.pause()
                    }
                }
                (getKevoreeComponentType().getModelService() as ModelHandlerServiceProxy).setTempModel(tmodel)
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Stop>())
                met?.invoke(ct)
                (getKevoreeComponentType().getModelService() as ModelHandlerServiceProxy).unsetTempModel()
                ct_started = false
                return true
            } catch(e: InvocationTargetException){
                Log.error("Kevoree Component Instance Stop Error !", e.getCause())
                return false

            } catch(e: Exception) {
                Log.error("Kevoree Component Instance Stop Error !", e)
                return false
            }
        } else {
            return false
        }
    }

    public override fun kUpdateDictionary(d: Map<String, Any>, cmodel: ContainerRoot): Map<String, Any>? {
        try {
            val previousDictionary = ct.getDictionary()!!.clone()
            for(v in d.keySet()) {
                getKevoreeComponentType().getDictionary()!!.put(v, d.get(v)!!)
            }
            if (ct_started) {
                (getKevoreeComponentType().getModelService() as ModelHandlerServiceProxy).setTempModel(cmodel)
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Update>())
                met?.invoke(ct)
                (getKevoreeComponentType().getModelService() as ModelHandlerServiceProxy).unsetTempModel()
            }
            return previousDictionary as Map<String, Any>?
        } catch(e: InvocationTargetException){
            Log.error("Kevoree Component Instance Update Error !", e.getCause())
            return null
        } catch(e: Exception) {
            Log.error("Kevoree Component Instance Update Error !", e)
            return null
        }
    }

}
