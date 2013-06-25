package org.kevoree.library.sky.provider.tester;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.KevoreeFactory;
import org.kevoree.annotation.*;
import org.kevoree.api.service.core.script.KevScriptEngine;
import org.kevoree.api.service.core.script.KevScriptEngineException;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.KevoreePropertyHelper;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.sky.provider.api.IaaSManagerService;
import org.kevoree.library.sky.provider.api.SubmissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 03/11/12
 * Time: 18:22
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@Library(name = "Test")
@ComponentType
@DictionaryType({
        @DictionaryAttribute(name = "nbNodes", defaultValue = "1", optional = true)
})
@Requires({
        @RequiredPort(name = "delegate", type = PortType.SERVICE, className = IaaSManagerService.class, optional = true)
})
public class IaaSKloudManagerTester extends AbstractComponentType implements IaaSManagerService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private KevoreeFactory factory = new DefaultKevoreeFactory();

    private ContainerRoot model;

    @Start
    public void start() throws KevScriptEngineException {
        model = null;
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                generateModel("", factory.createContainerRoot());
                if (model != null) {
                    boolean check = true;
                    check = add(model, check);
                    logger.info("{}.add is OK: {}", getName(), check);
                    /*check = remove(model, check);
                    logger.info("{}.remove is OK: {}", getName(), check);
                    check = addToNode(model, check);
                    logger.info("{}.addToNode is OK: {}", getName(), check);
                    check = remove(model, check);
                    logger.info("{}.remove is OK: {}", getName(), check);*/


                        /*generateModel("merge", KevoreeFactory.createContainerRoot());
                        if (model != null) {
                            merge(model);
                            // TODO check if previous nodes have been removed and the new ones have been added
                            remove(model);
                            // TODO check all nodes are unavailable
                        }
                        generateModel("", KevoreeFactory.createContainerRoot());
                        if (model != null) {
                            addToNode(model, getNodeName());
                            generateModel("merge", KevoreeFactory.createContainerRoot());
                            if (model != null) {
                                mergeToNode(model, getNodeName());
                                // TODO check all previous nodes available and the new ones have been added
                                remove(model);
                                // TODO check al nodes are unavailable
                            }
                        }
                        generateModel("", KevoreeFactory.createContainerRoot());
                        if (model != null) {
                            add(model);
                            generateModel("merge", KevoreeFactory.createContainerRoot());
                            mergeToNode(model, getNodeName());
                            // TODO check all previous nodes available on local have been removed and the new ones have been added
                            remove(model);
                            // TODO check al nodes are unavailable
                        }*/
                }
            }
        }.start();
    }

    @Stop
    public void stop() {
    }

    private void generateModel(String prefix, ContainerRoot previousModel) {
        KevScriptEngine kengine = getKevScriptEngineFactory().createKevScriptEngine(previousModel);

        kengine.append("merge 'mvn:org.kevoree.corelibrary.sky/org.kevoree.library.sky.api/1.9.0-SNAPSHOT'");

        ContainerNode currentNode = getModelService().getLastModel().findByPath("nodes[" + getNodeName() + "]", ContainerNode.class);
        kengine.addVariable("logLevel", prefix + KevoreePropertyHelper.instance$.getProperty(currentNode, "logLevel", false, null));

        int nbNode = Integer.parseInt(getDictionary().get("nbNodes").toString());
        for (int i = 0; i < nbNode; i++) {
            kengine.addVariable("childName", prefix + "ChildNode" + i);
            kengine.append("addNode {childName} : PJavaSENode");
            kengine.append("updateDictionary {childName} {logLevel = '{logLevel}'}");
        }
        try {
            model = kengine.interpret();
        } catch (KevScriptEngineException e) {
            e.printStackTrace();
        }

    }

    private boolean add(ContainerRoot model, boolean check) {
        if (check) {
            try {
                add(model);
                // check all nodes are available
                ContainerRoot currentModel = getModelService().getLastModel();
                for (ContainerNode node : model.getNodes()) {
                    ContainerNode addedNode = currentModel.findByPath(node.path(), ContainerNode.class);
                    if (addedNode == null) {
                        logger.error("Node {} has not been added on the current model", node.getName());
                        check = false;
                    }
                }
            } catch (SubmissionException e) {
                e.printStackTrace();
                check = false;
            }
        }
        return check;
    }

    @Override
    public void add(ContainerRoot model) throws SubmissionException {
        if (isPortBinded("delegate")) {
            logger.debug("call delegate port with method add");
            getPortByName("delegate", IaaSManagerService.class).add(model);
        }
    }

    private boolean addToNode(ContainerRoot model, boolean check) {
        if (check) {
            try {
                addToNode(model, getNodeName());
                // check all nodes are available on the local host node
                ContainerRoot currentModel = getModelService().getLastModel();
                for (ContainerNode node : model.getNodes()) {
                    ContainerNode addedNode = currentModel.findByPath(node.path(), ContainerNode.class);
                    if (addedNode == null || (addedNode.getHost() != null && addedNode.getHost().getName().equals(getNodeName()))) {
                        logger.error("Node {} has not been added on the current model or on the local node {}", node.getName(), getNodeName());
                        check = false;
                    }
                }
            } catch (SubmissionException e) {
                e.printStackTrace();
                check = false;
            }
        }
        return check;
    }

    @Override
    public void addToNode(ContainerRoot model, String nodeName) throws SubmissionException {
        if (isPortBinded("delegate")) {
            logger.debug("call delegate port with method addToNode");
            getPortByName("delegate", IaaSManagerService.class).addToNode(model, nodeName);
        }
    }

    private boolean remove(ContainerRoot model, boolean check) {
        if (check) {
            try {
                remove(model);
                // check all nodes are unavailable
                ContainerRoot currentModel = getModelService().getLastModel();
                for (ContainerNode node : model.getNodes()) {
                    ContainerNode removedNode = currentModel.findByPath(node.path(), ContainerNode.class);
                    if (removedNode != null) {
                        logger.error("Node {} has not been removed on the current model", node.getName());
                        check = false;
                    }
                }
            } catch (SubmissionException e) {
                e.printStackTrace();
                check = false;
            }
        }
        return check;
    }

    @Override
    public void remove(ContainerRoot model) throws SubmissionException {
        if (isPortBinded("delegate")) {
            logger.debug("call delegate port with method remove");
            getPortByName("delegate", IaaSManagerService.class).remove(model);
        }
    }

    @Override
    public void merge(ContainerRoot model) throws SubmissionException {
        if (isPortBinded("delegate")) {
            logger.debug("call delegate port with method merge");
            getPortByName("delegate", IaaSManagerService.class).merge(model);
        }
    }

    @Override
    public void mergeToNode(ContainerRoot model, String nodeName) throws SubmissionException {
        if (isPortBinded("delegate")) {
            logger.debug("call delegate port with method mergeToNode");
            getPortByName("delegate", IaaSManagerService.class).mergeToNode(model, nodeName);
        }
    }
}
