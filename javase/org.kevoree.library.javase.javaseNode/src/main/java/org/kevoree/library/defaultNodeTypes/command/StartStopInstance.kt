package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.ContainerRoot
import org.kevoree.Instance
import org.kevoree.framework.KInstance
import org.kevoree.log.Log
import org.kevoree.library.defaultNodeTypes.wrapper.KevoreeComponent
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelTypeFragmentThread
import org.kevoree.library.defaultNodeTypes.wrapper.KevoreeGroup
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper

/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class StartStopInstance(c: Instance, nodeName: String, val start: Boolean, val registry: MutableMap<String, Any>) : LifeCycleCommand(c, nodeName), Runnable {

    var t: Thread? = null
    var resultAsync = false
    var root: ContainerRoot? = null
    var iact: KInstanceWrapper? = null

    public override fun run() {

        Thread.currentThread().setContextClassLoader(iact.javaClass.getClassLoader())
        if(start){
            Thread.currentThread().setName("KevoreeStartInstance" + c.name!!)
            resultAsync = iact!!.kInstanceStart(root!!)
        } else {
            Thread.currentThread().setName("KevoreeStopInstance" + c.name!!)
            val res = iact!!.kInstanceStop(root!!)
            Thread.currentThread().setContextClassLoader(null)
            resultAsync = res
        }
    }

    override fun undo() {
        StartStopInstance(c, nodeName, !start, registry).execute()
    }

    override fun execute(): Boolean {

        //Look thread group
        root = c.typeDefinition!!.eContainer() as ContainerRoot
        val ref = registry.get(c.path()!!)
        if(ref != null && ref is KInstanceWrapper){
            iact = ref as KInstanceWrapper

            t = Thread(iact!!.tg, this)
            t!!.start()
            t!!.join()
            if(!start){
                //kill subthread
                val subThread: Array<Thread> = Array<Thread>(iact!!.tg.activeCount(), { i -> Thread.currentThread() })
                iact!!.tg.enumerate(subThread)
                for(subT in subThread){
                    try {
                        subT.stop()
                    } catch(t: Throwable){
                        //ignore
                    }
                }
            }

            //call sub
            return resultAsync
        } else {
            Log.error("issue while searching TG to start (or stop) thread")
            return false
        }
    }

    override fun toString(): String {
        var s = "StartStopInstance ${c.name}"
        if (start) {
            s += " start"
        } else {
            s += " stop"
        }
        return s
    }

}
