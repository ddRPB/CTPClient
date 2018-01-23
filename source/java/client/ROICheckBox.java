package client;

import javax.swing.*;
import java.awt.*;

public class ROICheckBox extends JCheckBox{

    public ROICheckBox() {
        super();
        setBorder(BorderFactory.createEmptyBorder(0,40,0,0));
        setSelected(false);
        setBackground(Color.white);
        setEnabled(false);
    }
}
