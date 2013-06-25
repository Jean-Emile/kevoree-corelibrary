package org.kevoree.library.javase.autoBasicGossiper;

import org.kevoree.annotation.DictionaryAttribute;
import org.kevoree.annotation.DictionaryType;
import org.kevoree.annotation.GroupType;
import org.kevoree.library.javase.basicGossiper.group.BasicGossiperGroup;
import org.kevoree.library.javase.jmdns.JmDNSComponent;
import org.kevoree.library.javase.jmdns.JmDNSListener;
import org.kevoree.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 13/02/13
 * Time: 11:48
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@GroupType
@DictionaryType(
        @DictionaryAttribute(name = "ipv4Only", vals = {"true", "false"}, defaultValue = "true")
)
public class AutoBasicGossiperGroup extends BasicGossiperGroup implements JmDNSListener {

    private JmDNSComponent jmDnsComponent;

    @Override
    public void startGossiperGroup() throws IOException {
        super.startGossiperGroup();
        jmDnsComponent = new JmDNSComponent(this, this, this.getDictionary().get("ip").toString(), Integer.parseInt(this.getDictionary().get("port").toString()), getDictionary().get("ipv4Only").toString().equalsIgnoreCase("true"));
        jmDnsComponent.start();
    }

    @Override
    public void stopGossiperGroup() {
        super.stopGossiperGroup();
        jmDnsComponent.stop();
    }

    @Override
    public void notifyNewSubNode(String remoteNodeName) {
        Log.debug("new remote node discovered, try to pull the model from {}", remoteNodeName);
//        super.actor.doGossip(remoteNodeName);
        List<String> nodeNames = new ArrayList<String>(1);
        nodeNames.add(remoteNodeName);
        super.notifyPeersInternal(nodeNames);
    }
}
