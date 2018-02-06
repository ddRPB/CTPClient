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

	LinkedList<FileName> list = null;
	LinkedList<SeriesName> seriesNameList = null;
	Hashtable<String, Series> seriesTable;
	StudyCheckBox cb = null;
	StudyName studyName = null;
	String patientName = null;
	String studyDate = null;
	String patientID = null;
	String siuid = null;
	String studyType = "";
	boolean showDicomFiles = false;
	boolean seriesVisible = true;

	public Study(FileName fileName) {
		list = new LinkedList<FileName>();
		seriesTable = new Hashtable<String, Series>();
		cb = new StudyCheckBox();
		cb.addActionListener(this);
		patientName = fileName.getPatientName();
		patientID = fileName.getPatientID();
		studyDate = fileName.getStudyDate();
		studyName = new StudyName(fileName);
		studyName.addActionListener(this);
		siuid = fileName.getStudyInstanceUID();
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
		else if (source.equals(studyName)) {
			if (seriesVisible) {
				hideSeries(false);
			}
			else {
				hideSeries(true);
			}
		}
	}

	public void hideSeries(boolean hide) {
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
		if (study == null)  return 0;
		int c;
		if ( (c = this.patientName.compareTo(study.getPatientName())) != 0 ) return c;
		return this.studyDate.compareTo(study.getStudyDate());
 	}

	public StudyCheckBox getCheckBox() {
		return cb;
	}

	public void setStudyType(String st) {
		studyType = st;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getSiuid() {
		return siuid;
	}

	public String getPatientID() { return patientID; }

	public String getStudyDate() {
		return studyDate;
	}

	public boolean isSelected() {
		return cb.isSelected();
	}

	public void setSelected(boolean selected) {
		cb.setSelected(selected);
		if (selected) selectAll();
		else deselectAll();
	}

	public void selectAll() {
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

	public void deselectAll() {
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
			return (DirectoryPanel)container;
		}
		return null;
	}

	public FileName[] getFileNames() {
		FileName[] names = new FileName[list.size()];
		names = list.toArray(names);
		Arrays.sort(names);
		return names;
	}

	public void generateSeries() {
		for (final FileName name : list) {
			studyName.addSeriesModality(name.getModality());
			String seriesInstanceUID = name.getSeriesInstanceUID();
			if(!seriesTable.containsKey(seriesInstanceUID)){
				Series series = new Series(name);
				series.showDicomFiles(showDicomFiles);
				seriesTable.put(seriesInstanceUID, series);
			}
			else {
				Series s = seriesTable.get(seriesInstanceUID);
				s.add(name);
			}
		}
	}

	public boolean checkFrameOfRef() {

		String temp = "default";
		boolean moreThanOne = true;
		for (String k : seriesTable.keySet()) {
		    if (!temp.equals(seriesTable.get(k).getFrameOfRef())) {
				if(moreThanOne){
					if (!seriesTable.get(k).getFrameOfRef().equals("")) {
						temp = seriesTable.get(k).getFrameOfRef();
						moreThanOne = false;
					}
				}
				else if (!seriesTable.get(k).getFrameOfRef().equals("")) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean checkStudyType() {

		if(!studyName.getClassification().equals(studyType)) {
			return false;
		}

		if(studyType.equals("Contouring")) {
			String ctSOPClassUID = "";
			String rtstructRefSOPClassUID = "";
			for(String k : seriesTable.keySet()) {
				if(seriesTable.get(k).getSeriesName().getModality().equals("CT")){
					ctSOPClassUID = seriesTable.get(k).getSOPClassUID();
				}
				if(seriesTable.get(k).getSeriesName().getModality().equals("RTSTRUCT")){
					rtstructRefSOPClassUID = seriesTable.get(k).getRefSOPClassUID();
				}
			}
			if(!ctSOPClassUID.equals(rtstructRefSOPClassUID)) {
				return false;
			}
		}else if (studyType.equals("TreatmentPlan")) {
            String rtplanRefStructSOPInst = "";
            String rtplanRefDoseSOPInst = "";
            String rtstructSOPInstanceUID = "";
            String rtdoseSOPInstanceUID = "";
		    for (String k : seriesTable.keySet()) {
		    	if (seriesTable.get(k).getSeriesName().getModality().equals("RTPLAN")) {
					rtplanRefStructSOPInst = seriesTable.get(k).getRefStructSOPInst();
					rtplanRefDoseSOPInst = seriesTable.get(k).getRefDoseSOPInst();
				}
				if (seriesTable.get(k).getSeriesName().getModality().equals("RTSTRUCT")) {
		    		rtstructSOPInstanceUID = seriesTable.get(k).getSOPInstanceUID();
				}
				if (seriesTable.get(k).getSeriesName().getModality().equals("RTDOSE")) {
					rtdoseSOPInstanceUID = seriesTable.get(k).getSOPInstanceUID();
				}
            }

            if (!rtplanRefStructSOPInst.equals(rtstructSOPInstanceUID) ||
					!rtplanRefDoseSOPInst.equals(rtdoseSOPInstanceUID)) {
            	return false;
			}
		}

		return true;
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
			studyName.setFrameOfRefInfo();
		}

		if (!checkStudyType()) {
			dp.add(new JLabel("studyType!!!"));
			dp.add(RowLayout.crlf());
		}

		dp.add(studyName);
		dp.add(RowLayout.crlf());

		Set<String> keys = seriesTable.keySet();
		for (String key : keys) {
			seriesTable.get(key).display(dp);
		}
	}

}
