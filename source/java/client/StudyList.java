/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import org.rsna.ui.RowLayout;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class StudyList implements ActionListener {

	Hashtable<String,Study> table;
	Hashtable<String, LinkedList<String>> patientIDtoStudy;
	Hashtable<String, String> patientIDtoPatientName;

	File directory = null;
	boolean radioMode = false;
	boolean anio = false;
	boolean showDicomFiles = false;
	FileFilter filesOnlyFilter = new FilesOnlyFilter();
	FileFilter directoriesOnlyFilter = new DirectoriesOnlyFilter();

	String firstStudy = null;
	String studyType = "";

	public StudyList(File directory, boolean radioMode,
					 boolean acceptNonImageObjects, boolean showDicomFiles) {
		this.directory = directory;
		this.radioMode = radioMode;
		this.anio = acceptNonImageObjects;
		this.showDicomFiles = showDicomFiles;
		table = new Hashtable<String,Study>();
		patientIDtoStudy = new Hashtable<String,LinkedList<String>>();
		patientIDtoPatientName = new Hashtable<String,String>();
		addFiles(directory);
		StatusPane.getInstance().setText("Directory: "+directory);
	}

	public void actionPerformed(ActionEvent event) {
		StudyCheckBox cb = (StudyCheckBox)event.getSource();
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
			//table.get(patientIDtoStudy.get(patientIDtoStudy.keys().nextElement()).getFirst()).setSelected(true);
			table.get(firstStudy).setSelected(true);
		}
	}

	public void deselectAll() {
		for (Study study : table.values()) {
			study.setSelected(false);
		}
	}

	public void setStudyType(String st) {
		studyType = st;
	}

	public Study[] getStudies() {
		Study[] studies = new Study[table.size()];
		studies = table.values().toArray(studies);
		Arrays.sort(studies);
		return studies;
	}

	private void addFiles(File dir) {
		File[] files = dir.listFiles(filesOnlyFilter);
		for (File file: files) {
			FileName fileName = new FileName(file);
			if (fileName.isDICOM() && (anio || fileName.isImage())) {
				String siuid = fileName.getStudyInstanceUID();
				String patientID = fileName.getPatientID();

				if (patientIDtoStudy.get(patientID) == null) {
					LinkedList<String> siuids = new LinkedList<String>();
					siuids.add(siuid);
					patientIDtoStudy.put(patientID, siuids);
					patientIDtoPatientName.put(patientID, fileName.getPatientName());
				}
				else patientIDtoStudy.get(patientID).add(siuid);

				Study study = table.get(siuid);
				if (study == null) {
					study = new Study(fileName);
					study.getCheckBox().addActionListener(this);
					study.showDicomFiles(showDicomFiles);
					table.put(siuid, study);
				}
				else study.add(fileName);
			}
		}
		files = dir.listFiles(directoriesOnlyFilter);
		for (File file : files) addFiles(file);
	}

	public void display(DirectoryPanel dp) {

		boolean first = false;

		for (String patient : patientIDtoStudy.keySet()) {

			PatientName patientName = new PatientName(patient
					+ " - " + patientIDtoPatientName.get(patient));
			dp.add(patientName, RowLayout.span(4));
			dp.add(RowLayout.crlf());

			for (Study study : getStudies()) {
				study.setStudyType(studyType);
				if (patientIDtoStudy.get(patient).contains(study.getSiuid())) {
					if (first == false) {
						first = true;
						firstStudy = study.getSiuid();
					}
					study.display(dp);
				}
			}
		}
	}

}
