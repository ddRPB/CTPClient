/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import javax.swing.*;
import javax.xml.crypto.Data;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ui.RowLayout;
import org.rsna.util.StringUtil;

public class FileName implements Comparable<FileName> {

	File file;
	FileSize fileSize;
	String patientName = "";
	String patientID = "";
	String studyInstanceUID = "";
	String seriesInstanceUID = "";
	String studyDate = "";
	String seriesDate = "";
	String modality = "";
	int seriesNumberInt = 0;
	int acquisitionNumberInt = 0;
	int instanceNumberInt = 0;
	boolean isDICOM = false;
	boolean isImage = false;
	FileCheckBox cb = null;
	StatusText statusText = null;
	String description = "";

	String studyDescription = "";
	String seriesDescription = "";
	String frameOfReference = "";
	boolean oncentraCheck = false;

	String refSOPClassUID = "";
	String refStructSOPInst = "";
	String refDoseSOPInst = "";

	LinkedList<String> listRoiRefFrameOfRef = null;
	LinkedList<String> listRefFrameOfRefFrameOfRefUID = null;
	LinkedList<String> listROINames = null;
	LinkedList<String> listROINumber = null;
	LinkedList<String> listRefROINumber = null;
	LinkedList<String> listROIObservationLabel = null;
	LinkedList<String> listROIInterpretedType = null;

	DicomObject dob;

