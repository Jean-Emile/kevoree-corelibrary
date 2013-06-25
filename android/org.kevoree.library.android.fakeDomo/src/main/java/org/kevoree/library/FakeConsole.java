package org.kevoree.library;

import android.graphics.Color;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.kevoree.android.framework.helper.UIServiceHandler;
import org.kevoree.android.framework.service.KevoreeAndroidService;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.KevoreeMessage;
import org.kevoree.framework.MessagePort;


/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 09/11/11
 * Time: 14:58
 */


@Provides({
        @ProvidedPort(name = "showText", type = PortType.MESSAGE)
})
@Requires({
        @RequiredPort(name = "textEntered", type = PortType.MESSAGE, optional = true)
})
@DictionaryType({
        @DictionaryAttribute(name = "singleFrame", defaultValue = "true", optional = true)
})
@ComponentType
@Library(name = "Android")
public class FakeConsole extends AbstractComponentType {
    KevoreeAndroidService uiService = null;
    Object bundle;
    LinearLayout layout;
    ImageView kevoreeimg = null;
    TextView textview=null;
    EditText texteditor=null;
    Button button = null;


    @Start
    public void start() {
        uiService = UIServiceHandler.getUIService();
        button = new Button(uiService.getRootActivity());
        button.setText("Send");
        button.setWidth(300);



        layout = new LinearLayout(uiService.getRootActivity());
        layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,AbsListView.LayoutParams.FILL_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        textview = new TextView(uiService.getRootActivity());
        textview.setBackgroundColor(Color.WHITE);
        textview.setHeight(200);
        textview.setWidth(500);
        textview.setMovementMethod(new ScrollingMovementMethod());
        textview.setText(" ");
        texteditor = new EditText(uiService.getRootActivity());
        texteditor.setWidth(200);

        layout.addView(textview);
        layout.addView(texteditor);
        layout.addView(button);

        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPortBinded("textEntered")) {
                    getPortByName("textEntered", MessagePort.class).process(texteditor.getText().toString()+"\n");
                    Log.i("Sending", texteditor.getText().toString());
                }
            }
        });

        uiService.addToGroup("Console"+getName(), layout);
    }


    @Stop
    public void stop() {
        uiService.remove(layout);
    }


    @Port(name = "showText")
    public void appendIncoming(final Object text)
    {

        uiService.getRootActivity().runOnUiThread(new Runnable() {
            @Override
            public void run ()
            {
                if (text != null) {
                    textview.append("\n");
                    if (text instanceof KevoreeMessage) {
                        KevoreeMessage kmsg = (KevoreeMessage) text;
                        textview.append("->");
                        for(String key : kmsg.getKeys()){
                            textview.append(key+"="+kmsg.getValue(key));
                        }
                    } else {
                        textview.append("->"+text.toString());
                    }
                     int scrollAmount=0;
                      if(textview.getText().length() > 0 & textview !=null){
                          if(textview.getLayout() !=null){
                              scrollAmount = textview.getLayout().getLineTop(textview.getLineCount()) - textview.getHeight();
                          }
                      }

                    if (scrollAmount > 0)
                        textview.scrollTo(0, scrollAmount);
                    else
                        textview.scrollTo(0, 0);
                }

            }
        });

    }
}