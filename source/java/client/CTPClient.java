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
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import org.json.simple.*;
import org.apache.log4j.*;
import org.json.simple.parser.JSONParser;
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
    private static final Color disabledCellColor = Color.PINK;
    private static final Color enabledCellColor = Color.GREEN;
    private static final Color blueColor = Color.BLUE;
    /*
    private static final Color bgColor = new Color(0xf1faee);
    private static final Color disabledCellColor = new Color(0xe63946);
    private static final Color enabledCellColor = new Color(0xa8dadc);
    private static final Color blueColor = new Color(0x1d3557);*/

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
    private String requiredPatientsBirthDate;
    private String pseudonym;
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
    private JTextField newDescField;
    private JTextField descField;
    private JButton userOKButton;
    private JButton userCancelbutton;
    private JFrame userDialogFrame;
    private DefaultTableModel seriesTableModel;
    private DefaultTableModel rtTableModel;
    private JTabbedPane seriesTabbedPane;
    JProgressBar progressBar;
    LinkedList<String> rtList = new LinkedList<>();
    String selectedStudyInstanceUID;
    private JTextField siuidField;
    private JLabel target;
    String hashedStudyInstanceUID = "";

    String sessionID = "";
    String apiKey = "";

    // Parameters for EDC - reference
    String studyOID = "";
    String subjectKey = "";
    String studyEventOID = "";
    String eventRepeatKey = "";
    String formOID = "";
    String itemGroupOID = "";
    String patientIDItemOID = "";
    String dicomStudyItemOID = "";


    // URL for portal to obtain API-Key
    String protocol = "http://";
    String host = "g40rpbtrialsdev.med.tu-dresden.de:8080";
    String dir = "/pacs/apiKey.faces?sessionid=";
    String portalURL = protocol + host + dir;

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

        target = new JLabel("Target: ");
        target.setFont(new Font("SansSerif", Font.BOLD, 12));

        //Get the DICOM export URL
        dicomURL = config.getProperty("dicomURL", "");
        if (dicomURL != null) {
            target.setText(target.getText() + " - " + dicomURL);
        }

        //Get the DICOM STOWRS export parameters
        stowURL = config.getProperty("stowURL", "");
        stowUsername = config.getProperty("stowUsername", "");
        stowPassword = config.getProperty("stowPassword", "");

        seriesTabbedPane = new JTabbedPane();

        //Set up the exportDirectory
        String expDir = config.getProperty("exportDirectory");
        if (expDir != null) exportDirectory = new File(expDir);
        renameFiles = config.getProperty("renameFiles", "no").equals("yes");

        //Get the button enables
        boolean showURL = !config.getProperty("showURL", "no").equals("no");
        showBrowseButton = !config.getProperty("showBrowseButton", "yes").equals("no") || (scpPort <= 0);
        boolean showDialogButton = !config.getProperty("showDialogButton", "yes").equals("no");

        setTitle(config.getProperty("windowTitle"));
        JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);

        //Get the study type to crosscheck later on
        requiredStudyType = Objects.toString(config.getProperty("studyType"), "");
        if (!requiredStudyType.equals("")) {
            target.setText(target.getText() + " - " + requiredStudyType);
        }

        //Get the birth date to crosscheck later on
        requiredPatientsBirthDate = Objects.toString(config.getProperty("birthDate"), "");
        // convert from iso date to dicom date
        if (requiredPatientsBirthDate.length() == 10) {
            requiredPatientsBirthDate = requiredPatientsBirthDate.replace("-", "");
        }

        pseudonym = Objects.toString(config.getProperty("pseudonym"), "");

        if (!pseudonym.equals("")) {
            target.setText(target.getText() + " - " + pseudonym);
        }

        //Get the gender of the patient to crosscheck
        requiredGender = Objects.toString(config.getProperty("gender"), "").toUpperCase();

        sessionID = Objects.toString(config.getProperty("sessionid"), "");
        portalURL += sessionID;

        obtaintAPIkey(portalURL);

        //Get the EDC Parameters
        studyOID = Objects.toString(config.getProperty("studyOID"), "");
        subjectKey = Objects.toString(config.getProperty("subjectKey"), "");
        studyEventOID = Objects.toString(config.getProperty("studyEventOID"), "");
        eventRepeatKey = Objects.toString(config.getProperty("eventRepeatKey"), "");
        formOID = Objects.toString(config.getProperty("formOID"), "");
        itemGroupOID = Objects.toString(config.getProperty("itemGroupOID"), "");
        patientIDItemOID = Objects.toString(config.getProperty("patientIDItemOID"), "");
        dicomStudyItemOID = Objects.toString(config.getProperty("dicomStudyItemOID"), "");

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
        if (!httpURL.equals("")) {
            target.setText(target.getText() + " - " + httpURL);
        }

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

        JPanel targetPanel = new JPanel();
        targetPanel.setBackground(bgColor);
        targetPanel.add(target);
        targetPanel.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));
        //panel.add(targetPanel, BorderLayout.SOUTH);

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

        vBox.add(targetPanel);

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

        //Make a footer bar to display status.
        StatusPane status = StatusPane.getInstance(" ", bgColor);

        //ProgressBar
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        status.addRightComponent(progressBar);

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

    boolean browseTheFirstTime = true;
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
                if (browseTheFirstTime) {
                    target.setText(target.getText() + " - " + studyList.getStudyDescriptionOfFirstStudy());
                    browseTheFirstTime = false;
                }
                else {
                    int start = target.getText().lastIndexOf("-");
                    int end = target.getText().length();
                    StringBuffer temp = new StringBuffer(target.getText());
                    temp.replace(start, end, "");
                    target.setText(temp.toString() + "- " + studyList.getStudyDescriptionOfFirstStudy());
                }

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
                int numberOfSelectedRTSTRUCTS = 0;
                for (Study study : studies) {
                    // check the number of selected RTSTRUCTs here
                    Series[] series = study.getSeries();
                    for (Series s : series) {
                        if (s.isSelected()) {
                            if (s.getModality().equals("RTSTRUCT")) {
                                numberOfSelectedRTSTRUCTS++;
                            }
                        }
                    }

                    if (study.isSelected()) {
                        numberOfSelectedStudies++;
                    }
                }
                if (numberOfSelectedStudies != 1) {

                    String msg = "You have to select exactly one study in order to proceed.";
                    showWarning(msg);

                } else if (numberOfSelectedRTSTRUCTS > 1) {

                    String msg = "You can select at most one RTSTRUCT. Please choose the RTSTRUCT " +
                            "of the main study and deselect all the other ones.";
                    showWarning(msg);

                } else if (!studyList.isNumberOfUniqueSOPInstanceUIDsEqual()) {

                    String msg ="The number of unique SOPInstanceUIDs detected from selected \n" +
                            "series needs to be equal to the number of files of selected DICOM series.";
                    showWarning(msg);
                } else if (!studyList.isSelectedConstellationValid()) {

                    String msg ="The selected constellation does not comply with the required type. \n" +
                            "Valid types are \n" +
                            "CT --- [CT] \n" +
                            "PET-CT --- [CT] + [PT] \n" +
                            "Treatmentplan --- [CT] + [RTSTRUCT] + [RTPLAN] + [RTDOSE] \n" +
                            "Contouring --- [CT] + [RTSTRUCT] \n" +
                            "MRI --- [MR] \n" +
                            "PET-MRI --- [MR] + [PT]";
                    showWarning(msg);
                }
                else {

                    startButton.setEnabled(false);
                    dialogButton.setEnabled(false);
                    browseButton.setEnabled(false);
                    scpButton.setEnabled(false);
                    sending = true;

                    //show window to change study/series descriptions
                    this.setEnabled(false);
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
            openInstructionWebPage();
        } else if (source.equals(userOKButton)) {
            // the new Study Description
            newStudyDescription = newDescField.getText();

            // the new Series Description
            for (int i = 0; i < seriesTableModel.getRowCount(); i++) {
                siUIDtoNewDescription.put(seriesTableModel.getValueAt(i, 3).toString(),
                        seriesTableModel.getValueAt(i, 2).toString());
            }

            for (int i = 0; i < rtTableModel.getRowCount(); i++) {
                //combine tagname with siUID (combined key), parse it in sender thread
                tagToNewRTDesc.put(rtTableModel.getValueAt(i, 0).toString()
                                + ";"
                                + rtTableModel.getValueAt(i, 3),
                        rtTableModel.getValueAt(i, 2).toString());
            }

            userDialogFrame.setEnabled(false);
            userDialogFrame.setVisible(false);

            this.setEnabled(true);
            this.setVisible(true);
            SenderThread sender = new SenderThread(this);
            sender.start();

        } else if (source.equals(userCancelbutton)) {
            seriesTabbedPane.removeAll();
            userDialogFrame.dispose();
            this.setEnabled(true);
            this.setVisible(true);
            startButton.setEnabled(true);
            dialogButton.setEnabled(true);
            browseButton.setEnabled(true);
            scpButton.setEnabled(true);
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

    public void componentShown(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {

    }

    public void componentMoved(ComponentEvent e) {

    }

    private void obtaintAPIkey(String portalURL) {
        Log log = Log.getInstance();
        String exMsg = "";
        int responseCode = 0;
        try {

            URL obj = new URL(portalURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();
            try {

                Object simpleObj = parser.parse(response.toString());
                JSONObject jsonObj = (JSONObject) simpleObj;

                apiKey = (String) jsonObj.get("apiKey");


            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception ex) {
            exMsg = ex.getMessage();
        }
        if (!exMsg.equals("")) log.append("Http Get Exception: " + exMsg + " --- ResponseCode: " + responseCode);
    }

    public static void openInstructionWebPage() {
        String url = "https://radplanbio.uniklinikum-dresden.de/help/client/clientmanual.html";

        if(Desktop.isDesktopSupported()){
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }else{
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                        "The selection has been processed... \n" +
                                "new siUID: " + hashedStudyInstanceUID,
                        "Processing Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                WindowEvent wev = new WindowEvent(parent, WindowEvent.WINDOW_CLOSING);
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
            }
        };
        SwingUtilities.invokeLater(enable);
    }

    private void showWarning (String msg) {
        final JFrame parent = this;
        Runnable enable = new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(parent,
                        msg,
                        "Information", JOptionPane.WARNING_MESSAGE);
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
            case "rtstruct": {
                infoString = "You have selected more than one RTSTRUCT. " +
                        "Please choose the RTSTRUCT of the main study.\n";
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

    public void showUserDialog () {

        Study studies[] = studyList.getStudies();
        Study selectedStudy = null;
        for (Study s : studies) {
            if (s.isSelected()) {
                selectedStudy = s;
            }
        }

        selectedStudyInstanceUID = selectedStudy.getSiuid();

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

        JLabel dicomPatientLabel = new JLabel("ID: ");
        dicomPatientLabel.setPreferredSize(new Dimension(50, 24));

        JTextField idField = new JTextField(selectedStudy.getPatientID());
        idField.setEditable(false);
        idField.setBackground(disabledCellColor);
        idField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel01 = new JLabel(" -> ");
        JTextField newIdField = new JTextField();
        newIdField.setText(pseudonym);
        newIdField.setEnabled(false);
        newIdField.setBackground(enabledCellColor);
        newIdField.setPreferredSize(new Dimension(250, 24));

        idPanel.add(dicomPatientLabel);
        idPanel.add(idField);
        idPanel.add(arrowLabel01);
        idPanel.add(newIdField);
        dicomPatientPanel.add(idPanel);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new FlowLayout());

        JLabel dicomNameLabel = new JLabel("Name: ");
        dicomNameLabel.setPreferredSize(new Dimension(50, 24));

        JTextField nameField = new JTextField(selectedStudy.getPatientname());
        nameField.setEditable(false);
        nameField.setBackground(disabledCellColor);
        nameField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel02 = new JLabel(" -> ");
        JTextField newNameField = new JTextField();
        newNameField.setText(pseudonym);
        newNameField.setBackground(enabledCellColor);
        newNameField.setPreferredSize(new Dimension(250, 24));

        namePanel.add(dicomNameLabel);
        namePanel.add(nameField);
        namePanel.add(arrowLabel02);
        namePanel.add(newNameField);
        dicomPatientPanel.add(namePanel);

        JPanel genderPanel = new JPanel();
        genderPanel.setLayout(new FlowLayout());

        JLabel dicomGenderLabel = new JLabel("Gender: ");
        dicomGenderLabel.setPreferredSize(new Dimension(50, 24));

        JTextField genderField = new JTextField(selectedStudy.getGender());
        genderField.setEditable(false);
        genderField.setBackground(disabledCellColor);
        genderField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel03 = new JLabel(" -> ");
        JTextField newGenderField = new JTextField(selectedStudy.getGender());
        newGenderField.setBackground(enabledCellColor);
        newGenderField.setPreferredSize(new Dimension(250, 24));

        genderPanel.add(dicomGenderLabel);
        genderPanel.add(genderField);
        genderPanel.add(arrowLabel03);
        genderPanel.add(newGenderField);
        dicomPatientPanel.add(genderPanel);

        JPanel dobPanel = new JPanel();
        dobPanel.setLayout(new FlowLayout());

        JLabel dicomDOBLabel = new JLabel("DOB: ");
        dicomDOBLabel.setPreferredSize(new Dimension(50, 24));

        JTextField dobField = new JTextField(selectedStudy.getPatientBirthDate());
        dobField.setEditable(false);
        dobField.setBackground(disabledCellColor);
        dobField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel04 = new JLabel(" -> ");
        JTextField newDOBField = new JTextField();
        newDOBField.setText("19000101");
        newDOBField.setBackground(enabledCellColor);
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
        typeField.setEditable(false);
        typeField.setBackground(enabledCellColor);
        typeField.setPreferredSize(new Dimension(250, 24));

        typePanel.add(dicomTypeLabel);
        typePanel.add(typeField);
        dicomStudyPanel.add(typePanel);

        JPanel descPanel = new JPanel();
        descPanel.setLayout(new FlowLayout());

        JLabel dicomDescLabel = new JLabel("Description: ");
        descField = new JTextField(selectedStudy.getStudyDescription());
        descField.setEditable(false);
        descField.setBackground(disabledCellColor);
        descField.setPreferredSize(new Dimension(250, 24));
        JLabel arrowLabel05 = new JLabel(" -> ");
        newDescField = new JTextField(selectedStudy.getStudyDescription());
        newDescField.setBackground(enabledCellColor);
        newDescField.setPreferredSize(new Dimension(250, 24));

        descPanel.add(dicomDescLabel);
        descPanel.add(descField);
        descPanel.add(arrowLabel05);
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
        JLabel arrowLabel06 = new JLabel("  ");
        seriesButtonPanel.add(arrowLabel06);

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
                    dataList.add(s.getSeriesDescription());
                    siUID = s.getSeriesName().getSeriesInstanceUID();
                    dataList.add(siUID);

                    //for rtRenaming
                    switch (modality) {
                        case "RTDOSE":
                            rtList.add("DoseComment");
                            rtList.add(s.getRTDoseComment());
                            rtList.add(s.getRTDoseComment());
                            rtList.add(s.getSeriesInstanceUID());
                            break;
                        case "RTPLAN":
                            rtList.add("RTPlanLabel");
                            rtList.add(s.getRTPlanLabel());
                            rtList.add(s.getRTPlanLabel());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("RTPlanName");
                            rtList.add(s.getRTPlanName());
                            rtList.add(s.getRTPlanName());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("RTPlanDescription");
                            rtList.add(s.getRTPlanDescription());
                            rtList.add(s.getRTPlanDescription());
                            rtList.add(s.getSeriesInstanceUID());
                            break;
                        case "RTSTRUCT":
                            rtList.add("StructureSetLabel");
                            rtList.add(s.getSSLabel());
                            rtList.add(s.getSSLabel());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("StructureSetName");
                            rtList.add(s.getSSName());
                            rtList.add(s.getSSName());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("StructureSetDescription");
                            rtList.add(s.getSSDescription());
                            rtList.add(s.getSSDescription());
                            rtList.add(s.getSeriesInstanceUID());
                            break;
                        case "RTIMAGE":
                            rtList.add("RTImageName");
                            rtList.add(s.getRTImageName());
                            rtList.add(s.getRTImageName());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("RTImageLabel");
                            rtList.add(s.getRTImageLabel());
                            rtList.add(s.getRTImageLabel());
                            rtList.add(s.getSeriesInstanceUID());

                            rtList.add("RTImageDescription");
                            rtList.add(s.getRTImageDescription());
                            rtList.add(s.getRTImageDescription());
                            rtList.add(s.getSeriesInstanceUID());
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
        seriesTable.getTableHeader().setEnabled(false);

        seriesTable.getColumnModel().getColumn(0).setMaxWidth(100);
        seriesTable.getColumnModel().getColumn(0).setMinWidth(70);
        seriesTable.getColumnModel().getColumn(1).setMinWidth(260);
        seriesTable.getColumnModel().getColumn(2).setMinWidth(260);
        seriesTable.getColumnModel().getColumn(3).setMinWidth(430);

        JScrollPane seriesTableScrollPane= new JScrollPane(seriesTable);
        seriesTable.setFillsViewportHeight(true);

        seriesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (seriesTable.getValueAt(seriesTable.getSelectedRow(), 0).toString().contains("RT")) {
                    seriesTabbedPane.setEnabledAt(1, true);
                }
                else {
                    seriesTabbedPane.setEnabledAt(1, false);
                }


                siuidField.setText(seriesTable.getValueAt(seriesTable.getSelectedRow(), 3).toString());
                }
        });

        seriesTabbedPane.addTab("Modalities", null, seriesTableScrollPane,
                "Overview of the selected modalities. " +
                        "Select a modality and go to 'Modify descriptions' tab " +
                        "to change the corresponding values.");


        /*--------------second-tab---------------------------------------*/

        String[] rtColumnNames = {"Tag",
                "original Description",
                "new Description",
                "siUID"};

        Object[][] rtTempData = new Object[rtList.size() / 4][4];
        for (int i = 0; i < rtList.size(); i++) {
            rtTempData[i / 4][i % 4] = rtList.get(i);
        }

        rtTableModel = new DefaultTableModel(rtTempData, rtColumnNames){
            @Override
            public boolean isCellEditable(int row, int column) {
                // make read only fields except column 3
                return column == 2;
            }
        };

        JTable rtTable = new JTable(rtTableModel);
        rtTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TableCellRenderer rtRenderer = new EvenOddRenderer();
        rtTable.setDefaultRenderer(Object.class, rtRenderer);
        rtTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        rtTable.getTableHeader().setEnabled(false);

        rtTable.getColumnModel().getColumn(0).setMinWidth(140);
        rtTable.getColumnModel().getColumn(1).setMinWidth(230);
        rtTable.getColumnModel().getColumn(2).setMinWidth(230);
        rtTable.getColumnModel().getColumn(3).setMinWidth(450);

        JScrollPane rtTableScrollPane= new JScrollPane(rtTable);
        rtTable.setFillsViewportHeight(true);

        TableRowSorter<TableModel> rowSorter = new TableRowSorter<>(rtTable.getModel());
        rtTable.setRowSorter(rowSorter);

        rtTable.getColumnModel().removeColumn(rtTable.getColumnModel().getColumn(3));

        JPanel rtPanel = new JPanel();
        rtPanel.setLayout(new BoxLayout(rtPanel, BoxLayout.Y_AXIS));

        TitledBorder rtBorder = new TitledBorder("RT Descriptions");
        rtBorder.setTitleJustification(TitledBorder.LEFT);
        rtBorder.setTitlePosition(TitledBorder.TOP);
        rtPanel.setBorder(rtBorder);
        rtPanel.add(rtTableScrollPane);

        JPanel dicomModalityPanel = new JPanel();

        siuidField = new JTextField();
        siuidField.setVisible(false);
        siuidField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                String text = siuidField.getText();
                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                    rowSorter.setRowFilter(RowFilter.regexFilter(text));
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                String text = siuidField.getText();
                if (text.trim().length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                    rowSorter.setRowFilter(RowFilter.regexFilter(text));
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });

        seriesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JTable t = (JTable)e.getSource();
                Point p = e.getPoint();
                int row = t.rowAtPoint(p);
                int col = t.columnAtPoint(p);
                String modality = (String) t.getValueAt(row, 0);
                if (col == 0 && e.getClickCount() == 2 && (modality.contains("RT"))) {
                    seriesTabbedPane.setSelectedIndex(1);
                }
            }
        });

        dicomModalityPanel.add(siuidField);
        dicomModalityPanel.add(rtTableScrollPane);
        dicomModalityPanel.setLayout(new BoxLayout(dicomModalityPanel, BoxLayout.Y_AXIS));
        seriesTabbedPane.addTab("Modify descriptions", null, dicomModalityPanel,
                "Change values of selected modality.");

        seriesTabbedPane.setEnabledAt(1, false);

        dicomSeriesPanel.add(seriesTabbedPane);
        dicomSeriesPanel.add(seriesButtonPanel);
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

        userDialogFrame.pack();

        userDialogFrame.setLocation(this.getLocation().x + ((this.getWidth() - userDialogFrame.getWidth()) / 2),
               this.getLocation().y + ((this.getHeight() - userDialogFrame.getHeight()) / 2));
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
                background = enabledCellColor;
            } else {
                foreground = Color.black;
                background = disabledCellColor;
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
        String anio = config.getProperty("acceptNonImageObjects", "yes");
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
            setForeground(blueColor);
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
            setForeground(blueColor);
        }
    }

    class FieldButton extends JButton {
        FieldButton(String s) {
            super(s);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(blueColor);
            setAlignmentX(0.5f);
        }
    }
}