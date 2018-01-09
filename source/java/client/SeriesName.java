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

        if(fn.getModality().equals("RTSTRUCT")) {
            name = String.format("%-16s", "[" + fn.getModality() + "]")
                    + " " + String.format("%-50s", fn.getSeriesDescription().substring(0,
                    Math.min(fn.getSeriesDescription().length(), 50)))
                    + " " + String.format("%-11s", "<ROI=NR>")
                    //+ " " + String.format("%-8s", "(" + String.format("%03d", numberOfFiles) + ")")
                    + " " + String.format("%-10s", "SERIES")
                    + " " + String.format("%-2s", "  " + fn.getSeriesDate());
        }
        else {
            name = String.format("%-16s", "[" + fn.getModality() + "]")
                    + " " + String.format("%-53s", fn.getSeriesDescription().substring(0,
                    Math.min(fn.getSeriesDescription().length(), 53)))
                    + " " + String.format("%-8s", "(" + String.format("%03d", numberOfFiles) + ")")
                    + " " + String.format("%-10s", "SERIES")
                    + " " + String.format("%-2s", "  " + fn.getSeriesDate());
        }
        setText(name);
    }
}
