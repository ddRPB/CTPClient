/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import javax.swing.*;
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
	String structureSetROISequence = "";

	public FileName(File file) {
		this.file = file;
		cb = new FileCheckBox();
		statusText = new StatusText();
		fileSize = new FileSize(file);
		try {
			DicomObject dob = new DicomObject(file);
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
			seriesDescription = fixNull(dob.getSeriesDescription());

			seriesDate = fixDate(dob.getElementValue(524321));

			/*ByteBuffer bb = null;
			bb = dob.getElementByteBuffer(805699616);
			//Charset charset = Charset.forName("UTF-8");
			//structureSetROISequence = charset.decode(bb).toString();
			structureSetROISequence = String.valueOf(bb.capacity());
*/
/*
			byte[] byteArray = dob.getElementBytes(805699616);
			structureSetROISequence = new String(String.valueOf(byteArray.length));
*/


			//"[3006,0020}"
			//"(3006,0020)"
			//"StructureSetROISequence"
			//0x30060020
			//805699616
			//structureSetROISequence = dob.getElementValue("StructureSetROISequence", "myDefaultString");

			//--> all this approaches work with other non sequence tags...


		/*	if (isImage) {
				description += "image";
				description += getText("Series:", seriesNumber, " ");
				description += getText("Acquisition:", acquisitionNumber, " ");
				description += getText("Image:", instanceNumber, "");
			}
			else description += fixNull(dob.getSOPClassName());
		*/
		}
		catch (Exception nonDICOM) { }
	}

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

	public String getSeriesDescription() { return seriesDescription; }

	private String fixDate(String s) {
		if (s == null) s = "";
		if (s.length() == 8) {
			s = s.substring(0,4) + "." + s.substring(4,6) + "." + s.substring(6);
		}
		return s;
	}

	private String getText(String prefix, String s, String suffix) {
		if (s.length() != 0) s = prefix + s + suffix;
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

		panel.add(new JLabel("SSRS: " + structureSetROISequence));
		panel.add(RowLayout.crlf());

		dp.add(cb);
		dp.add(panel);
		dp.add(RowLayout.crlf());
	}

}
