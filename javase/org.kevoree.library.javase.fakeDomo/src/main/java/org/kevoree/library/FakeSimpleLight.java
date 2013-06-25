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
package org.kevoree.library;

import org.kevoree.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author ffouquet
 */
@Provides({
        @ProvidedPort(name = "on", type = PortType.MESSAGE, theadStrategy = ThreadStrategy.SHARED_THREAD),
        @ProvidedPort(name = "off", type = PortType.MESSAGE),
        @ProvidedPort(name = "toggle", type = PortType.SERVICE, className = ToggleLightService.class)
})
@ComponentType
public class FakeSimpleLight extends AbstractFakeStuffComponent {

    private static final Logger logger = LoggerFactory.getLogger(FakeSimpleLight.class);
    private static final int FRAME_WIDTH = 300;
    private static final int FRAME_HEIGHT = 300;
    private MyFrame frame;
    private Boolean state = false;

    @Port(name = "toggle", method = "toggle")
    public String toogle() throws Exception {
        if (state) {
            this.lightOff("");
        } else {
            this.lightOn("");
        }
        return state.toString();
    }

    @Override
    public void start() throws Exception {

//        logger = Logger.getLogger(this.getClass().getName());

        frame = new MyFrame(Color.RED);
        frame.setVisible(true);
        state = false;

        logger.debug("Hello from " + this.getName() + " ;-)");

    }

    @Override
    public void stop() {
        frame.dispose();
        frame = null;
    }

    @Update
    public void update() {
        for (String s : this.getDictionary().keySet()) {
            logger.debug("Dic => " + s + " - " + this.getDictionary().get(s));
        }
    }

    @Ports({
            @Port(name = "on")
    })
    public void lightOn(Object o) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setColor(Color.green);
                //frame.revalidate();
                state = true;
            }
        });
    }

    @Ports({
            @Port(name = "off")
    })
    public void lightOff(Object o) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setColor(Color.red);
                //frame.revalidate();
                state = false;
            }
        });
    }

    private static class MyFrame extends JFrame {

        private Color c;

        public MyFrame(Color c) {
            super("Couleur " + c.toString());
            this.c = c;
            setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            pack();
        }

        public void paint(Graphics g) {
            if (g instanceof Graphics2D) {
                Graphics2D g2d = Graphics2D.class.cast(g);
                g2d.setColor(c);
                g2d.fillOval(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
            } else {
                logger.debug("Graphics are not 2D. Instance of:" + g.getClass());
            }
        }

        public final void setColor(Color c) {
            this.c = c;
            repaint();
            logger.debug("SetColor " + c.toString());
        }
    }
}
