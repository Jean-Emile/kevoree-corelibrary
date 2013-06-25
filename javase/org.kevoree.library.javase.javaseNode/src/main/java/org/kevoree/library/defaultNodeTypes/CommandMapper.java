package org.kevoree.library.defaultNodeTypes;

import org.kevoree.Channel;
import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.MBinding;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.framework.AbstractNodeType;
import org.kevoree.kompare.JavaSePrimitive;
import org.kevoree.library.defaultNodeTypes.command.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 1/29/13
 * Time: 11:22 AM
 */
public class CommandMapper {

    AbstractNodeType nodeType = null;
    java.util.ArrayList<EndAwareCommand> toClean = new java.util.ArrayList<EndAwareCommand>();

    public void setNodeType(AbstractNodeType n) {
        nodeType = n;
    }

    public void doEnd() {
        for (EndAwareCommand cmd : toClean) {
            cmd.doEnd();
        }
        toClean.clear();
    }

    public PrimitiveCommand buildPrimitiveCommand(org.kevoreeadaptation.AdaptationPrimitive p, String nodeName) {

        String pTypeName = p.getPrimitiveType().getName();
        if (pTypeName.equals(JavaSePrimitive.instance$.getUpdateDictionaryInstance())) {
            if (((Instance) p.getRef()).getName().equals(nodeName)) {
                return new SelfDictionaryUpdate((Instance) p.getRef(), nodeType);
            } else {
                return new UpdateDictionary((Instance) p.getRef(), nodeName);
            }

        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getAddFragmentBinding())) {
            return new AddFragmentBindingCommand((Channel) p.getRef(), p.getTargetNodeName(), nodeName);
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getRemoveFragmentBinding())) {
            return new RemoveFragmentBindingCommand((Channel) p.getRef(), p.getTargetNodeName(), nodeName);
        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getStartInstance())) {
            return new StartStopInstance((Instance) p.getRef(), nodeName, true);
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getStopInstance())) {
            return new StartStopInstance((Instance) p.getRef(), nodeName, false);
        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getAddBinding())) {
            return new AddBindingCommand((MBinding) p.getRef(), nodeName);
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getRemoveBinding())) {
            return new RemoveBindingCommand((MBinding) p.getRef(), nodeName);
        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getAddDeployUnit())) {
            return new AddDeployUnit((DeployUnit) p.getRef(), nodeType.getBootStrapperService());
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getRemoveDeployUnit())) {
            RemoveDeployUnit res = new RemoveDeployUnit((DeployUnit) p.getRef(), nodeType.getBootStrapperService());
            toClean.add(res);
            return res;
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getUpdateDeployUnit())) {
            UpdateDeployUnit res = new UpdateDeployUnit((DeployUnit) p.getRef(), nodeType.getBootStrapperService());
            toClean.add(res);
            return res;
        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getAddInstance())) {
            return new AddInstance((Instance) p.getRef(), nodeName, nodeType.getModelService(), nodeType.getKevScriptEngineFactory(), nodeType.getBootStrapperService(),nodeType);
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getRemoveInstance())) {
            return new RemoveInstance((Instance) p.getRef(), nodeName, nodeType.getModelService(), nodeType.getKevScriptEngineFactory(), nodeType.getBootStrapperService(),nodeType);
        }

        if (pTypeName.equals(JavaSePrimitive.instance$.getAddThirdParty())) {
            return new AddDeployUnit((DeployUnit) p.getRef(), nodeType.getBootStrapperService());
        }
        if (pTypeName.equals(JavaSePrimitive.instance$.getRemoveThirdParty())) {
            return new RemoveDeployUnit((DeployUnit) p.getRef(), nodeType.getBootStrapperService());
        }

        //LoggerFactory.getLogger(this.getClass()).error("Return Noop command for "+pTypeName);

        return new NoopCommand();
    }


}
