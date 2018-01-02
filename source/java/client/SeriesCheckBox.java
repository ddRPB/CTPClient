/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class SeriesCheckBox extends JCheckBox {

    Series series = null;

    public SeriesCheckBox() {
        super();
        setBorder(BorderFactory.createEmptyBorder(0,30,0,0));
        setSelected(false);
        setBackground(Color.white);
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }
}
