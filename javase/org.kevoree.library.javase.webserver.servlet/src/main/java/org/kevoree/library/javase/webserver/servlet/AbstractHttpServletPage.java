package org.kevoree.library.javase.webserver.servlet;

import org.kevoree.annotation.ComponentFragment;
import org.kevoree.library.javase.webserver.AbstractPage;
import org.kevoree.library.javase.webserver.KevoreeHttpRequest;
import org.kevoree.library.javase.webserver.KevoreeHttpResponse;
import org.kevoree.log.Log;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 07/12/11
 * Time: 12:50
 */
@ComponentFragment
public abstract class AbstractHttpServletPage extends AbstractPage {

	protected HttpServlet legacyServlet;
	protected ServletConfig config;

	public abstract ServletContext getSharedServletContext ();

	public abstract void initServlet ();

	@Override
	public void startPage () {
		super.startPage();
		try {
			initServlet();
			if (config == null) {
				config = new ServletConfig() {
					@Override
					public String getServletName () {
						return getName();
					}

					@Override
					public ServletContext getServletContext () {
						return getSharedServletContext();
					}

					@Override
					public String getInitParameter (String name) {
						return null;
					}

					@Override
					public Enumeration<String> getInitParameterNames () {
						return Collections.enumeration(new ArrayList<String>(0));
					}
				};
			}

			legacyServlet.init(config);
		} catch (ServletException e) {
			Log.error("Error while starting servlet");
		}
	}

	@Override
	public void stopPage () {
		legacyServlet.destroy();
		super.stopPage();
	}

	@Override
	public KevoreeHttpResponse process (final KevoreeHttpRequest request, final KevoreeHttpResponse response) {

		KevoreeServletRequest wrapper_request = new KevoreeServletRequest(request, getLastParam(request.getUrl()));
		KevoreeServletResponse wrapper_response = new KevoreeServletResponse();
		try {
			// logger.debug("Sending " + new String(request.getRawBody()));
			//logger.debug("Sending " + request.getResolvedParams().keySet().size());
			legacyServlet.service(wrapper_request, wrapper_response);
		} catch (Exception e) {
			Log.error("Error while processing request", e);
		}
		wrapper_response.populateKevoreeResponse(response);
		return response;
	}


}