	public FileName(File file) {
		this.file = file;
		cb = new FileCheckBox();
		listROINames = new LinkedList<String>();
		listROINumber = new LinkedList<String>();
		listRefROINumber = new LinkedList<String>();
		listROIObservationLabel = new LinkedList<String>();
		listROIInterpretedType = new LinkedList<String>();
		listRoiRefFrameOfRef = new LinkedList<String>();
		listRefFrameOfRefFrameOfRefUID = new LinkedList<String>();
		statusText = new StatusText();
		fileSize = new FileSize(file);
		try {
			dob = new DicomObject(file);
			isDICOM = true;
			isImage = dob.isImage();
			patientName = fixNull(dob.getPatientName());
			patientID = fixNull(dob.getPatientID());
			studyInstanceUID = fixNull(dob.getStudyInstanceUID());
			seriesInstanceUID = fixNull(dob.getSeriesInstanceUID());
			modality = fixNull(dob.getModality());
			studyDate = fixDate(dob.getStudyDate());
			String seriesNumber = fixNull(dob.getSeriesNumber());
			String acquisitionNumber = fixNull(dob.getAcquisitionNumber());
			String instanceNumber = fixNull(dob.getInstanceNumber());
			seriesNumberInt = StringUtil.getInt(seriesNumber);
			acquisitionNumberInt = StringUtil.getInt(acquisitionNumber);
			instanceNumberInt = StringUtil.getInt(instanceNumber);

			studyDescription = fixNull(dob.getStudyDescription());
			frameOfReference = fixNull(dob.getElementValue(Tags.FrameOfReferenceUID));

			//look for a precise description
			if (modality.equals("RTDOSE")) {
				seriesDescription = fixNull(dob.getElementValue(Tags.DoseComment));
				seriesDate = fixDate(dob.getElementValue(Tags.InstanceCreationDate));
			}
			else if (modality.equals("RTPLAN")) {
                seriesDescription = fixNull(dob.getElementValue(Tags.RTPlanLabel));
                if (seriesDescription.equals("")) { fixNull(dob.getElementValue(Tags.RTPlanName)); }
                if (seriesDescription.equals("")) { fixNull(dob.getElementValue(Tags.RTPlanDescription)); }
				seriesDate = fixDate(dob.getElementValue(Tags.RTPlanDate));

				DcmElement refStructSet = dob.getDataset().get(Tags.RefStructureSetSeq);
				if (refStructSet != null) {
					for (int i = 0; i < refStructSet.countItems(); i++) {
						Dataset dcm = refStructSet.getItem(i);
						refStructSOPInst = dcm.getString(Tags.RefSOPInstanceUID);
					}
				}

				DcmElement refDoseSeq = dob.getDataset().get(Tags.RefDoseSeq);
				if (refDoseSeq != null) {
					for (int i = 0; i < refDoseSeq.countItems(); i++) {
						Dataset dcm = refDoseSeq.getItem(i);
						refDoseSOPInst = dcm.getString(Tags.RefSOPInstanceUID);
					}
				}
			}
			else if (modality.equals("RTSTRUCT")) {
				seriesDescription = fixNull(dob.getElementValue(Tags.StructureSetLabel));
				String temp = fixNull(dob.getElementValue(Tags.StructureSetName));
				if (seriesDescription.equals("")) {
					seriesDescription += temp;
				}
				else if (!temp.equals("")) {
					seriesDescription += " - " + temp;
				}

				if (seriesDescription.equals("")) { fixNull(dob.getElementValue(Tags.StructureSetDescription)); }
				seriesDate = fixDate(dob.getElementValue(Tags.StructureSetDate));


			}
			else if (modality.equals("RTIMAGE")) {
				seriesDescription = fixNull(dob.getElementValue(Tags.RTImageName));
				if (seriesDescription.equals("")) { fixNull(dob.getElementValue(Tags.RTImageLabel)); }
				if (seriesDescription.equals("")) { fixNull(dob.getElementValue(Tags.RTImageDescription)); }
				seriesDate = fixDate(dob.getElementValue(Tags.InstanceCreationDate));
			}

			if (seriesDescription.equals("")) { seriesDescription = fixNull(dob.getSeriesDescription()); }
			if (seriesDate.equals("")) { seriesDate =  fixDate(dob.getElementValue(Tags.SeriesDate));}


			if(modality.equals("RTSTRUCT")) {
				//get ROI information
				DcmElement roiSequence = dob.getDataset().get(Tags.StructureSetROISeq);
				if (roiSequence != null) {
					for (int i = 0; i < roiSequence.countItems(); i++) {
						Dataset dcm = roiSequence.getItem(i);
						listROINames.add(dcm.getString(Tags.ROIName));
						listROINumber.add(dcm.getString(Tags.ROINumber));

						String temp = dcm.getString(Tags.RefFrameOfReferenceUID);
						if (!listRoiRefFrameOfRef.contains(temp)) {
							listRoiRefFrameOfRef.add(temp);
						}
					}
				}

				DcmElement roiObservations = dob.getDataset().get(Tags.RTROIObservationsSeq);
				if (roiObservations != null) {
					for (int i = 0; i < roiObservations.countItems(); i++) {
						Dataset dcm = roiObservations.getItem(i);
						listRefROINumber.add(dcm.getString(Tags.RefROINumber));
						listROIObservationLabel.add(fixNull(dcm.getString(Tags.ROIObservationLabel)));
						listROIInterpretedType.add(fixNull(dcm.getString(Tags.RTROIInterpretedType)));
					}
				}

				//handle Oncentra MasterPlan case for ReferenceCheck
				// read all the referenced frameofrefs -> should only be one
				// return it and compare it with the refs in the other files
 				DcmElement refFrameOfRef = dob.getDataset().get(Tags.RefFrameOfReferenceSeq);
				if (refFrameOfRef != null) {
					for (int i = 0; i < refFrameOfRef.countItems(); i++) {
						Dataset dcm = refFrameOfRef.getItem(i);

						listRefFrameOfRefFrameOfRefUID.add(dcm.getString(Tags.FrameOfReferenceUID));

						DcmElement FrameOfReferenceRelationshipSeq = dcm.get(Tags.FrameOfReferenceRelationshipSeq);

						if (FrameOfReferenceRelationshipSeq != null) {
							for (int y = 0; y <  FrameOfReferenceRelationshipSeq.countItems(); y++) {
								Dataset dataset = FrameOfReferenceRelationshipSeq.getItem(y);

								String transformationComment =
										dataset.getString(Tags.FrameOfReferenceTransformationComment);
								if (transformationComment.equals("Treatment planning reference point")) {
									oncentraCheck = true;
								}
							}
						}
					}
				}

				DcmElement roiContourSeq = dob.getDataset().get(Tags.ROIContourSeq);
				if(roiContourSeq != null) {
					for(int i = 0; i < roiContourSeq.countItems(); i++) {
						Dataset dcm = roiContourSeq.getItem(i);
						DcmElement contourSeq = dcm.get(Tags.ContourSeq);
						for(int j = 0; j < contourSeq.countItems(); j++) {
							Dataset dc = contourSeq.getItem(j);
							DcmElement contourImageSeq = dc.get(Tags.ContourImageSeq);
							for(int k = 0; k < contourImageSeq.countItems(); k++) {
								Dataset d = contourImageSeq.getItem(k);
								refSOPClassUID = d.getString(Tags.RefSOPClassUID);
							}
						}
					}
				}
			}
		}
		catch (Exception nonDICOM) { }
	}

