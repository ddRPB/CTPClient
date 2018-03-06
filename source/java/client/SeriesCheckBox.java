/*---------------------------------------------------------------
 *  Copyright 2005 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;

public class SeriesCheckBox extends JCheckBox {

    public SeriesCheckBox() {
        super();
        setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        setSelected(false);
        setBackground(Color.white);
    }
}
