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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.*;
import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.PixelScript;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;
import org.rsna.ctp.stdstages.dicom.SimpleDicomStorageSCP;
import org.rsna.server.HttpResponse;
import org.rsna.ui.GeneralAuthenticator;
import org.rsna.util.BrowserUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.HttpUtil;
import org.rsna.util.IPUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

@SuppressWarnings("ALL")
public class CTPClient extends JFrame implements ActionListener, ComponentListener {

    private static final String title = "CTP Client - DKTK";
    private static final Color bgColor = new Color(0xc6d8f9);
    final HashMap<String, String> siUIDtoNewDescription;
    final HashMap<String, String> tagToNewRTDesc;
    private final JScrollPane sp;
    private final DirectoryPanel dp;
    private final DialogPanel dialog;
    private final JCheckBox chkFiles;

    private final InputText httpURLField;
    private final FieldButton browseButton;
    private final FieldButton scpButton;
    private final FieldButton dialogButton;
    private final FieldButton helpButton;
    private final FieldButton startButton;
    private final FieldButton showLogButton;
    private final FieldButton instructionsButton;
    private final AttachedFrame instructionsFrame;
    private final boolean dfEnabled;
    private final boolean dpaEnabled;
    private final boolean showBrowseButton;
    private final boolean zip;
    private final boolean setBurnedInAnnotation;
    private final Properties config;
    private final String dfScript;
    private final PixelScript dpaPixelScript;
    private final IDTable idTable = new IDTable();
    private final boolean renameFiles;
    private final String dicomURL;
    private final String stowURL;
    private final String stowUsername;
    private final String stowPassword;
    private final String requiredStudyType;
    private final String requiredGender;
    private final String requiredPatientsBirthDate;
    private final String pseudonym;
    String newStudyDescription = null;
    private volatile boolean sending = false;
    private FieldButton showMemoryButton = null;
    private JFileChooser chooser = null;
    private DAScript daScript;
    private File daScriptFile;
    private String defaultKeyType = null;
    private File lookupTableFile;
    private File scpDirectory = null;
    private SimpleDicomStorageSCP scp = null;
    private String ipAddressString = "";
    private File exportDirectory = null;
    private StudyList studyList = null;
    private JButton studyDescriptionButton;
    private JButton seriesDescriptionButton;
    private JTextField newDescField;
    private JTextField descField;
    private JButton userOKButton;
    private JButton userCancelbutton;
    private JButton rtOKButton;
    private JButton rtCancelButton;
    private JFrame userDialogFrame;
    private JFrame rtDialogFrame;
    private DefaultTableModel seriesTableModel;
    private DefaultTableModel rtTableModel;
    JProgressBar pBar;
    LinkedList<String> rtList = new LinkedList<>();

    private CTPClient(String[] args) {
        super();
        System.setProperty("http.keepAlive", "false");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
        }

        Authenticator authenticator = new GeneralAuthenticator(this);
        Authenticator.setDefault(authenticator);

        //Get the configuration from the args.
        config = getConfiguration(args);

        //Set up the SCP directory
        int scpPort = StringUtil.getInt(config.getProperty("scpPort"), 0);
        if (scpPort > 0) {
            try {
                scpDirectory = File.createTempFile("TMP-", "");
                scpDirectory.delete();
                scpDirectory.mkdirs();
            } catch (Exception ignoreForNow) {
            }
        }

        //Get the DICOM export URL
        dicomURL = config.getProperty("dicomURL", "");

        //Get the DICOM STOWRS export parameters
        stowURL = config.getProperty("stowURL", "");
        stowUsername = config.getProperty("stowUsername", "");
        stowPassword = config.getProperty("stowPassword", "");

        //Set up the exportDirectory
        String expDir = config.getProperty("exportDirectory");
        if (expDir != null) exportDirectory = new File(expDir);
        renameFiles = config.getProperty("renameFiles", "no").equals("yes");

        //Get the button enables
        boolean showURL = !config.getProperty("showURL", "yes").equals("no");
        showBrowseButton = !config.getProperty("showBrowseButton", "yes").equals("no") || (scpPort <= 0);
        boolean showDialogButton = !config.getProperty("showDialogButton", "yes").equals("no");

