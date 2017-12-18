/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class SeriesName extends JButton {

    public SeriesName(FileName fileName) {
        super();
        String name = fileName.getPatientName()
                + " " + fileName.getSeriesDescription();
        setText(name);
        setFont( new Font( "Monospaced", Font.BOLD, 14 ) );
        setForeground( Color.blue );
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

}
