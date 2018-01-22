package client;

import org.rsna.ui.RowLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

public class ROI implements ActionListener, Comparable<ROI>{

    ROICheckBox cb = null;
    ROIName rname = null;
    String patientName = null;
    LinkedList<FileName> list = null;
    FileName fn = null;

    public ROI(String name, String numberAsString, String ObsLabel, String RoiType){
        cb = new ROICheckBox();
        list = new LinkedList<FileName>();

        String description = numberAsString
                + ": " + name
                + " (" + ObsLabel + " - " + RoiType + ")";

        rname = new ROIName("[ROI] "
                + String.format("%-74s", description.substring(0, Math.min(description.length(), 74)))
                + " " + String.format("%-10s", "ROI"));
        rname.addActionListener(this);
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source.equals(cb)) {
            if (cb.isSelected()) selectAll();
            else {
                deselectAll();
                DirectoryPanel dp = getDirectoryPanel();
                if (dp != null) {
                    boolean ctrl = (event.getModifiers() & ActionEvent.CTRL_MASK) != 0;
                    if (ctrl) dp.setRowVisible(cb, false);
                }
            }
        }
        else if (source.equals(rname)) {
            cb.doClick();
        }
    }

    public ROICheckBox getCheckBox() {
        return cb;
    }

    public int compareTo(ROI roi) {
        if (roi == null)  return 0;
        int c;
        if ( (c = this.patientName.compareTo(roi.getPatientName())) != 0 ) return c;
        return 0;
    }

    public String getPatientName() {
        return patientName;
    }

    public void selectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(true);
            if (dp != null){
                dp.setRowVisible(fn.getCheckBox(), true);
                dp.setRowVisible(cb, true);
            }

        }
    }

    public  void setSelected(boolean selected) {
        cb.setSelected(selected);
        if(selected) {
            selectAll();
        }
        else{
            deselectAll();
        }
    }

    public void deselectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(false);
            if (dp != null){
                dp.setRowVisible(fn.getCheckBox(), false);
                dp.setRowVisible(cb, false);
            }
        }
    }

    private DirectoryPanel getDirectoryPanel() {
        Component container = cb.getParent();
        if ((container != null) && (container instanceof DirectoryPanel)) {
            return (DirectoryPanel)container;
        }
        return null;
    }

    public void display(DirectoryPanel dp) {

        dp.add(cb);
        dp.add(rname, RowLayout.span(4));
        dp.add(RowLayout.crlf());
/*        int i = 0;
        for (String s : fn.getROINameList()) {
            dp.add(new ROICheckBox());
            dp.add(new ROIName("[ROI] " + fn.getROINumberList().get(i) + ": " + s),
                    RowLayout.span(4));
            dp.add(RowLayout.crlf());
            i++;
        }*/
    }


}
