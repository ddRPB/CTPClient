/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.rsna.ui.RowLayout;

public class Series implements ActionListener, Comparable<Series> {

    LinkedList<FileName> list = null;
    SeriesCheckBox cb = null;
    SeriesName seriesName = null;
    String patientName = null;

    public Series(FileName fileName) {
        list = new LinkedList<FileName>();
        cb = new SeriesCheckBox();
        cb.addActionListener(this);
        patientName = fileName.getPatientName();
        seriesName = new SeriesName(fileName);
        seriesName.addActionListener(this);
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
            cb.doClick();
        }
    }

    public int compareTo(Series series) {
        if (series == null)  return 0;
        int c;
        if ( (c = this.patientName.compareTo(series.getPatientName())) != 0 ) return c;
        return 0;
    }

    public SeriesCheckBox getCb() { return cb; }

    public String getPatientName() { return patientName; }

    public boolean isSelected() { return cb.isSelected(); }

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
    }

    public void deselectAll() {
        DirectoryPanel dp = getDirectoryPanel();
        for (FileName fn : list) {
            fn.setSelected(false);
            if (dp != null) dp.setRowVisible(fn.getCheckBox(), false);
        }
    }

    private DirectoryPanel getDirectoryPanel() {
        Component container = cb.getParent();
        if ((container != null) && (container instanceof DirectoryPanel)) {
            return (DirectoryPanel)container;
        }
        return null;
    }

    public FileName[] getFileNames() {

        //Java 8 is needed
        Comparator<FileName> FileNameComparator
                = Comparator.comparing(FileName::getSeriesDescription);

        FileName[] names = new FileName[list.size()];
        names = list.toArray(names);
        //Arrays.sort(names);
        Arrays.sort(names, FileNameComparator);
        return names;
    }

    public void display(DirectoryPanel dp) {
//        dp.add(Box.createHorizontalStrut(1));
        dp.add(cb);
        dp.add(seriesName, RowLayout.span(4));
        dp.add(RowLayout.crlf());
        for (FileName fn : getFileNames()) {
            fn.display(dp);
        }
    }
}