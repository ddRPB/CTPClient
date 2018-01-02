/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import javax.swing.*;

public class StudyName extends JButton {

	int numberOfSeries = 0;
	String name = "";
	String classification = "";
	FileName fn = null;
	LinkedList<String> modalities = null;

	public StudyName(FileName fileName) {
		super();
		fn = fileName;
		updateName();
		setFont( new Font( "Monospaced", Font.BOLD, 17 ) );
		setForeground( Color.blue );
		setBorder(BorderFactory.createEmptyBorder());
		setBorderPainted(false);
		setMargin(new Insets(0, 0, 0, 0));
		setContentAreaFilled(false);
		setFocusPainted(false);
		modalities = new LinkedList<String>();
	}

	public void setNumberOfSeries(int i) {
		numberOfSeries = i;
		updateName();
	}

	public void addSeriesModality(String modality) {
		if (!modalities.contains(modality)) {
			modalities.add(modality);
		}
	}

	public void setClassification() {
		if (modalities.contains("CT")) {
			if (modalities.contains("PT")) {
				classification = "PET-CT";
			}
			else if (modalities.contains("RTSTRUCT")) {
				if (modalities.contains("RTPLAN")
						&& modalities.contains("RTDOSE")) {
					classification = "TreatmentPlan";
				}
				else{
					classification = "Contouring";
				}
			}
		}
		else {
			classification = "";
			modalities.remove();
		}
		updateName();
	}

	public void updateName() {
		name = "[" + classification + "]"
				+ " " +	fn.getStudyDescription()
				+ " " + "<Series=" + String.valueOf(numberOfSeries) + ">"
				+ " " + "STUDY"
				+ " " + fn.getStudyDate();
		setText(name);
	}
}
