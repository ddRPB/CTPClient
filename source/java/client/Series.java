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

    private final LinkedList<FileName> list;
    private final LinkedList<ROI> roiList;
    private final SeriesCheckBox cb;
    private final SeriesName seriesName;
    private final String patientName;
    private final FileName fn;
    private final String frameOfRef;
    private boolean showDicomFiles = false;
    private boolean roisVisible = true;

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
        } else if (source.equals(seriesName)) {
            if (seriesName.getModality().equals("RTSTRUCT") && roisVisible) {
                hideROIs(false);
            } else if (seriesName.getModality().equals("RTSTRUCT") && !roisVisible) {
                hideROIs(true);
            } else {
                cb.doClick();
            }
        }
    }

    public void hideROIs(boolean hide) {
        DirectoryPanel dp = getDirectoryPanel();
        for (ROI r : roiList) {
            if (dp != null) dp.setRowVisible(r.getCheckBox(), hide);
            roisVisible = hide;
        }
    }

    public int compareTo(Series series) {
        if (series == null) return 0;
        int c;
        if ((c = this.patientName.compareTo(series.getPatientName())) != 0) return c;
        return 0;
    }

    public String getSOPClassUID() {
        return fn.getSOPClassUID();
    }

    public SeriesCheckBox getCb() {
        return cb;
    }

    private String getPatientName() {
        return patientName;
    }

    public String getFrameOfRef() {
        return frameOfRef;
    }

    public String getSeriesInstanceUID() {
        return seriesName.getSeriesInstanceUID();
    }

    public String getDisplayedSeriesDescription() {
        return seriesName.getDisplayedSeriesDescription();
    }

    public String getSeriesDescription() {
        return seriesName.getSeriesDescription();
    }

    public boolean isSelected() {
        return cb.isSelected();
    }

    public void setSelected(boolean selected) {
        cb.setSelected(selected);
        if (selected) selectAll();
        else deselectAll();
    }

    public void showDicomFiles(boolean s) {
        showDicomFiles = s;
    }

    private void selectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(true);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), true);
        }
        for (ROI r : roiList) {

            r.setSelected(true);
            if (dp != null) dp.setRowVisible(r.getCheckBox(), true);
            roisVisible = true;
        }
    }

    private void deselectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(false);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), false);
        }
        for (ROI r : roiList) {

            r.setSelected(false);
            if (dp != null) dp.setRowVisible(r.getCheckBox(), false);
            roisVisible = false;
        }
    }

    private DirectoryPanel getDirectoryPanel() {
        Component container = cb.getParent();
        if ((container != null) && (container instanceof DirectoryPanel)) {
            return (DirectoryPanel) container;
        }
        return null;
    }

    public SeriesName getSeriesName() {
        return seriesName;
    }

    public String getSOPInstanceUID() {
        return fn.getSOPInstanceUID();
    }

    public String getRefSOPClassUID() {
        return fn.getRefSOPClassUID();
    }

    public String getRefStructSOPInst() {
        return fn.getRefStructSOPInst();
    }

    public String getRefDoseSOPInst() {
        return fn.getRefDoseSOPInst();
    }

    public String getRefSOPinstanceUID() {
        if (fn.getListRefRTPlanSequence().size() == 1) {
            return fn.getListRefRTPlanSequence().getFirst();
        }
        return "";
    }

    public FileName[] getFileNames() {
        FileName[] names = new FileName[list.size()];
        names = list.toArray(names);
        Arrays.sort(names);
        return names;
    }

    private void generateROIs() {
        int i = 0;
        String ObsLabel = "", RoiType = "";
        for (String name : fn.getROINameList()) {

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

    public String getROIRefFrame() {
        return list.getFirst().getROIRefFrameOfRef();
    }

    private boolean frameOfRefOK() {
        return list.getFirst().frameOfRefsOK()
                && list.getFirst().hasROIonlyOneRefFrameOfRefUID();
    }

    public String getRTDoseComment() {
        return fn.getRtDoseComment();
    }

    public String getRTPlanLabel() {
        return fn.getRtPlanLabel();
    }

    public String getRTPlanName() {
        return fn.getRtPlanName();
    }

    public String getRTPlanDescription() {
        return fn.getRtPlanDescription();
    }

    public String getSSLabel() {
        return fn.getSsLabel();
    }

    public String getSSName() {
        return fn.getSsName();
    }

    public String getSSDescription() {
        return fn.getSsDescription();
    }

    public String getRTImageName() {
        return fn.getRtImageName();
    }

    public String getRTImageLabel() {
        return fn.getRtImageLabel();
    }

    public String getRTImageDescription() {
        return fn.getRtImageDescription();
    }

    public String getModality() {
        return seriesName.getModality();
    }

    public void display(DirectoryPanel dp) {
        dp.add(cb);
        seriesName.setNumberOfFiles(list.size());
        dp.add(seriesName, RowLayout.span(4));
        dp.add(RowLayout.crlf());

        if (seriesName.getModality().equals("RTSTRUCT")) {

            if (frameOfRefOK()) {
                generateROIs();
                for (ROI r : roiList) {
                    r.display(dp);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (String ref : list.getFirst().getListRoiRefFrameOfRef()) {
                    sb.append(ref).append("\n");
                }

                Runnable enable = new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null,
                                "The referenced frames of reference do not match\n" +
                                        "The following references were found: \n" +
                                        sb.toString(),
                                "Series: " + seriesName.getDisplayedSeriesDescription(), JOptionPane.WARNING_MESSAGE);
                    }
                };
                SwingUtilities.invokeLater(enable);
            }
        }

        if (showDicomFiles) {
            for (FileName fn : getFileNames()) {
                fn.display(dp);
            }
        }
    }
}