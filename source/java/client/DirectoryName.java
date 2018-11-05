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

class DirectoryName extends JLabel {

    private final File dir;

    public DirectoryName(File dir) {
        super(dir.getAbsolutePath());
        this.dir = dir;
        setFont(new Font("Monospaced", Font.BOLD, 12));
        setForeground(Color.black);
    }

    public File getDirectory() {
        return dir;
    }
}

