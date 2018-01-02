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

    int numberOfFiles = 0;
    String name = "";
    FileName fn = null;

    public SeriesName(FileName fileName) {
        super();
        fn = fileName;
        updateName();
        setFont( new Font( "Monospaced", Font.BOLD, 14 ) );
        setForeground( Color.blue );
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    public void setNumberOfFiles (int i) {
        numberOfFiles = i;
        updateName();
    }

    public void updateName () {
        name = "[" + fn.getModality() + "]"
                + " " + fn.getSeriesDescription()
                + " " + "(" + String.valueOf(numberOfFiles) + ")"
                + " " + "SERIES"
                + " " + fn.getStudyDate();
        setText(name);
    }
}