        setTitle(config.getProperty("windowTitle"));
        JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);

        //Get the study type to crosscheck later on
        requiredStudyType = Objects.toString(config.getProperty("studyType"), "");
        //Get the birth date to crosscheck later on
        requiredPatientsBirthDate = Objects.toString(config.getProperty("birthDate"), "");

        pseudonym = Objects.toString(config.getProperty("pseudonym"), "");

        //Get the gender of the patient to crosscheck
        requiredGender = Objects.toString(config.getProperty("gender"), "");

        //Set the SSL params
        getKeystore();

        //Get the DialogPanel if specified
        dialog = getDialogPanel();

        //Get the anonymizer script
        getDAScript();

        //Get the LookupTable
        getLookupTable();

        //Get the PixelScript
        dpaPixelScript = getDPAPixelScriptObject();

        //Get the filter script
        dfScript = getDFScriptObject();

        //Get the zip parameter for HTTP export
        zip = config.getProperty("zip", "no").trim().equals("yes");

        //Set the enables
        dfEnabled = config.getProperty("dfEnabled", "no").trim().equals("yes");
        dpaEnabled = config.getProperty("dpaEnabled", "no").trim().equals("yes");
        setBurnedInAnnotation = config.getProperty("setBurnedInAnnotation", "no").trim().equals("yes");

        //Make the UI components
        String httpURL = config.getProperty("httpURL", "").trim();
        httpURLField = new InputText(httpURL);
        browseButton = new FieldButton("Open Local Folder");
        browseButton.setEnabled(true);
        browseButton.addActionListener(this);

        chkFiles = new JCheckBox("show Dicom Files");
        chkFiles.setEnabled(true);
        chkFiles.setBackground(bgColor);

        scpButton = new FieldButton("Open DICOM Storage");
        scpButton.setEnabled((scpPort > 0));
        scpButton.addActionListener(this);

        helpButton = new FieldButton("Help");
        helpButton.setEnabled(true);
        helpButton.addActionListener(this);

        startButton = new FieldButton("Start");
        startButton.setEnabled(false);
        startButton.addActionListener(this);

        siUIDtoNewDescription = new HashMap<String, String>();
        tagToNewRTDesc = new HashMap<String, String>();

        //Make the header panel
        JPanel header = new JPanel();
        header.setBackground(bgColor);
        header.add(new TitleLabel(config.getProperty("panelTitle")));
        header.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        panel.add(header, BorderLayout.NORTH);

        //Make a panel for the input fields and the progress display
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        main.setBackground(bgColor);

        //Put the input fields in a vertical Box
        Box vBox = Box.createVerticalBox();
        vBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        vBox.setBackground(bgColor);

        if (showURL) {
            Box destBox = Box.createHorizontalBox();
            destBox.setBackground(bgColor);
            destBox.add(new FieldLabel());
            destBox.add(Box.createHorizontalStrut(5));
            destBox.add(httpURLField);
            vBox.add(destBox);
            vBox.add(Box.createVerticalStrut(10));
        }

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.setBackground(bgColor);

        if ((dialog != null) && showDialogButton) {
            dialogButton = new FieldButton(dialog.getTitle());
            dialogButton.setEnabled(true);
            dialogButton.addActionListener(this);
            buttonBox.add(dialogButton);
            buttonBox.add(Box.createHorizontalStrut(20));
        } else dialogButton = new FieldButton("unused");

        if (showBrowseButton) {
            buttonBox.add(browseButton);
            if (scpPort > 0) buttonBox.add(Box.createHorizontalStrut(20));
        }

        if (scpPort > 0) {
            buttonBox.add(scpButton);
        }

        String helpURL = config.getProperty("helpURL", "").trim();
        if (!helpURL.equals("")) {
            buttonBox.add(Box.createHorizontalStrut(20));
            buttonBox.add(helpButton);
        }
        vBox.add(buttonBox);

        //Put the vBox in the north panel of the main panel
        main.add(vBox, BorderLayout.NORTH);

        //Put a DirectoryPanel in a scroll pane and put that in the center of the main panel
        dp = new DirectoryPanel();
        sp = new JScrollPane();
        sp.setViewportView(dp);
        sp.getVerticalScrollBar().setBlockIncrement(100);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        main.add(sp, BorderLayout.CENTER);

        //Put the start button in the south panel of the main panel
        JPanel startPanel = new JPanel();
        startPanel.add(startButton);
        startPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        startPanel.setBackground(bgColor);
        main.add(startPanel, BorderLayout.SOUTH);

        //Now put the main panel in the center of the frame
        panel.add(main, BorderLayout.CENTER);

        //Make the instructionsFrame
        int instructionsWidth = 425;
        instructionsFrame = new AttachedFrame("Instructions", instructionsWidth, bgColor);

        //Make a footer bar to display status.
        StatusPane status = StatusPane.getInstance(" ", bgColor);

        //ProgressBar
        pBar = new JProgressBar(0, 100);
        pBar.setVisible(false);
        pBar.setValue(0);
        pBar.setStringPainted(true);
        status.addRightComponent(pBar);

        if (config.getProperty("showMemory", "no").equals("yes")) {
            showMemoryButton = new FieldButton("Show Memory");
            showMemoryButton.addActionListener(this);
            status.addRightComponent(showMemoryButton);
        }
        showLogButton = new FieldButton("Show Log");
        showLogButton.addActionListener(this);
        showLogButton.setVisible(false);
        status.addRightComponent(showLogButton);
        instructionsButton = new FieldButton("Instructions");
        instructionsButton.addActionListener(this);
        status.addRightComponent(instructionsButton);
        //status.addRightComponent(chkFiles);
        panel.add(status, BorderLayout.SOUTH);

        //Catch close requests and check before closing if we are busy sending
        addWindowListener(new WindowCloser(this));

        pack();
        centerFrame();
        setVisible(true);

        //Start the SCP if so configured.
        if (scpPort > 0) {
            try {
                ipAddressString = IPUtil.getIPAddress() + ":" + scpPort + " [AET: CTP]";
                scp = new SimpleDicomStorageSCP(scpDirectory, scpPort);
                scp.start();
                status.setText("DICOM Storage SCP open on " + ipAddressString + ".");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to start the\nDICOM Storage SCP on\n" + ipAddressString);
                System.exit(0);
            }
        }

        //Now that everything is set up, set the text of the
        //instructions to make it correspond to the configuration
        //and display the frame
        instructionsFrame.setText(getInstructions());
        instructionsFrame.attachTo(this);
        instructionsFrame.setVisible(false);
        this.requestFocus();
        addComponentListener(this);
    }

    public static void main(String[] args) {
        Logger.getRootLogger().addAppender(
                new ConsoleAppender(
                        new PatternLayout("%d{HH:mm:ss} %-5p [%c{1}] %m%n")));
        Logger.getRootLogger().setLevel(Level.WARN);
        Logger.getLogger("org.rsna").setLevel(Level.INFO);
        new CTPClient(args);
    }

    //.Implement the ActionListener interface
    @SuppressWarnings("SpellCheckingInspection")
    public void actionPerformed(ActionEvent event) {
        boolean radioMode = (dialog != null) && dialog.studyMode();
        boolean anio = getAcceptNonImageObjects();
        Object source = event.getSource();
        if (source.equals(browseButton)) {
            File dir = getDirectory();
            if (dir != null) {
                setWaitCursor(true);
                dp.clear();
                dp.setDeleteOnSuccess(false);
                studyList = new StudyList(dir, radioMode, anio, chkFiles.isSelected());
                studyList.setRequiredStudyType(requiredStudyType);
                studyList.display(dp);
                studyList.selectFirstStudy();
                startButton.setEnabled(true);
                if (studyList.isStudyTypeWrong()) {
                    startButton.setEnabled(false);
                    showSelectionInfo("studyType");
                } else if (studyList.wrongReferences) {
                    startButton.setEnabled(false);
                    showSelectionInfo("references");
                } else if ((requiredPatientsBirthDate.length() == 4
                        && !requiredPatientsBirthDate.equals(studyList.getPatientBirthYear()))
                        || (requiredPatientsBirthDate.length() == 8
                        && !requiredPatientsBirthDate.equals(studyList.getPatientBirthDate()))){
                    startButton.setEnabled(false);                    showSelectionInfo("birthdate");
                } else if (!requiredGender.equals(studyList.getPatientsGender())
                        && !requiredGender.equals("") &&
                        !studyList.getPatientsGender().equals("")) {
                    startButton.setEnabled(false);
                    showSelectionInfo("gender");
                }
                sp.getVerticalScrollBar().setValue(0);
                setWaitCursor(false);
            } else startButton.setEnabled(false);
        } else if (source.equals(scpButton)) {
            if (scpDirectory != null) {
                setWaitCursor(true);
                dp.clear();
                dp.setDeleteOnSuccess(true);
                studyList = new StudyList(scpDirectory, radioMode,
                        anio, chkFiles.isSelected());
                studyList.setRequiredStudyType(requiredStudyType);
                studyList.display(dp);
                studyList.selectFirstStudy();
                startButton.setEnabled(true);
                if (studyList.isStudyTypeWrong()) {
                    startButton.setEnabled(false);
                    showSelectionInfo("studyType");
                } else if (studyList.wrongReferences) {
                    startButton.setEnabled(false);
                    showSelectionInfo("references");
                } else if ((requiredPatientsBirthDate.length() == 4
                        && !requiredPatientsBirthDate.equals(studyList.getPatientBirthYear()))
                        || (requiredPatientsBirthDate.length() == 8
                        && !requiredPatientsBirthDate.equals(studyList.getPatientBirthDate()))){
                    startButton.setEnabled(false);
                    showSelectionInfo("birthdate");
                }
                sp.getVerticalScrollBar().setValue(0);
                setWaitCursor(false);
            }
        } else if (source.equals(dialogButton)) {
            displayDialog();
        } else if (source.equals(startButton)) {
            if (displayDialog()) {

                Study[] studies = studyList.getStudies();
                int numberOfSelectedStudies = 0;
                for (Study study : studies) {
                    if (study.isSelected()) {
                        numberOfSelectedStudies++;
                    }
                }
                if (numberOfSelectedStudies != 1) {
                    final JFrame parent = this;
                    Runnable enable = new Runnable() {

                        public void run() {
                            JOptionPane.showMessageDialog(parent,
                                    "You have to select exactly one study in order to proceed.",
                                    "Information", JOptionPane.WARNING_MESSAGE);
                        }
                    };
                    SwingUtilities.invokeLater(enable);

                }
                else {

                    startButton.setEnabled(false);
                    dialogButton.setEnabled(false);
                    browseButton.setEnabled(false);
                    scpButton.setEnabled(false);
                    sending = true;

                    //show window to change study/series descriptions
                    this.setEnabled(false);
                    this.setVisible(false);
                    showUserDialog();
                }

            }
        } else if (source.equals(helpButton)) {
            String helpURL = config.getProperty("helpURL");
            if (!helpURL.equals("")) {
                BrowserUtil.openURL(helpURL);
            }
        } else if (source.equals(showMemoryButton)) {
            showMemory();
        } else if (source.equals(showLogButton)) {
            showLog();
        } else if (source.equals(instructionsButton)) {
            instructionsFrame.attachTo(this);
            instructionsFrame.setVisible(true);
            this.requestFocus();
        } else if (source.equals(studyDescriptionButton)) {
            newDescField.setText(descField.getText());

        } else if (source.equals(userOKButton)) {
            // the new Study Description
            newStudyDescription = newDescField.getText();

            // the new Series Description
            for (int i = 0; i < seriesTableModel.getRowCount(); i++) {
                siUIDtoNewDescription.put(seriesTableModel.getValueAt(i, 3).toString(),
                        seriesTableModel.getValueAt(i, 2).toString());
            }

            userDialogFrame.setEnabled(false);
            userDialogFrame.setVisible(false);

            showRTDialog();

        } else if (source.equals(seriesDescriptionButton)) {
            for (int i = 0; i < seriesTableModel.getRowCount(); i++) {
                seriesTableModel.setValueAt(seriesTableModel.getValueAt(i, 1), i, 2);
            }
        } else if (source.equals(userCancelbutton)) {
            userDialogFrame.dispose();
            this.setEnabled(true);
            this.setVisible(true);
            startButton.setEnabled(true);
            dialogButton.setEnabled(true);
            browseButton.setEnabled(true);
            scpButton.setEnabled(true);
            sending = false;
        } else if (source.equals(rtOKButton)) {
            rtDialogFrame.setEnabled(false);
            rtDialogFrame.setVisible(false);

            for (int i = 0; i < rtTableModel.getRowCount(); i++) {
                tagToNewRTDesc.put(rtTableModel.getValueAt(i, 0).toString(),
                        rtTableModel.getValueAt(i, 2).toString());
            }

            this.setEnabled(true);
            this.setVisible(true);
            SenderThread sender = new SenderThread(this);
            sender.start();

        } else if (source.equals(rtCancelButton)) {
            rtDialogFrame.dispose();
            userDialogFrame.setEnabled(true);
            userDialogFrame.setVisible(true);
            sending = false;
        }
    }

    private void setWaitCursor(boolean on) {
        if (on) setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void showMemory() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        StringBuffer sb = new StringBuffer();
        Formatter formatter = new Formatter(sb);
        sb.append("Memory in use: ");
        formatter.format("%,d bytes", usedMemory);
        sb.append("\n");
        sb.append("JVM memory: ");
        formatter.format("%,d bytes", totalMemory);
        sb.append("\n");
        sb.append("Max memory: ");
        formatter.format("%,d bytes", maxMemory);
        sb.append("\n");

        JOptionPane.showMessageDialog(this, sb.toString(), "Memory", JOptionPane.PLAIN_MESSAGE);
    }

    private void showLog() {
        String text = Log.getInstance().getText().trim();
        if (text.equals("")) text = "The log is empty.";
        JOptionPane.showMessageDialog(this, text, "Log", JOptionPane.PLAIN_MESSAGE);
    }

    //Implement the ComponentListener interface
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
        setInstructionsPosition();
    }

    public void componentResized(ComponentEvent e) {
        setInstructionsPosition();
    }

    public void componentShown(ComponentEvent e) {
    }

    private void setInstructionsPosition() {
        if (instructionsFrame.isVisible()) {
            instructionsFrame.attachTo(this);
        }
    }

    private String getInstructions() {
        StringBuffer sb = new StringBuffer();
        sb.append("<center><h1>Instructions</h1></center><hr/>\n");
        if ((scp != null)) {
            sb.append("<h2>To process and export images received from a PACS or workstation:</h2>\n");
            sb.append("<ol>");
            sb.append("<li>Send images to <b>").append(ipAddressString).append("</b>\n");
            sb.append("<li>Click the <b>Open DICOM Storage</b> button.\n");
            sb.append("<li>Check the boxes of the images to be processed.\n");
            sb.append("<li>Click the <b>Start</b> button.\n");
            if (dialog != null) {
                sb.append("<li>Fill in the fields in the dialog.\n");
                sb.append("<li>Click <b>OK</b> on the dialog.\n");
            }
            sb.append("</ol>");
        }
        if (showBrowseButton) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("<h2>To process and export images stored on this computer:</h2>\n");
            sb.append("<ol>");
            sb.append("<li>Click the <b>Open Local Folder</b> button\n");
            sb.append("<li>Navigate to a folder containing dicom files.\n");
            sb.append("<li>Click <b>OK</b> on the file dialog.\n");
            sb.append("<li>Check the boxes of the series to be processed.\n");
            sb.append("<li>Click the <b>Start</b> button.\n");
            if (dialog != null) {
                sb.append("<li>Fill in the fields in the dialog.\n");
                sb.append("<li>Click <b>OK</b> on the dialog.\n");
            }
            sb.append("</ol>");
        }
        return sb.toString();
    }

    private boolean displayDialog() {
        if (dialog != null) {
            int result = JOptionPane.showOptionDialog(
                    this,
                    dialog,
                    dialog.getTitle(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, //icon
                    null, //options
                    null); //initialValue
            if (result == JOptionPane.OK_OPTION) {
                //Set the field values in the configuration
                dialog.setProperties(config);
                return true;
            } else return false;
        }
        return true;
    }

    public void transmissionComplete() {
        sending = false;
        final JFrame parent = this;
        Runnable enable = new Runnable() {

            public void run() {
                scpButton.setEnabled(true);
                browseButton.setEnabled(true);
                dialogButton.setEnabled(true);
                startButton.setEnabled(true);
                JOptionPane.showMessageDialog(parent,
                        "The selection has been processed..",
                        "Processing Complete", JOptionPane.INFORMATION_MESSAGE);
                WindowEvent wev = new WindowEvent(parent, WindowEvent.WINDOW_CLOSING);
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
            }
        };
        SwingUtilities.invokeLater(enable);
    }

    private void showSelectionInfo(String infoType) {
        final JFrame parent = this;
        String infoString = "";

        switch (infoType) {
            case "studyType": {
                infoString = "The files do not comply with the required study type.\n" +
                        "Please choose another study.";
                break;
            }
            case "references": {
                infoString = "The files do not refer to each other (in relation to the required study type).\n" +
                        "Please choose another study.";
                break;
            }
            case "birthdate": {
                infoString = "The Patient's Birth Date of the files does not match the required one.\n" +
                        "Please choose another patient.";
                break;
            }
            case "gender": {
                infoString = "The Patient's gender of the files does not match the required one.\n" +
                        "Please choose another patient.";
                break;
            }
            default: {
                infoString = "Unknown Information.";
                break;
            }
        }

        String finalInfoString = infoString;
        Runnable enable = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(parent,
                        finalInfoString,
                        "Information", JOptionPane.WARNING_MESSAGE);
            }
        };
        SwingUtilities.invokeLater(enable);
    }

    public void showRTDialog() {

        rtDialogFrame = new JFrame("RT - Dialog");
        rtDialogFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] columnNames = {"Tag",
                "original Description",
                "new Description"};

        Object[][] tempData = new Object[rtList.size() / 3][3];
        for (int i = 0; i < rtList.size(); i++) {
            tempData[i / 3][i % 3] = rtList.get(i);
        }

        rtTableModel = new DefaultTableModel(tempData, columnNames){
            @Override
            public boolean isCellEditable(int row, int column) {
                // make read only fields except column 3
                return column == 2;
            }
        };

        JTable rtTable = new JTable(rtTableModel);
        rtTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TableCellRenderer renderer = new EvenOddRenderer();
        rtTable.setDefaultRenderer(Object.class, renderer);
        rtTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        rtTable.getColumnModel().getColumn(0).setMinWidth(150);
        rtTable.getColumnModel().getColumn(1).setMinWidth(150);
        rtTable.getColumnModel().getColumn(2).setMinWidth(150);

        JScrollPane rtTableScrollPane= new JScrollPane(rtTable);
        rtTable.setFillsViewportHeight(true);

        /*-------------------------------------------------------------*/

        rtOKButton = new JButton("OK");
        rtOKButton.addActionListener(this);
        rtOKButton.setMinimumSize(new Dimension(this.getWidth(), 24));

        /*-------------------------------------------------------------*/

        rtCancelButton = new JButton("Cancel");
        rtCancelButton.addActionListener(this);
        rtCancelButton.setMinimumSize(new Dimension(this.getWidth(), 24));

        JPanel naviButtonPanel = new JPanel();
        naviButtonPanel.setLayout(new FlowLayout());
        naviButtonPanel.add(rtOKButton);
        naviButtonPanel.add(rtCancelButton);
        /*-------------------------------------------------------------*/
        rtDialogFrame.add(rtTableScrollPane, gbc);
        rtDialogFrame.add(naviButtonPanel);

        rtDialogFrame.setMinimumSize(new Dimension(this.getWidth(), this.getHeight()));
        rtDialogFrame.setLocation(this.getLocation());
        rtDialogFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rtDialogFrame.setVisible(true);
    }

    public void showUserDialog () {

        Study studies[] = studyList.getStudies();
        Study selectedStudy = null;
        for (Study s : studies) {
            if (s.isSelected()) {
                selectedStudy = s;
            }
        }

        userDialogFrame = new JFrame("Selected DICOM Study");
        userDialogFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel dicomPatientPanel = new JPanel();
        dicomPatientPanel.setLayout(new BoxLayout(dicomPatientPanel, BoxLayout.Y_AXIS));

        TitledBorder dicomPatientBorder = new TitledBorder("DICOM Patient");
        dicomPatientBorder.setTitleJustification(TitledBorder.LEFT);
        dicomPatientBorder.setTitlePosition(TitledBorder.TOP);
        dicomPatientPanel.setBorder(dicomPatientBorder);

        JPanel idPanel = new JPanel();
        idPanel.setLayout(new FlowLayout());

        JLabel dicomPatientLabel = new JLabel("ID:         ");
        JTextField idField = new JTextField(selectedStudy.getPatientID());
        idField.setEnabled(false);
        idField.setBackground(Color.PINK);
        idField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel01 = new JLabel(" -> ");
        JTextField newIdField = new JTextField();
        newIdField.setText(pseudonym);
        newIdField.setEnabled(false);
        newIdField.setBackground(Color.GREEN);
        newIdField.setPreferredSize(new Dimension(250, 24));

        idPanel.add(dicomPatientLabel);
        idPanel.add(idField);
        idPanel.add(arrowLabel01);
        idPanel.add(newIdField);
        dicomPatientPanel.add(idPanel);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new FlowLayout());

        JLabel dicomNameLabel = new JLabel("Name:    ");

        JTextField nameField = new JTextField(selectedStudy.getPatientname());
        nameField.setEnabled(false);
        nameField.setBackground(Color.PINK);
        nameField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel02 = new JLabel(" -> ");
        JTextField newNameField = new JTextField();
        newNameField.setText(pseudonym);
        newNameField.setBackground(Color.GREEN);
        newNameField.setPreferredSize(new Dimension(250, 24));

        namePanel.add(dicomNameLabel);
        namePanel.add(nameField);
        namePanel.add(arrowLabel02);
        namePanel.add(newNameField);
        dicomPatientPanel.add(namePanel);

        JPanel genderPanel = new JPanel();
        genderPanel.setLayout(new FlowLayout());

        JLabel dicomGenderLabel = new JLabel("Gender:  ");
        JTextField genderField = new JTextField(selectedStudy.getGender());
        genderField.setEnabled(false);
        genderField.setBackground(Color.PINK);
        genderField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel03 = new JLabel(" -> ");
        JTextField newGenderField = new JTextField();
        newGenderField.setBackground(Color.GREEN);
        newGenderField.setPreferredSize(new Dimension(250, 24));

        genderPanel.add(dicomGenderLabel);
        genderPanel.add(genderField);
        genderPanel.add(arrowLabel03);
        genderPanel.add(newGenderField);
        dicomPatientPanel.add(genderPanel);

        JPanel dobPanel = new JPanel();
        dobPanel.setLayout(new FlowLayout());

        JLabel dicomDOBLabel = new JLabel("DOB:      ");
        JTextField dobField = new JTextField(selectedStudy.getPatientBirthDate());
        dobField.setEnabled(false);
        dobField.setBackground(Color.PINK);
        dobField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel04 = new JLabel(" -> ");
        JTextField newDOBField = new JTextField();
        newDOBField.setText("19000101");
        newDOBField.setBackground(Color.GREEN);
        newDOBField.setPreferredSize(new Dimension(250, 24));

        dobPanel.add(dicomDOBLabel);
        dobPanel.add(dobField);
        dobPanel.add(arrowLabel04);
        dobPanel.add(newDOBField);
        dicomPatientPanel.add(dobPanel);


        /*-------------------------------------------------------------*/

        JPanel dicomStudyPanel = new JPanel();
        dicomStudyPanel.setLayout(new BoxLayout(dicomStudyPanel, BoxLayout.Y_AXIS));

        TitledBorder dicomStudyBorder = new TitledBorder("DICOM Study");
        dicomStudyBorder.setTitleJustification(TitledBorder.LEFT);
        dicomStudyBorder.setTitlePosition(TitledBorder.TOP);
        dicomStudyPanel.setBorder(dicomStudyBorder);

        JPanel typePanel = new JPanel();
        typePanel.setLayout(new FlowLayout());

        JLabel dicomTypeLabel = new JLabel("Study type: ");
        JTextField typeField = new JTextField(selectedStudy.getStudyType());
        typeField.setEnabled(false);
        typeField.setBackground(Color.PINK);
        typeField.setPreferredSize(new Dimension(250, 24));

        typePanel.add(dicomTypeLabel);
        typePanel.add(typeField);
        dicomStudyPanel.add(typePanel);

        JPanel descPanel = new JPanel();
        descPanel.setLayout(new FlowLayout());

        JLabel dicomDescLabel = new JLabel("Description: ");
        descField = new JTextField(selectedStudy.getStudyDescription());
        descField.setEnabled(false);
        descField.setBackground(Color.PINK);
        descField.setPreferredSize(new Dimension(250, 24));
        studyDescriptionButton = new JButton(" -> ");
        studyDescriptionButton.addActionListener(this);
        newDescField = new JTextField();
        newDescField.setBackground(Color.GREEN);
        newDescField.setPreferredSize(new Dimension(250, 24));

        descPanel.add(dicomDescLabel);
        descPanel.add(descField);
        descPanel.add(studyDescriptionButton);
        descPanel.add(newDescField);
        dicomStudyPanel.add(descPanel);

        /*-------------------------------------------------------------*/

        JPanel dicomSeriesPanel = new JPanel();
        dicomSeriesPanel.setLayout(new BoxLayout(dicomSeriesPanel, BoxLayout.Y_AXIS));

        TitledBorder dicomSeriesBorder = new TitledBorder("DICOM Study Series");
        dicomSeriesBorder.setTitleJustification(TitledBorder.LEFT);
        dicomSeriesBorder.setTitlePosition(TitledBorder.TOP);
        dicomSeriesPanel.setBorder(dicomSeriesBorder);

        JPanel seriesButtonPanel = new JPanel();
        seriesButtonPanel.setLayout(new FlowLayout());
        seriesDescriptionButton = new JButton(" -> ");
        seriesDescriptionButton.addActionListener(this);
        seriesButtonPanel.add(seriesDescriptionButton);

        String[] columnNames = {"Modality",
                "original Description",
                "new Description",
                "siUID"};

        // Parse the Studylist
        LinkedList<String> dataList = new LinkedList<>();
        rtList = new LinkedList<>();
        for (Study study : studies) {
            Series[] series = study.getSeries();
            for (Series s : series) {
                String modality = "";
                String siUID = "";
                if (s.isSelected()) {
                    modality = s.getSeriesName().getModality();
                    dataList.add(modality);
                    dataList.add(s.getSeriesDescription());
                    dataList.add("");
                    siUID = s.getSeriesName().getSeriesInstanceUID();
                    dataList.add(siUID);

                    //for rtRenaming
                    switch (modality) {
                        case "RTDOSE":
                            rtList.add("DoseComment");
                            rtList.add(s.getRTDoseComment());
                            rtList.add("");
                            break;
                        case "RTPLAN":
                            rtList.add("RTPlanLabel");
                            rtList.add(s.getRTPlanLabel());
                            rtList.add("");

                            rtList.add("RTPlanName");
                            rtList.add(s.getRTPlanName());
                            rtList.add("");

                            rtList.add("RTPlanDescription");
                            rtList.add(s.getRTPlanDescription());
                            rtList.add("");
                            break;
                        case "RTSTRUCT":
                            rtList.add("StructureSetLabel");
                            rtList.add(s.getSSLabel());
                            rtList.add("");

                            rtList.add("StructureSetName");
                            rtList.add(s.getSSName());
                            rtList.add("");

                            rtList.add("StructureSetDescription");
                            rtList.add(s.getSSDescription());
                            rtList.add("");
                            break;
                        case "RTIMAGE":
                            rtList.add("RTImageName");
                            rtList.add(s.getRTImageName());
                            rtList.add("");

                            rtList.add("RTImageLabel");
                            rtList.add(s.getRTImageLabel());
                            rtList.add("");

                            rtList.add("RTImageDescription");
                            rtList.add(s.getRTImageDescription());
                            rtList.add("");
                            break;
                        default:

                            break;
                    }

                }
            }
        }

        Object[][] tempData = new Object[dataList.size() / 4][4];
        for (int i = 0; i < dataList.size(); i++) {
            tempData[i / 4][i % 4] = dataList.get(i);
        }

        seriesTableModel = new DefaultTableModel(tempData, columnNames){
            @Override
            public boolean isCellEditable(int row, int column) {
                // make read only fields except column 2
                return column == 2;
            }
        };

        JTable seriesTable = new JTable(seriesTableModel);
        seriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TableCellRenderer renderer = new EvenOddRenderer();
        seriesTable.setDefaultRenderer(Object.class, renderer);
        seriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        seriesTable.getColumnModel().getColumn(0).setMaxWidth(100);
        seriesTable.getColumnModel().getColumn(0).setMinWidth(70);
        seriesTable.getColumnModel().getColumn(1).setMinWidth(200);
        seriesTable.getColumnModel().getColumn(2).setMinWidth(200);
        seriesTable.getColumnModel().getColumn(3).setMinWidth(430);

        JScrollPane seriesTableScrollPane= new JScrollPane(seriesTable);
        seriesTable.setFillsViewportHeight(true);

        dicomSeriesPanel.add(seriesButtonPanel);
        dicomSeriesPanel.add(seriesTableScrollPane);

        /*-------------------------------------------------------------*/

        userOKButton = new JButton("OK");
        userOKButton.addActionListener(this);
        userOKButton.setMinimumSize(new Dimension(this.getWidth(), 24));

        /*-------------------------------------------------------------*/

        userCancelbutton = new JButton("Cancel");
        userCancelbutton.addActionListener(this);
        userCancelbutton.setMinimumSize(new Dimension(this.getWidth(), 24));

        JPanel naviButtonPanel = new JPanel();
        naviButtonPanel.setLayout(new FlowLayout());
        naviButtonPanel.add(userOKButton);
        naviButtonPanel.add(userCancelbutton);
        /*-------------------------------------------------------------*/

        userDialogFrame.add(dicomPatientPanel, gbc);
        userDialogFrame.add(dicomStudyPanel, gbc);
        userDialogFrame.add(dicomSeriesPanel, gbc);
        userDialogFrame.add(naviButtonPanel);

        userDialogFrame.setMinimumSize(new Dimension(this.getWidth(), this.getHeight()));
        userDialogFrame.setLocation(this.getLocation());
        userDialogFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        userDialogFrame.setVisible(true);

    }

    class EvenOddRenderer implements TableCellRenderer {

        public final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer = DEFAULT_RENDERER.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            ((JLabel) renderer).setOpaque(true);
            Color foreground, background;
            if (column % 2 == 0) {
                foreground = Color.black;
                background = Color.green;
            } else {
                foreground = Color.black;
                background = Color.pink;
            }
            renderer.setForeground(foreground);
            renderer.setBackground(background);
            return renderer;
        }
    }

    public String getHttpURL() {
        return httpURLField.getText().trim();
    }

    public String getDicomURL() {
        return dicomURL;
    }

    public String getSTOWURL() {
        return stowURL;
    }

    public String getSTOWUsername() {
        return stowUsername;
    }

    public String getSTOWPassword() {
        return stowPassword;
    }

    public File getExportDirectory() {
        return exportDirectory;
    }

    public boolean getRenameFiles() {
        return renameFiles;
    }

    public DirectoryPanel getDirectoryPanel() {
        return dp;
    }

    public StudyList getStudyList() {
        return studyList;
    }

    public Properties getDAScriptProps() {
        daScript = DAScript.getInstance(daScriptFile);
        Properties daScriptProps = daScript.toProperties();
        for (String configProp : config.stringPropertyNames()) {
            if (configProp.startsWith("@")) {
                String value = config.getProperty(configProp);
                String key = "param." + configProp.substring(1);
                daScriptProps.setProperty(key, value);
            }
        }
        return daScriptProps;
    }

    public Properties getDALUTProps() {
        Properties daLUTProps = LookupTable.getProperties(lookupTableFile, defaultKeyType);
        if (daLUTProps != null) {
            for (String configProp : config.stringPropertyNames()) {
                if (configProp.startsWith("$")) {
                    String value = config.getProperty(configProp);
                    String key = configProp.substring(1);
                    daLUTProps.setProperty(key, value);
                }
            }
        }
        return daLUTProps;
    }

    public IDTable getIDTable() {
        return idTable;
    }

    public String getDFScript() {
        return dfScript;
    }

    public PixelScript getDPAPixelScript() {
        return dpaPixelScript;
    }

    public boolean getDFEnabled() {
        return dfEnabled;
    }

    public boolean getDPAEnabled() {
        return dpaEnabled;
    }

    public boolean getSetBurnedInAnnotation() {
        return setBurnedInAnnotation;
    }

    public boolean getZip() {
        return zip;
    }

    public boolean getAcceptNonImageObjects() {
        //Require an explicit acceptance of non-image objects
        String anio = config.getProperty("acceptNonImageObjects", "");
        return anio.trim().equals("yes");
    }

    private Properties getConfiguration(String[] args) {
        Properties props = new Properties();

        //Put in the default titles
        props.setProperty("windowTitle", title);
        props.setProperty("panelTitle", title);

        try {
            //Get the config file from the jar
            File configFile = File.createTempFile("CONFIG-", ".properties");
            configFile.delete();
            FileUtil.getFile(configFile, "/config.properties");
            loadProperties(props, configFile);

            //Overwrite any props from the local defaults, if present
            File defProps = new File("config.default");
            loadProperties(props, defProps);

            //Add in the args
            for (String arg : args) {
                if (arg.length() >= 2) {
                    arg = StringUtil.removeEnclosingQuotes(arg);
                    int k = arg.indexOf("=");
                    if (k > 0) {
                        String name = arg.substring(0, k).trim();
                        String value = arg.substring(k + 1).trim();
                        props.setProperty(name, value);
                    }
                }
            }

            //Fix the httpURL property for backward compatibility
            String httpURL = props.getProperty("httpURL");
            String url = props.getProperty("url");
            if (((httpURL == null) || httpURL.equals("")) && (url != null)) {
                props.setProperty("httpURL", url);
            }
            //System.out.println(props.toString());
        } catch (Exception noProps) {
            Log.getInstance().append("Unable to load the config properties\n");
        }
        return props;
    }

    private void loadProperties(Properties props, File file) {
        if (file.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(file);
                props.load(stream);
            } catch (Exception ignore) {
            }
            FileUtil.close(stream);
        }
    }

    private DialogPanel getDialogPanel() {
        if (config.getProperty("dialogEnabled", "no").equals("yes")) {
            try {
                String dialogName = config.getProperty("dialogName", "DIALOG.xml");
                File dialogFile = getTextFile(dialogName, "/DIALOG.xml");
                //Now parse the file
                Document doc = XmlUtil.getDocument(dialogFile);
                return new DialogPanel(doc, config);
            } catch (Exception unable) {
                StringWriter sw = new StringWriter();
                unable.printStackTrace(new PrintWriter(sw));
                JOptionPane.showMessageDialog(this, "Exception in getDialogPanel:\n" + sw.toString());
            }
        }
        return null;
    }

    private void getDAScript() {
        daScript = null;
        String daScriptName = config.getProperty("daScriptName", "DA.script");
        daScriptFile = getTextFile(daScriptName, "/DA.script");
        if (daScriptFile != null) {
            daScript = DAScript.getInstance(daScriptFile);
            defaultKeyType = daScript.getDefaultKeyType();
        }
    }

    private void getLookupTable() {
        String daLUTName = config.getProperty("daLUTName");
        lookupTableFile = getTextFile(daLUTName, "/LUT.properties");
    }

    private String getDFScriptObject() {
        String filterScript = null;
        if (config.getProperty("dfEnabled", "no").equals("yes")) {
            String dfName = config.getProperty("dfScriptName", "DF.script");
            File dfFile = getTextFile(dfName, "/DF.script");
            if (dfFile != null) filterScript = FileUtil.getText(dfFile);
            else Log.getInstance().append("Unable to obtain the DicomFilter script\n");
        }
        return filterScript;
    }

    private PixelScript getDPAPixelScriptObject() {
        PixelScript pixelScript = null;
        if (config.getProperty("dpaEnabled", "no").equals("yes")) {
            String dpaScriptName = config.getProperty("dpaScriptName", "DPA.script");
            File dpaFile = getTextFile(dpaScriptName, "/DPA.script");
            if (dpaFile != null) pixelScript = new PixelScript(dpaFile);
            else Log.getInstance().append("Unable to obtain the DicomPixelAnonymizer script\n");
        }
        return pixelScript;
    }

    private void getKeystore() {
        try {
            File keystore = File.createTempFile("CC-", ".keystore");
            keystore.delete();
            FileUtil.getFile(keystore, "/keystore");
            System.setProperty("javax.net.ssl.keyStore", keystore.getAbsolutePath());
            System.setProperty("javax.net.ssl.keyStorePassword", "ctpstore");
        } catch (Exception ex) {
            Log.getInstance().append("Unable to install the keystore\n");
        }
    }

    private File getTextFile(String name, String resource) {
        if (name == null) return null;
        String protocol = config.getProperty("protocol");
        String host = config.getProperty("host");
        String application = config.getProperty("application");
        if (protocol != null && host != null && application != null) {
            String url = protocol + "://" + host + "/" + application + "/" + name;
            BufferedReader reader = null;
            try {
                //Connect to the server
                HttpURLConnection conn = HttpUtil.getConnection(url);
                conn.setRequestMethod("GET");
                conn.setDoOutput(false);
                conn.connect();

                //Get the response
                if (conn.getResponseCode() == HttpResponse.ok) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), FileUtil.utf8));
                    StringWriter buffer = new StringWriter();
                    char[] cbuf = new char[1024];
                    int n;
                    while ((n = reader.read(cbuf, 0, 1024)) != -1) buffer.write(cbuf, 0, n);
                    reader.close();
                    File file = File.createTempFile("CTPClient-", name);
                    if (FileUtil.setText(file, buffer.toString())) return file;
                }
            } catch (Exception unable) {
                FileUtil.close(reader);
            }
        } else {
            //The file is not available on the server, try to get it locally.
            File localFile = new File(name);
            if (localFile.exists()) return localFile;
        }
        if (resource != null) {
            //The file is not available locally; use the resource as a last resort.
            try {
                File file = File.createTempFile("CTPClient-", name);
                file.delete();
                return FileUtil.getFile(file, resource);
            } catch (Exception unable) {
            }
        }
        return null;
    }

    private File getDirectory() {
        if (chooser == null) {
            File here = new File(System.getProperty("user.dir"));
            chooser = new JFileChooser(here);
            chooser.setDialogTitle("Navigate to a directory containing images and click Open");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            if ((dir != null) && dir.exists()) return dir;
        }
        return null;
    }

    private void centerFrame() {
        Toolkit t = getToolkit();
        Dimension scr = t.getScreenSize();
        //int thisWidth = 2*(scr.width - instructionsWidth)/3;
        int thisWidth = scr.width;
        if (scr.width >= 960) {
            thisWidth = 960;
        }
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int taskBarSize = scnMax.bottom;
        int thisHeight = scr.height - taskBarSize;
        setSize(thisWidth, thisHeight);
        setLocation(new Point(0, 0));
    }

    class WindowCloser extends WindowAdapter {
        private final Component parent;

        WindowCloser(JFrame parent) {
            this.parent = parent;
            parent.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }

        public void windowClosing(WindowEvent evt) {

            //Make sure we aren't busy
            if (sending) {
                int response = JOptionPane.showConfirmDialog(
                        parent,
                        "Files are still being transmitted.\nAre you sure you want to stop the program?",
                        "Are you sure?",
                        JOptionPane.YES_NO_OPTION);
                if (response != JOptionPane.YES_OPTION) return;
            }

            //Stop the SCP if it's running.
            if (scp != null) scp.stop();

            //Offer to save the idTable if it isn't empty
            //idTable.save(parent);

            //Offer to save the log if it isn't empty
            Log.getInstance().save(parent);

            System.exit(0);
        }
    }

    //UI components
    class TitleLabel extends JLabel {
        TitleLabel(String s) {
            super(s);
            setFont(new Font("SansSerif", Font.BOLD, 24));
            setForeground(Color.BLUE);
        }
    }

    class InputText extends JTextField {
        InputText(String s) {
            super(s);
            setFont(new Font("Monospaced", Font.PLAIN, 12));
            Dimension size = getPreferredSize();
            size.width = 400;
            setPreferredSize(size);
            setMaximumSize(size);
        }
    }

    class FieldLabel extends JLabel {
        FieldLabel() {
            super("Destination URL:");
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.BLUE);
        }
    }

    class FieldButton extends JButton {
        FieldButton(String s) {
            super(s);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.BLUE);
            setAlignmentX(0.5f);
        }
    }

}