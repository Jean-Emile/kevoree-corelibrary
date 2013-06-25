package org.kevoree.library.javase.webserver.sample;

import org.kevoree.annotation.ComponentType;
import org.kevoree.library.javase.webserver.AbstractPage;
import org.kevoree.library.javase.webserver.KevoreeHttpRequest;
import org.kevoree.library.javase.webserver.KevoreeHttpResponse;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 17/10/11
 * Time: 09:29
 * To change this template use File | Settings | File Templates.
 */
@ComponentType
public class SampleFormPage extends AbstractPage {

    @Override
    public KevoreeHttpResponse process(KevoreeHttpRequest request,KevoreeHttpResponse response){
        response.setContent(generateFormHtml(/*request.getUrl()*/));
        return response;
    }


    private String generateFormHtml() {
        return "<html><body><form action={\"/sample\"} method=\"POST\"><input type=\"text\" name=\"name\"/><input type=\"text\" name=\"forname\"/><input type=\"submit\" value=\"Send\"/></form></body></html>";
    }
}