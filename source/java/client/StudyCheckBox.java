/*---------------------------------------------------------------
 *  Copyright 2005 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;

public class StudyCheckBox extends JCheckBox {

    private Study study = null;

    public StudyCheckBox() {
        super();
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        setSelected(false);
        setBackground(Color.white);
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }
}
