/*---------------------------------------------------------------
 *  Copyright 2014 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package client;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ui.RowLayout;
import org.rsna.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;

public class FileName implements Comparable<FileName> {

    private final File file;
    private final FileSize fileSize;
    private final FileCheckBox cb;
    private final StatusText statusText;
    private final LinkedList<String> listRoiRefFrameOfRef;
    private final LinkedList<String> listRefFrameOfRefFrameOfRefUID;
    private final LinkedList<String> listROINames;
    private final LinkedList<String> listROINumber;
    private final LinkedList<String> listRefROINumber;
    private final LinkedList<String> listROIObservationLabel;
    private final LinkedList<String> listROIInterpretedType;
    private final LinkedList<String> listRefRTPlanSequence;
    private String patientName = "";
    private String patientID = "";
    private String studyInstanceUID = "";
    private String seriesInstanceUID = "";
    private String studyDate = "";
    private String seriesDate = "";
    private String modality = "";
    private int seriesNumberInt = 0;
    private int acquisitionNumberInt = 0;
    private int instanceNumberInt = 0;
    private boolean isDICOM = false;
    private boolean isImage = false;
    private String studyDescription = "";
    private String seriesDescription = "";
    private String frameOfReference = "";
    private boolean oncentraCheck = false;
    private String refSOPClassUID = "";
    private String refStructSOPInst = "";
    private String refDoseSOPInst = "";
    private DicomObject dob;
    private String rtDoseComment = "";
    private String rtPlanLabel = "";
    private String rtPlanName = "";
    private String rtPlanDescription = "";
    private String ssLabel = "";
    private String ssName = "";
    private String ssDescription = "";
    private String rtImageName = "";
    private String rtImageLabel = "";
    private String rtImageDescription = "";

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
        listRefRTPlanSequence = new LinkedList<String>();
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
            switch (modality) {
                case "RTDOSE":
                    seriesDescription = fixNull(dob.getElementValue(Tags.DoseComment));
                    rtDoseComment = seriesDescription;
                    seriesDate = fixDate(dob.getElementValue(Tags.InstanceCreationDate));

                    // for RTDOSE -> RTPLAN check
                    DcmElement refRTPlanSeq = dob.getDataset().get(Tags.RefRTPlanSeq);
                    if (refRTPlanSeq != null) {
                        for (int i = 0; i < refRTPlanSeq.countItems(); i++) {
                            Dataset dcm = refRTPlanSeq.getItem(i);
                            listRefRTPlanSequence.add(dcm.getString(Tags.RefSOPInstanceUID));
                        }
                    }

                    break;
                case "RTPLAN":
                    seriesDescription = fixNull(dob.getElementValue(Tags.RTPlanLabel));
                    rtPlanLabel = seriesDescription;
                    if (seriesDescription.equals("")) {
                        seriesDescription = fixNull(dob.getElementValue(Tags.RTPlanName));
                        rtPlanName = seriesDescription;
                    }
                    if (seriesDescription.equals("")) {
                        seriesDescription = fixNull(dob.getElementValue(Tags.RTPlanDescription));
                        rtPlanDescription = seriesDescription;
                    }
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
                    break;
                case "RTSTRUCT":
                    seriesDescription = fixNull(dob.getElementValue(Tags.StructureSetLabel));
                    ssLabel = seriesDescription;
                    String temp = fixNull(dob.getElementValue(Tags.StructureSetName));
                    ssName = temp;
                    if (seriesDescription.equals("")) {
                        seriesDescription += temp;
                    } else {
                        seriesDescription += " - " + temp;
                    }

                    if (seriesDescription.equals("")) {
                        seriesDescription = fixNull(dob.getElementValue(Tags.StructureSetDescription));
                        ssDescription = seriesDescription;
                    }
                    seriesDate = fixDate(dob.getElementValue(Tags.StructureSetDate));


                    break;
                case "RTIMAGE":
                    seriesDescription = fixNull(dob.getElementValue(Tags.RTImageName));
                    rtImageName = seriesDescription;
                    if (seriesDescription.equals("")) {
                        seriesDescription = fixNull(dob.getElementValue(Tags.RTImageLabel));
                        rtImageLabel = seriesDescription;
                    }
                    if (seriesDescription.equals("")) {
                        seriesDescription = fixNull(dob.getElementValue(Tags.RTImageDescription));
                        rtImageDescription = seriesDescription;
                    }
                    seriesDate = fixDate(dob.getElementValue(Tags.InstanceCreationDate));
                    break;
            }

            if (seriesDescription.equals("")) {
                seriesDescription = fixNull(dob.getSeriesDescription());
            }
            if (seriesDate.equals("")) {
                seriesDate = fixDate(dob.getElementValue(Tags.SeriesDate));
            }


            if (modality.equals("RTSTRUCT")) {
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
                            for (int y = 0; y < FrameOfReferenceRelationshipSeq.countItems(); y++) {
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
                if (roiContourSeq != null) {
                    for (int i = 0; i < roiContourSeq.countItems(); i++) {
                        Dataset dcm = roiContourSeq.getItem(i);
                        DcmElement contourSeq = dcm.get(Tags.ContourSeq);
                        for (int j = 0; j < contourSeq.countItems(); j++) {
                            Dataset dc = contourSeq.getItem(j);
                            DcmElement contourImageSeq = dc.get(Tags.ContourImageSeq);
                            for (int k = 0; k < contourImageSeq.countItems(); k++) {
                                Dataset d = contourImageSeq.getItem(k);
                                refSOPClassUID = d.getString(Tags.RefSOPClassUID);
                            }
                        }
                    }
                }
            }
        } catch (Exception nonDICOM) {
        }
    }

    public String getRtPlanLabel() {
        return rtPlanLabel;
    }

    public String getRtPlanName() {
        return rtPlanName;
    }

    public String getRtPlanDescription() {
        return rtPlanDescription;
    }

    public String getSsLabel() {
        return ssLabel;
    }

    public String getSsName() {
        return ssName;
    }

    public String getSsDescription() {
        return ssDescription;
    }

    public String getRtImageName() {
        return rtImageName;
    }

    public String getRtImageLabel() {

        return rtImageLabel;
    }

    public String getRtImageDescription() {

        return rtImageDescription;
    }

    public String getRtDoseComment() {
        return rtDoseComment;
    }

    public String getSOPInstanceUID() {
        return dob.getSOPInstanceUID();
    }

    public String getSOPClassUID() {
        return dob.getSOPClassUID();
    }

    public String getRefSOPClassUID() {
        return refSOPClassUID;
    }

    public String getRefStructSOPInst() {
        return refStructSOPInst;
    }

    public String getRefDoseSOPInst() {
        return refDoseSOPInst;
    }

    public String getPatientGender() {
        return dob.getElementValue(Tags.PatientSex);
    }

    public boolean frameOfRefsOK() {
        return listRefFrameOfRefFrameOfRefUID.size() <= 1 || oncentraCheck;
    }

    public boolean hasROIonlyOneRefFrameOfRefUID() {
        return listRoiRefFrameOfRef.size() <= 1;
    }

    public String getROIRefFrameOfRef() {
        if (listRoiRefFrameOfRef.size() == 1) {
            return listRoiRefFrameOfRef.getFirst();
        }
        return null;
    }

    public LinkedList<String> getListRefRTPlanSequence() {
        return listRefRTPlanSequence;
    }

    public LinkedList<String> getListRoiRefFrameOfRef() {
        return listRoiRefFrameOfRef;
    }

    public LinkedList<String> getROINameList() {
        return listROINames;
    }

    public LinkedList<String> getROINumberList() {
        return listROINumber;
    }

    public LinkedList<String> getRefROINumberList() {
        return listRefROINumber;
    }

    public LinkedList<String> getROIObservationLabelList() {
        return listROIObservationLabel;
    }

    public LinkedList<String> getROIInterpretedTypeList() {
        return listROIInterpretedType;
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

    public String getPatientBirthday() {
        return dob.getElementValue(Tags.PatientBirthDate);
    }

    public String getPatientID() {
        return patientID;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getSeriesDate() {
        return seriesDate;
    }

    public String getModality() {
        return modality;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public String getFrameOfReference() {
        return frameOfReference;
    }

    public String getSeriesDescription() {
        return seriesDescription;
    }

    private String fixDate(String s) {
        if (s == null) s = "";
        if (s.length() == 8) {
            s = s.substring(0, 4) + "." + s.substring(4, 6) + "." + s.substring(6);
        }
        return s;
    }

    public int compareTo(FileName fn) {
        if (fn == null) return 0;
        int c;
        if ((c = this.patientID.compareTo(fn.patientID)) != 0) return c;
        if ((c = this.studyDate.compareTo(fn.studyDate)) != 0) return c;
        if ((c = this.studyInstanceUID.compareTo(fn.studyInstanceUID)) != 0) return c;
        if ((c = this.seriesNumberInt - fn.seriesNumberInt) != 0) return c;
        if ((c = this.acquisitionNumberInt - fn.acquisitionNumberInt) != 0) return c;
        if ((c = this.instanceNumberInt - fn.instanceNumberInt) != 0) return c;
        return 0;
    }

    public boolean isDICOM() {
        return isDICOM;
    }

    public boolean isImage() {
        return isImage;
    }

    public boolean isSelected() {
        return cb.isSelected();
    }

    public void setSelected(boolean selected) {
        cb.setSelected(selected);
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
        name.setFont(new Font("Monospaced", Font.PLAIN, 12));
        name.setForeground(Color.BLACK);
        panel.add(name);
        panel.add(RowLayout.crlf());
        String description = "";
        panel.add(new JLabel(description));
        panel.add(RowLayout.crlf());

        panel.add(new JLabel("      " + fileSize.getText() + " Byte"));
        panel.add(RowLayout.crlf());

        dp.add(cb);
        dp.add(panel);
        dp.add(RowLayout.crlf());
    }

}
