package org.kevoree.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.Toast;
import org.kevoree.android.framework.helper.UIServiceHandler;
import org.kevoree.android.framework.service.KevoreeAndroidService;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.log.Log;

import java.io.InputStream;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 02/09/11
 * Time: 16:23
 */
@Provides({
        @ProvidedPort(name = "on", type = PortType.MESSAGE),
        @ProvidedPort(name = "off", type = PortType.MESSAGE),
        @ProvidedPort(name = "toggle", type = PortType.SERVICE, className = ToggleLightService.class)
})
/*@DictionaryType({
		@DictionaryAttribute(name = "COLOR_ON", defaultValue = "GREEN", optional = true,
				vals = {"GREEN", "RED", "BLUE", "YELLOW"}),
		@DictionaryAttribute(name = "COLOR_OFF", defaultValue = "RED", optional = true,
				vals = {"GREEN", "RED", "BLUE", "YELLOW"})
})*/
@Library(name = "Android")
@ComponentType
public class FakeSimpleLight extends AbstractComponentType implements ToggleLightService {

    private KevoreeAndroidService uiService = null;
    private ImageView view = null;
    private Boolean on;

    @Start
    public void start() {
        uiService = UIServiceHandler.getUIService();
        view = new ImageView(uiService.getRootActivity());
        on = false;
        uiService.addToGroup("kevLight"+getName(), view);
        uiService.getRootActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                applyColor(view, false);
            }
        });
    }

    @Stop
    public void stop() {
        uiService.remove(view);
    }

    @Update
    public void update() {
        stop();
        start();
    }

    private void applyColor(ImageView view, boolean on) {
        Bitmap image = null;
        if (on) {
            try {
                InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("ampAllume.png");
                image = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
				Log.debug("Unable to apply color on light", e);
            }
        } else {
            try {
                InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("ampEteinte.png");
                image = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                Log.debug("Unable to apply color on light", e);
            }
        }
        if (image != null) {
            view.setImageBitmap(image);
        } else {
            Log.debug("Image not found via classloader {}",this.getClass().getClassLoader().toString());
        }
    }

    @Port(name = "on")
    public void lightOn(Object message) {
        uiService.getRootActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                applyColor(view, true);
                Toast.makeText(uiService.getRootActivity(), "Light on!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Port(name = "off")
    public void lightOff(Object message) {
        uiService.getRootActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                applyColor(view, false);
                Toast.makeText(uiService.getRootActivity(), "Light off!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Port(name = "toggle", method = "toggle")
    public String toggle() {
        if (on) {
            this.lightOff("");
            on = !on;
            return "on";
        } else {
            this.lightOn("");
            on = !on;
            return "off";
        }
    }
}