	public String getSOPInstanceUID() { return dob.getSOPInstanceUID(); }

	public String getSOPClassUID() { return dob.getSOPClassUID(); }

	public  String getRefSOPClassUID() { return refSOPClassUID; }

	public String getRefStructSOPInst() { return refStructSOPInst; }

	public String getRefDoseSOPInst() { return refDoseSOPInst; }

	public String getPatientGender () { return  dob.getElementValue(Tags.PatientSex); }

	public boolean frameOfRefsOK() {

		if (listRefFrameOfRefFrameOfRefUID.size() > 1 && oncentraCheck == false) {
			return false;
		}
		return true;
	}

	public boolean hasROIonlyOneRefFrameOfRefUID() {
		if (listRoiRefFrameOfRef.size() > 1) {
			return false;
		}
		return true;
	}

	public String getROIRefFrameOfRef() {
		if (listRoiRefFrameOfRef.size() == 1) {
			return listRoiRefFrameOfRef.getFirst();
		}
		return null;
	}

	public LinkedList<String> getROINameList() { return listROINames; }

    public LinkedList<String> getROINumberList() { return listROINumber;}

    public  LinkedList<String> getRefROINumberList() { return listRefROINumber; }

    public  LinkedList<String> getROIObservationLabelList() { return listROIObservationLabel; }

    public  LinkedList<String> getROIInterpretedTypeList() { return listROIInterpretedType; }

	private String fixNull(String s) {
		return (s == null) ? "" : s;
	}

	public File getFile() {
		return file;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getPatientID() {
		return patientID;
	}

	public String getStudyInstanceUID() {
		return studyInstanceUID;
	}

	public String getSeriesInstanceUID() { return seriesInstanceUID; }

	public String getStudyDate() {
		return studyDate;
	}

	public String getSeriesDate() { return seriesDate;}

	public String getModality() {
		return modality;
	}

	public String getStudyDescription(){
		return studyDescription;
	}

	public  String getFrameOfReference(){
		return frameOfReference;
	}

	public String getSeriesDescription() { return seriesDescription; }

	private String fixDate(String s) {
		if (s == null) s = "";
		if (s.length() == 8) {
			s = s.substring(0,4) + "." + s.substring(4,6) + "." + s.substring(6);
		}
		return s;
	}

	public int compareTo(FileName fn) {
		if (fn == null)  return 0;
		int c;
		if ( (c = this.patientID.compareTo(fn.patientID)) != 0 ) return c;
		if ( (c = this.studyDate.compareTo(fn.studyDate)) != 0 ) return c;
		if ( (c = this.studyInstanceUID.compareTo(fn.studyInstanceUID)) != 0 ) return c;
		if ( (c = this.seriesNumberInt - fn.seriesNumberInt) != 0 ) return c;
		if ( (c = this.acquisitionNumberInt - fn.acquisitionNumberInt) != 0 ) return c;
		if ( (c = this.instanceNumberInt - fn.instanceNumberInt) != 0 ) return c;
		return 0;
 	}

 	public boolean isSamePatient(FileName fn) {
		if (fn == null) return false;
		return (this.patientID.equals(fn.patientID));
	}

	public boolean isSameStudy(FileName fn) {
		if (fn == null) return false;
		return isSamePatient(fn) && (this.studyInstanceUID.equals(fn.studyInstanceUID));
	}

	public boolean isDICOM() {
		return isDICOM;
	}

	public boolean isImage() {
		return isImage;
	}

	public void setSelected(boolean selected) {
		cb.setSelected(selected);
	}

	public boolean isSelected() {
		return cb.isSelected();
	}

	public StatusText getStatusText() {
		return statusText;
	}

	public FileCheckBox getCheckBox() {
		return cb;
	}

	public void display(DirectoryPanel dp) {
		JPanel panel = new JPanel();
		panel.setLayout(new RowLayout(0, 0));
		panel.setBackground(Color.white);
		JLabel name = new JLabel("  " + file.getName());
		name.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		name.setForeground( Color.BLACK );
		panel.add(name);
		panel.add(RowLayout.crlf());
		panel.add(new JLabel(description));
		panel.add(RowLayout.crlf());

		panel.add(new JLabel("      " + fileSize.getText() + " Byte"));
		panel.add(RowLayout.crlf());

		dp.add(cb);
		dp.add(panel);
		dp.add(RowLayout.crlf());
	}

}
