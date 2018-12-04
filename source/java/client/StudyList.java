/*---------------------------------------------------------------
 *  Copyright 2014 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import org.rsna.ui.RowLayout;

import java.awt.event.*;
import java.io.*;
import java.util.*;

public class StudyList implements ActionListener {

    private final Hashtable<String, Study> table;
    private final Hashtable<String, LinkedList<String>> patientIDtoStudy;
    private final Hashtable<String, String> patientIDtoPatientName;

    private final boolean radioMode;
    private final boolean anio;
    private final boolean showDicomFiles;
    private final FileFilter filesOnlyFilter = new FilesOnlyFilter();
    private final FileFilter directoriesOnlyFilter = new DirectoriesOnlyFilter();
    private boolean wrongStudyType = false;
    boolean wrongReferences = false;
    private String firstStudy = null;
    private String studyType = "";
    private String birthdate = "";
    private String patientsGender = "";
    private String studyDescription = "";

    public StudyList(File directory, boolean radioMode,
                     boolean acceptNonImageObjects, boolean showDicomFiles) {
        this.radioMode = radioMode;
        this.anio = acceptNonImageObjects;
        this.showDicomFiles = showDicomFiles;
        table = new Hashtable<String, Study>();
        patientIDtoStudy = new Hashtable<String, LinkedList<String>>();
        patientIDtoPatientName = new Hashtable<String, String>();
        addFiles(directory);
        StatusPane.getInstance().setText("Directory: " + directory);
    }

    public void actionPerformed(ActionEvent event) {
        StudyCheckBox cb = (StudyCheckBox) event.getSource();
        Study study = cb.getStudy();
        if ((study != null) && cb.isSelected() && radioMode) {
            for (Study s : table.values()) {
                if (!s.equals(study)) s.setSelected(false);
            }
        }
    }

    public void selectFirstStudy() {
        if (table.size() > 0) {
            deselectAll();
            table.get(firstStudy).setSelected(true);
        }
    }

    public String getStudyDescriptionOfFirstStudy() {
        return studyDescription;
    }

    private void deselectAll() {
        for (Study study : table.values()) {
            study.setSelected(false);
        }
    }

    public void setRequiredStudyType(String st) {
        studyType = st;
    }

    public boolean isNumberOfUniqueSOPInstanceUIDsEqual() {

        List<String> uniqueSOPInstanceUIDs = new ArrayList<>();
        int counter = 0;

        for (String s : table.keySet()) {
            Series[] allSeries = table.get(s).getSeries();
            for (Series series : allSeries) {
                if (series.isSelected()) {
                    for (FileName fileName : series.getFileNames()) {
                        if (fileName.isSelected()) {
                            counter++;
                            // all selected unique SOPInstanceUIDs
                            if (!uniqueSOPInstanceUIDs.contains(fileName.getSOPInstanceUID())) {
                                uniqueSOPInstanceUIDs.add(fileName.getSOPInstanceUID());
                            }
                        }
                    }
                }
            }
        }

        if (!(uniqueSOPInstanceUIDs.size() == counter)) {
            return false;
        }
        return true;
    }

    public boolean isSelectedConstellationValid() {

        List<String> selectedModalities = new ArrayList<>();

        for (String s : table.keySet()) {
            Series[] allSeries = table.get(s).getSeries();
            for (Series series : allSeries) {
                if (series.isSelected()) {
                    selectedModalities.add(series.getModality());
                }
            }
        }

        if (selectedModalities.contains("CT")) {

            if (selectedModalities.size() == 1) return true; // "CT"

            if (selectedModalities.contains("PT")) {
                //"PET-CT"
                if (selectedModalities.size() == 2) return true;

            } else if (selectedModalities.contains("RTSTRUCT")) {

                if (selectedModalities.contains("RTPLAN")
                        && selectedModalities.contains("RTDOSE")) {
                    //"TreatmentPlan"
                    if (selectedModalities.size() == 4) return true;
                } else {
                    //"Contouring"
                    if (selectedModalities.size() == 2) return true;
                }
            }
        } else if (selectedModalities.contains("MR")) {
            //"MRI"
            if (selectedModalities.size() == 1) return true;

            if (selectedModalities.contains("PT")) {
                //"PET-MRI"
                if (selectedModalities.size() == 2) return true;
            }
        }

        return false;
    }

    public String getPatientsGender() {
        return patientsGender;
    }

    public String getPatientBirthDate() {
        return birthdate;
    }

    public String getPatientBirthYear() {
        return birthdate.substring(0, 4);
    }

    public boolean isStudyTypeWrong() {
        return wrongStudyType;
    }

    public Study[] getStudies() {
        Study[] studies = new Study[table.size()];
        studies = table.values().toArray(studies);
        Arrays.sort(studies);
        return studies;
    }

    private void addFiles(File dir) {
        File[] files = dir.listFiles(filesOnlyFilter);
        for (File file : Objects.requireNonNull(files)) {
            FileName fileName = new FileName(file);
            if (fileName.isDICOM() && (anio || fileName.isImage())) {
                String siuid = fileName.getStudyInstanceUID();
                String patientID = fileName.getPatientID();

                if (patientIDtoStudy.get(patientID) == null) {
                    LinkedList<String> siuids = new LinkedList<String>();
                    siuids.add(siuid);
                    patientIDtoStudy.put(patientID, siuids);
                    patientIDtoPatientName.put(patientID, fileName.getPatientName());
                } else patientIDtoStudy.get(patientID).add(siuid);

                Study study = table.get(siuid);
                if (study == null) {
                    study = new Study(fileName);
                    study.getCheckBox().addActionListener(this);
                    study.showDicomFiles(showDicomFiles);
                    table.put(siuid, study);
                } else study.add(fileName);
            }
        }
        files = dir.listFiles(directoriesOnlyFilter);
        for (File file : Objects.requireNonNull(files)) addFiles(file);
    }

    public void display(DirectoryPanel dp) {

        boolean first = false;

        for (String patient : patientIDtoStudy.keySet()) {

            PatientName patientName = new PatientName(patient
                    + " - " + patientIDtoPatientName.get(patient));
            dp.add(patientName, RowLayout.span(4));
            dp.add(RowLayout.crlf());

            for (Study study : getStudies()) {
                study.setRequiredStudyType(studyType);
                if (patientIDtoStudy.get(patient).contains(study.getSiuid())) {
                    if (!first) {
                        first = true;
                        firstStudy = study.getSiuid();
                        studyDescription = study.getStudyDescription();
                    }
                    study.display(dp);
                    wrongStudyType = study.isStudyTypeWrong();
                    wrongReferences = study.isReferenceWrong();
                    birthdate = study.getPatientBirthDate();
                    patientsGender = study.getGender();
                }
            }
        }
    }

}
