package org.kevoree.library;

import jexxus.common.Connection;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.Start;
import org.kevoree.api.service.core.handler.UUIDModel;
import org.kevoree.cloner.ModelCloner;
import org.kevoree.framework.KevoreePropertyHelper;
import org.kevoree.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/12
 * Time: 12:10
 */

@GroupType
public class AutoBasicGroup extends BasicGroup {

    private final byte discoveryRequest = 8;
    private final byte discoveryResponse = 9;

    @Override
    protected void externalProcess(byte[] data, Connection from) {
        switch (data[0]) {
            case discoveryRequest: {
                //BROADCAST MY INFO
                try {
                    broadCastMyInfo();
                } catch (IOException e) {
                    Log.error("Error while broadcasting node informations ", e);
                }
            }
            break;
            case discoveryResponse: {
                //BROADCAST MY INFO
                processRemoteInfo(data, from.getIP());
            }
            break;
            default: {
                from.close();
            }
            break;

        }
    }

    protected void broadCastMyInfo() throws IOException {
        String infos = getName() + ";" + getNodeName() + ";AutoBasicGroup;" + port;
        ByteArrayOutputStream outs = new ByteArrayOutputStream();
        outs.write(discoveryResponse);
        outs.write(infos.getBytes());
        outs.flush();
        BroadCastSender.send(port, outs.toByteArray());
        outs.close();
    }

    private static ModelCloner cloner = new ModelCloner();
   // private static KevoreeMergerComponent merger = new KevoreeMergerComponent();


    protected void processRemoteInfo(byte[] rawData, final String ip) {
        final String[] data = new String(rawData, 1, rawData.length - 1).split(";");
        if (data.length == 4 && getName().equals(data[0]) && "AutoBasicGroup".equals(data[2]) && !getName().equals(data[1])) {
            pool.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            UUIDModel model = getModelService().getLastUUIDModel();
                            if (!KevoreePropertyHelper.instance$.getNetworkProperties(model.getModel(), data[1], org.kevoree.framework.Constants.instance$.getKEVOREE_PLATFORM_REMOTE_NODE_IP()).contains(ip)) {
                                Log.info("New IP found for node " + data[1] + "->" + ip);
                                try {
                                    ContainerRoot modelRW = cloner.clone(model.getModel());
                                   // ContainerRoot newModel = merger.merge(modelRW, requestModel(ip, Integer.parseInt(data[3]), data[0]));
                                  //  getModelService().compareAndSwapModel(model, newModel);
                                } catch (Exception e) {
                                    Log.error("Error while merging remote model");
                                }
                            } else {
                                Log.info("Already ok " + data[1] + "->" + ip);
                            }
                        }
                    });
        }
    }

    private ExecutorService pool = null;

    @Start
    public void startRestGroup() throws IOException {
        pool = Executors.newSingleThreadExecutor();
        byte[] request = new byte[1];
        request[0] = discoveryRequest;
        udp = true;
        super.startRestGroup();
        BroadCastSender.send(port, request);
    }

    @Override
    public void stopRestGroup() {
        pool.shutdownNow();
        super.stopRestGroup();
    }
}
