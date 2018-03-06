/*---------------------------------------------------------------
 *  Copyright 2005 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;

public class SeriesName extends JButton {

    private int numberOfFiles = 0;
    private final FileName fn;
    private final String modality;
    private String seriesDescription;

    public SeriesName(FileName fileName) {
        super();
        fn = fileName;
        seriesDescription = fn.getSeriesDescription();
        modality = fn.getModality();
        updateName();
        setFont(new Font("Monospaced", Font.BOLD, 14));
        setForeground(Color.blue);
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    public void setNumberOfFiles(int i) {
        numberOfFiles = i;
        updateName();
    }

    public void changeDisplaySeriesDescription(String nsd) {
        seriesDescription = nsd;
        updateName();
    }

    public String getSeriesInstanceUID() {
        return fn.getSeriesInstanceUID();
    }

    public String getSeriesDescription() {
        return fn.getSeriesDescription();
    }

    public String getModality() {
        return modality;
    }

    private void updateName() {

        String name;
        if (modality.equals("RTSTRUCT")) {
            name = String.format("%-16s", "[" + modality + "]")
                    + " " + String.format("%-50s", seriesDescription.substring(0,
                    Math.min(seriesDescription.length(), 50)))
                    + " " + String.format("%-11s", "<ROI=" + fn.getROINumberList().size() + ">")
                    + " " + String.format("%-10s", "SERIES")
                    + " " + String.format("%-2s", "  " + fn.getSeriesDate());
        } else {
            name = String.format("%-16s", "[" + modality + "]")
                    + " " + String.format("%-53s", seriesDescription.substring(0,
                    Math.min(seriesDescription.length(), 53)))
                    + " " + String.format("%-8s", "(" + String.format("%03d", numberOfFiles) + ")")
                    + " " + String.format("%-10s", "SERIES")
                    + " " + String.format("%-2s", "  " + fn.getSeriesDate());
        }
        //name += "    " + fn.getFrameOfReference();
        setText(name);
    }
}
