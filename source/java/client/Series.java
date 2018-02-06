/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.rsna.ui.RowLayout;

import javax.swing.*;

public class Series implements ActionListener, Comparable<Series> {

    LinkedList<FileName> list = null;
    LinkedList<ROI> roiList = null;
    SeriesCheckBox cb = null;
    SeriesName seriesName = null;
    String patientName = null;
    boolean showDicomFiles = false;
    FileName fn = null;
    boolean roisVisible = true;
    String frameOfRef = null;

    public Series(FileName fileName) {
        fn = fileName;
        list = new LinkedList<FileName>();
        roiList = new LinkedList<ROI>();
        cb = new SeriesCheckBox();
        cb.addActionListener(this);
        patientName = fileName.getPatientName();
        seriesName = new SeriesName(fileName);
        seriesName.addActionListener(this);
        frameOfRef = fn.getFrameOfReference();
        add(fileName);
    }

    public void add(FileName fileName) {
        list.add(fileName);
        fileName.getCheckBox().addActionListener(this);
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
        else if (source.equals(seriesName)) {
            if (seriesName.getModality().equals("RTSTRUCT") && roisVisible == true) {
                hideROIs(false);
            }
            else if (seriesName.getModality().equals("RTSTRUCT") && roisVisible == false) {
                hideROIs(true);
            }
            else {
                cb.doClick();
            }
        }
    }

    public void hideROIs(boolean hide) {
        DirectoryPanel dp = getDirectoryPanel();
        for(ROI r : roiList) {
            if (dp != null) dp.setRowVisible(r.getCheckBox(), hide);
            roisVisible = hide;
        }
    }

    public int compareTo(Series series) {
        if (series == null)  return 0;
        int c;
        if ( (c = this.patientName.compareTo(series.getPatientName())) != 0 ) return c;
        return 0;
    }

    public String getSOPClassUID() {
        return fn.getSOPClassUID();
    }

    public SeriesCheckBox getCb() { return cb; }

    public String getPatientName() { return patientName; }

    public String getFrameOfRef() { return frameOfRef; }

    public boolean isSelected() { return cb.isSelected(); }

    public void showDicomFiles(boolean s) {
        showDicomFiles = s;
    }

    public void setSelected(boolean selected) {
        cb.setSelected(selected);
        if(selected) selectAll();
        else deselectAll();
    }

    public void selectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(true);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), true);
        }
        for(ROI r : roiList) {

            r.setSelected(true);
            if (dp != null) dp.setRowVisible(r.getCheckBox(), true);
            roisVisible = true;
        }
    }

    public void deselectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(false);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), false);
        }
        for(ROI r : roiList) {

            r.setSelected(false);
            if (dp != null) dp.setRowVisible(r.getCheckBox(), false);
            roisVisible = false;
        }
    }

    private DirectoryPanel getDirectoryPanel() {
        Component container = cb.getParent();
        if ((container != null) && (container instanceof DirectoryPanel)) {
            return (DirectoryPanel)container;
        }
        return null;
    }

    public SeriesName getSeriesName() { return seriesName; }

    public String getSOPInstanceUID() { return fn.getSOPInstanceUID(); }

    public String getRefSOPClassUID() { return fn.getRefSOPClassUID(); }

    public String getRefStructSOPInst() { return fn.getRefStructSOPInst(); }

    public String getRefDoseSOPInst() { return fn.getRefDoseSOPInst(); }

    public FileName[] getFileNames() {
        FileName[] names = new FileName[list.size()];
        names = list.toArray(names);
        Arrays.sort(names);
        return names;
    }

    public void generateROIs() {
        int i = 0;
        String ObsLabel = "", RoiType = "";
        for(String name : fn.getROINameList()) {

            for (String RefRoiNumber : fn.getRefROINumberList()) {
                if (RefRoiNumber.equals(fn.getROINumberList().get(i))) {
                    ObsLabel = fn.getROIObservationLabelList().get(i);
                    RoiType = fn.getROIInterpretedTypeList().get(i);
                    break;
                }
            }

            roiList.add(new ROI(name, fn.getROINumberList().get(i), ObsLabel, RoiType));
            i++;
        }
    }

    public String getROIRefFrame(){
        return list.getFirst().getROIRefFrameOfRef();
    }

    public void display(DirectoryPanel dp) {
        dp.add(cb);
        seriesName.setNumberOfFiles(list.size());
        dp.add(seriesName, RowLayout.span(4));
        dp.add(RowLayout.crlf());

        if(seriesName.getModality().equals("RTSTRUCT")) {

            if (list.getFirst().frameOfRefsOK()
                    && list.getFirst().hasROIonlyOneRefFrameOfRefUID()) {
                generateROIs();
                for (ROI r : roiList) {
                    r.display(dp);
                }
            }
            else {
                dp.add(new JLabel("FRAME OF REF!!!"));
                dp.add(RowLayout.crlf());
            }



        }

        if(showDicomFiles) {
            for (FileName fn : getFileNames()) {
                fn.display(dp);
            }
        }
    }
}