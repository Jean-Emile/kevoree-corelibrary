package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.DeployUnit
import java.util.Random
import org.kevoree.kcl.KevoreeJarClassLoader
import org.kevoree.log.Log

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
/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

class RemoveDeployUnit(val du: DeployUnit, val bootstrap: org.kevoree.api.Bootstraper, val registry: MutableMap<String, Any>) : EndAwareCommand {

    var random = Random()
    var lastKCL: KevoreeJarClassLoader? = null

    override fun undo() {
        if (lastKCL != null) {
            bootstrap.getKevoreeClassLoaderHandler().attachKCL(du, lastKCL)
            registry.put(du.path()!!, lastKCL!!)
        }
    }

    //LET THE UNINSTALL
    override fun execute(): Boolean {
        try {
            lastKCL = bootstrap.getKevoreeClassLoaderHandler().getKevoreeClassLoader(du)
            bootstrap.getKevoreeClassLoaderHandler().removeDeployUnitClassLoader(du)
            registry.remove(du.path())
            return true

        }catch (e: Exception) {
            Log.debug("error ", e)
            return false
        }
    }

    override fun doEnd() {
        lastKCL = null
    }

    fun toString(): String {
        return "RemoveDeployUnit " + du.groupName + "/" + du.name + "/" + du.version + "/" + du.hashcode
    }
}