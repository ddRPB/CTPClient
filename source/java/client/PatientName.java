/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;

public class PatientName extends JButton{

    String name = null;

    public PatientName(String patientName){
        super();
        name = patientName;
        setText(name);
        setFont( new Font( "Monospaced", Font.BOLD, 18 ) );
        setForeground( Color.blue );
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    public String getPatientName() {
        return name;
    }
}
