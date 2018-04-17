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

public class Study implements ActionListener, Comparable<Study> {

    private final LinkedList<FileName> list;
    private final Hashtable<String, Series> seriesTable;
    private final StudyCheckBox cb;
    private final StudyName studyName;
    private final String patientName;
    private final String studyDate;
    private final String siuid;
    private final String patientID;
    private final String gender;
    private final String dateOfBirth;
    private String requiredStudyType = "";
    private boolean showDicomFiles = false;
    private boolean seriesVisible = true;
    private boolean wrongStudyType = false;
    private boolean wrongReferences = false;
    private String refs;

    public Study(FileName fileName) {
        list = new LinkedList<FileName>();
        seriesTable = new Hashtable<String, Series>();
        cb = new StudyCheckBox();
        cb.addActionListener(this);
        patientName = fileName.getPatientName();
        studyDate = fileName.getStudyDate();
        studyName = new StudyName(fileName);
        studyName.addActionListener(this);
        siuid = fileName.getStudyInstanceUID();
        patientID = fileName.getPatientID();
        gender = fileName.getPatientGender();
        dateOfBirth  =fileName.getPatientBirthday();
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
        } else if (source.equals(studyName)) {
            if (seriesVisible) {
                hideSeries(false);
            } else {
                hideSeries(true);
            }
        }
    }

    private void hideSeries(boolean hide) {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), hide);
        }
        Set<String> keys = seriesTable.keySet();
        for (String key : keys) {
            if (dp != null) {
                dp.setRowVisible(seriesTable.get(key).getCb(), hide);
                seriesTable.get(key).hideROIs(hide);
            }
        }
        seriesVisible = hide;
    }

    public int compareTo(Study study) {
        if (study == null) return 0;
        int c;
        if ((c = this.patientName.compareTo(study.getPatientName())) != 0) return c;
        return this.studyDate.compareTo(study.getStudyDate());
    }

    public StudyCheckBox getCheckBox() {
        return cb;
    }

    public void setRequiredStudyType(String st) {
        requiredStudyType = st;
    }

    private String getPatientName() {
        return patientName;
    }

    public String getSiuid() {
        return siuid;
    }

    private String getStudyDate() {
        return studyDate;
    }

    public String getStudyDescription() {
        return studyName.getStudyDescription();
    }

    public boolean isSelected() {
        return cb.isSelected();
    }

    public void setSelected(boolean selected) {
        cb.setSelected(selected);
        if (selected) selectAll();
        else deselectAll();
    }

    private void selectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(true);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), true);
        }
        Set<String> keys = seriesTable.keySet();
        for (String key : keys) {
            seriesTable.get(key).setSelected(true);
            if (dp != null) dp.setRowVisible(seriesTable.get(key).getCb(), true);
        }
        seriesVisible = true;
    }

    private void deselectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(false);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), false);
        }
        Set<String> keys = seriesTable.keySet();
        for (String key : keys) {
            seriesTable.get(key).setSelected(false);
            if (dp != null) dp.setRowVisible(seriesTable.get(key).getCb(), false);
        }
        seriesVisible = false;
    }

    public void showDicomFiles(boolean s) {
        showDicomFiles = s;
    }

    private DirectoryPanel getDirectoryPanel() {
        Component container = cb.getParent();
        if ((container != null) && (container instanceof DirectoryPanel)) {
            return (DirectoryPanel) container;
        }
        return null;
    }

    private void generateSeries() {
        for (final FileName name : list) {
            studyName.addSeriesModality(name.getModality());
            String seriesInstanceUID = name.getSeriesInstanceUID();
            if (!seriesTable.containsKey(seriesInstanceUID)) {
                Series series = new Series(name);
                series.showDicomFiles(showDicomFiles);
                seriesTable.put(seriesInstanceUID, series);
            } else {
                Series s = seriesTable.get(seriesInstanceUID);
                s.add(name);
            }
        }
    }

    private boolean checkFrameOfRef() {

        String temp = "default";
        boolean moreThanOne = true;
        for (String k : seriesTable.keySet()) {
            if (!temp.equals(seriesTable.get(k).getFrameOfRef())) {
                if (moreThanOne) {
                    if (!seriesTable.get(k).getFrameOfRef().equals("")) {
                        temp = seriesTable.get(k).getFrameOfRef();
                        moreThanOne = false;
                    }
                } else if (!seriesTable.get(k).getFrameOfRef().equals("")) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isStudyTypeWrong() {
        return wrongStudyType;
    }

    public boolean isReferenceWrong() {
        return wrongReferences;
    }

    public String getStudyType() {
        return studyName.getClassification();
    }

    private void checkStudyType() {

        if (!studyName.getClassification().equals(requiredStudyType)
                && !requiredStudyType.equals("")) {
            wrongStudyType = true;
        }

        if (requiredStudyType.equals("Contouring")) {
            String ctSOPClassUID = "";
            String rtstructRefSOPClassUID = "";
            for (String k : seriesTable.keySet()) {
                if (seriesTable.get(k).getSeriesName().getModality().equals("CT")) {
                    ctSOPClassUID = seriesTable.get(k).getSOPClassUID();
                }
                if (seriesTable.get(k).getSeriesName().getModality().equals("RTSTRUCT")) {
                    rtstructRefSOPClassUID = seriesTable.get(k).getRefSOPClassUID();
                }
            }
            if (!ctSOPClassUID.equals(rtstructRefSOPClassUID)) {
                wrongReferences = true;
            }
        } else if (requiredStudyType.equals("TreatmentPlan")) {
            String rtplanRefStructSOPInst = "";
            String rtplanRefDoseSOPInst = "";
            String rtstructSOPInstanceUID = "";
            String rtdoseSOPInstanceUID = "";
            // RTDOSE -> RTPLAN
            String refSOPinstanceUID = "";
            String rtplanSOPInstanceUID = "";

            for (String k : seriesTable.keySet()) {
                if (seriesTable.get(k).getSeriesName().getModality().equals("RTPLAN")) {
                    rtplanRefStructSOPInst = seriesTable.get(k).getRefStructSOPInst();
                    rtplanRefDoseSOPInst = seriesTable.get(k).getRefDoseSOPInst();
                    rtplanSOPInstanceUID = seriesTable.get(k).getSOPInstanceUID();
                }
                if (seriesTable.get(k).getSeriesName().getModality().equals("RTSTRUCT")) {
                    rtstructSOPInstanceUID = seriesTable.get(k).getSOPInstanceUID();
                }
                if (seriesTable.get(k).getSeriesName().getModality().equals("RTDOSE")) {
                    rtdoseSOPInstanceUID = seriesTable.get(k).getSOPInstanceUID();
                    refSOPinstanceUID = seriesTable.get(k).getRefSOPinstanceUID();
                }
            }

            if (!rtplanRefStructSOPInst.equals("") && !rtplanRefDoseSOPInst.equals("") &&
                    (!rtplanRefStructSOPInst.equals(rtstructSOPInstanceUID) ||
                    !rtplanRefDoseSOPInst.equals(rtdoseSOPInstanceUID))) {
                wrongReferences = true;
            }

            if (!refSOPinstanceUID.equals("") &&
                    !refSOPinstanceUID.equals(rtplanSOPInstanceUID)) {
                wrongReferences = true;
            }
        }
    }

    public String getPatientname() {
        return patientName;
    }

    public String getPatientID() {
        return patientID;
    }

    public String getGender() {
        return gender;
    }

    public String getPatientBirthDate() {
        return dateOfBirth;
    }

    public Series[] getSeries() {
        Series[] series = new Series[seriesTable.size()];
        series = seriesTable.values().toArray(series);
        Arrays.sort(series);
        return series;
    }

    public void display(DirectoryPanel dp) {
        generateSeries();
        cb.setStudy(this);
        dp.add(cb);
        studyName.setClassification();
        studyName.setNumberOfSeries(seriesTable.size());
        if (!checkFrameOfRef() && (studyName.getClassification().equals("TreatmentPlan") ||
                studyName.getClassification().equals("Contouring"))) {
            refs = "";
            LinkedList<String> tempList = new LinkedList<String>();
            for (String k : seriesTable.keySet()) {
                String temp = seriesTable.get(k).getFrameOfRef();
                if (temp.isEmpty()) {
                    tempList.add("empty reference");
                } else {
                    tempList.add(temp);
                }
            }
            do {
                String t = tempList.getFirst();
                int occurences = Collections.frequency(tempList, t);
                refs += occurences + "x " + t + "\n";
                if (occurences == 1){
                    for (String k : seriesTable.keySet()) {
                        if (seriesTable.get(k).getFrameOfRef().equals(t)) {
                            refs = refs.trim() + " - (" + seriesTable.get(k).getDisplayedSeriesDescription() + ")\n";
                        }
                    }
                }
                tempList.removeAll(Collections.singleton(t));
            } while (tempList.size() > 0);

            Log.getInstance().append("Study: " + studyName.getStudyDescription() + "\n" +
                    "The following references were found: \n" +
                    refs);

            Runnable enable = new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            "The referenced frames of reference do not match\n" +
                                    "The following references were found: \n" +
                                    refs,
                            "Study: " + studyName.getStudyDescription(), JOptionPane.WARNING_MESSAGE);
                }
            };
            SwingUtilities.invokeLater(enable);
        }

        checkStudyType();

        dp.add(studyName);
        dp.add(RowLayout.crlf());

        Set<String> keys = seriesTable.keySet();
        for (String key : keys) {
            seriesTable.get(key).display(dp);
        }
    }

}
