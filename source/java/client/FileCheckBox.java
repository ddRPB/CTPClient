/*---------------------------------------------------------------
 *  Copyright 2005 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;

public class FileCheckBox extends JCheckBox {

    FileName fileName;
    StatusText statusText;

    public FileCheckBox() {
        super();
        setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 0));
        setSelected(false);
        setBackground(Color.white);
        setAlignmentY(0.0f);
    }
}
