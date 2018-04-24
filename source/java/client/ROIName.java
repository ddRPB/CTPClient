package client;

import javax.swing.*;
import java.awt.*;

class ROIName extends JButton {

    private final String name;

    public ROIName(String roiName) {
        super();
        name = roiName;
        updateName();
        setFont(new Font("Monospaced", Font.BOLD, 14));
        setForeground(Color.blue);
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
    }

    private void updateName() {
        setText(name);
    }

}
