package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.annotation.go.GOFactory;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * A dialog for choosing the species.
 *
 * @author Harald Barsnes
 */
public class SpeciesDialog extends javax.swing.JDialog {

    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The GO factory.
     */
    private GOFactory goFactory = GOFactory.getInstance();
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * If true, the GO mappings are updated when selecting an item in the drop
     * down menu. (Needed when automatically changing the selected value.)
     */
    private boolean loadMappings = false;

    /**
     * Creates a new SpeciesDialog.
     *
     * @param peptideShakerGUI
     * @param modal
     */
    public SpeciesDialog(PeptideShakerGUI peptideShakerGUI, boolean modal) {
        super(peptideShakerGUI, modal);
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        setUpGUI();
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI details.
     */
    private void setUpGUI() {
        speciesJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        speciesJComboBox.setModel(new DefaultComboBoxModel(peptideShakerGUI.getGenePreferences().getSpecies()));

        loadMappings = false;

        // select the current species
        boolean speciesFound = false;
        for (int i = 0; i < speciesJComboBox.getItemCount() && !speciesFound; i++) {
            String temp = (String) speciesJComboBox.getModel().getElementAt(i);
            if (temp.contains(peptideShakerGUI.getGenePreferences().getCurrentSpecies())) {
                speciesFound = true;
                speciesJComboBox.setSelectedIndex(i);
            }
        }

        loadMappings = true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        speciesJComboBox = new javax.swing.JComboBox();
        updateButton = new javax.swing.JButton();
        downloadButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        unknownSpeciesLabel = new javax.swing.JLabel();
        ensemblVersionLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Species");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Species"));
        jPanel1.setOpaque(false);

        speciesJComboBox.setMaximumRowCount(20);
        speciesJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        speciesJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speciesJComboBoxActionPerformed(evt);
            }
        });

        updateButton.setText("Update");
        updateButton.setToolTipText("Update the GO Mappings");
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        downloadButton.setText("Download");
        downloadButton.setToolTipText("Download GO Mappings");
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(speciesJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 409, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(downloadButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {downloadButton, updateButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(downloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(updateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        unknownSpeciesLabel.setFont(unknownSpeciesLabel.getFont().deriveFont((unknownSpeciesLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        unknownSpeciesLabel.setText("<html><a href>Species not in list?</a></html>");
        unknownSpeciesLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                unknownSpeciesLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                unknownSpeciesLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                unknownSpeciesLabelMouseExited(evt);
            }
        });

        ensemblVersionLabel.setFont(ensemblVersionLabel.getFont().deriveFont((ensemblVersionLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        ensemblVersionLabel.setText("<html><a href>Ensembl version?</a></html>");
        ensemblVersionLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ensemblVersionLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ensemblVersionLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ensemblVersionLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(unknownSpeciesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(ensemblVersionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(unknownSpeciesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(ensemblVersionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the gene and GO mappings according to the selected species.
     *
     * @param evt
     */
    private void speciesJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speciesJComboBoxActionPerformed

        if (loadMappings) {

            String selectedSpecies = (String) speciesJComboBox.getSelectedItem();
            clearOldResults();
            peptideShakerGUI.getGOPanel().clearOldResults();

            if (!selectedSpecies.equalsIgnoreCase(peptideShakerGUI.getGenePreferences().SPECIES_SEPARATOR) && !selectedSpecies.equalsIgnoreCase("-- Select Species --")) {

                selectedSpecies = selectedSpecies.substring(0, selectedSpecies.indexOf("[") - 1);
                peptideShakerGUI.getGenePreferences().setCurrentSpecies(selectedSpecies);

                if (peptideShakerGUI.getSelectedTab() == PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX) {
                    peptideShakerGUI.getGOPanel().displayResults();
                } else {
                    peptideShakerGUI.setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, false);

                    String speciesDatabase = peptideShakerGUI.getGenePreferences().getSpeciesMap().get(selectedSpecies);

                    if (speciesDatabase != null) {

                        final File goMappingsFile = new File(peptideShakerGUI.getGenePreferences().getGeneMappingFolder(), speciesDatabase + peptideShakerGUI.getGenePreferences().GO_MAPPING_FILE_SUFFIX);
                        final File geneMappingsFile = new File(peptideShakerGUI.getGenePreferences().getGeneMappingFolder(), speciesDatabase + peptideShakerGUI.getGenePreferences().GENE_MAPPING_FILE_SUFFIX);

                        if (goMappingsFile.exists()) {

                            progressDialog = new ProgressDialogX(peptideShakerGUI,
                                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                                    true);
                            progressDialog.setTitle("Getting Gene Mapping Files. Please Wait...");
                            progressDialog.setIndeterminate(true);

                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        progressDialog.setVisible(true);
                                    } catch (IndexOutOfBoundsException e) {
                                        // ignore
                                    }
                                }
                            }, "ProgressDialog").start();

                            new Thread("GoThread") {
                                @Override
                                public void run() {
                                    try {
                                        progressDialog.setTitle("Getting Gene Mappings. Please Wait...");
                                        geneFactory.initialize(geneMappingsFile, progressDialog);
                                        progressDialog.setTitle("Getting GO Mappings. Please Wait...");
                                        goFactory.initialize(goMappingsFile, progressDialog);
                                        progressDialog.setRunFinished();
                                    } catch (Exception e) {
                                        progressDialog.setRunFinished();
                                        peptideShakerGUI.catchException(e);
                                    }
                                }
                            }.start();
                        }
                    }
                }
            }
        }
    }//GEN-LAST:event_speciesJComboBoxActionPerformed

    /**
     * Try to download the GO mappings for the currently selected species.
     *
     * @param evt
     */
    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Sending Request. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("GoThread") {
            @Override
            public void run() {

                try {
                    // clear old data
                    clearOldResults();

                    // get the current Ensembl version
                    URL url = new URL("http://www.biomart.org/biomart/martservice?type=registry");

                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                    String inputLine;
                    boolean ensemblVersionFound = false;
                    String ensemblVersion = "?";

                    while ((inputLine = in.readLine()) != null && !ensemblVersionFound && !progressDialog.isRunCanceled()) {
                        if (inputLine.indexOf("database=\"ensembl_mart_") != -1) {
                            ensemblVersion = inputLine.substring(inputLine.indexOf("database=\"ensembl_mart_") + "database=\"ensembl_mart_".length());
                            ensemblVersion = ensemblVersion.substring(0, ensemblVersion.indexOf("\""));
                            ensemblVersionFound = true;
                        }
                    }

                    in.close();

                    String selectedSpecies = (String) speciesJComboBox.getSelectedItem();
                    selectedSpecies = selectedSpecies.substring(0, selectedSpecies.indexOf("[") - 1);
                    selectedSpecies = peptideShakerGUI.getGenePreferences().getSpeciesMap().get(selectedSpecies);

                    boolean geneMappingsDownloaded = false;
                    boolean goMappingsDownloadeded = false;

                    if (!progressDialog.isRunCanceled()) {
                        goMappingsDownloadeded = peptideShakerGUI.getGenePreferences().downloadGoMappings(selectedSpecies, ensemblVersion, progressDialog);
                    }
                    if (goMappingsDownloadeded && !progressDialog.isRunCanceled()) {
                        geneMappingsDownloaded = peptideShakerGUI.getGenePreferences().downloadGeneMappings(selectedSpecies, progressDialog);
                    }

                    progressDialog.setRunFinished();

                    if (geneMappingsDownloaded && goMappingsDownloadeded) {
                        JOptionPane.showMessageDialog(peptideShakerGUI, "Gene mappings downloaded.\nRe-select species to use.", "Gene Mappings", JOptionPane.INFORMATION_MESSAGE);
                        peptideShakerGUI.getGenePreferences().loadSpeciesAndGoDomains();
                        speciesJComboBox.setSelectedIndex(0);
                    }

                    // @TODO: the code below ought to work, but results in bugs...
                    //        therefore the user now has to reselect in the drop down menu
                    //                    int index = speciesJComboBox.getSelectedIndex();
                    //                    loadSpeciesAndGoDomains();
                    //                    speciesJComboBox.setSelectedIndex(index);
                    //                    speciesJComboBoxActionPerformed(null);
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "An error occured when downloading the mappings.", "Download Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }//GEN-LAST:event_downloadButtonActionPerformed

    /**
     * Tries to update the GO mappings for the currently selected species.
     *
     * @param evt
     */
    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed

        // delete the old mappings file
        String selectedSpecies = (String) speciesJComboBox.getSelectedItem();
        selectedSpecies = selectedSpecies.substring(0, selectedSpecies.indexOf("[") - 1);
        selectedSpecies = peptideShakerGUI.getGenePreferences().getSpeciesMap().get(selectedSpecies);
        goFactory.clearFactory();
        geneFactory.clearFactory();

        try {
            goFactory.closeFiles();
            geneFactory.closeFiles();

            File tempSpeciesFile = new File(peptideShakerGUI.getGenePreferences().getGeneMappingFolder(), selectedSpecies);

            if (tempSpeciesFile.exists()) {
                boolean delete = tempSpeciesFile.delete();

                if (!delete) {
                    JOptionPane.showMessageDialog(this, "Failed to delete \'" + tempSpeciesFile.getAbsolutePath() + "\'.\n"
                            + "Please delete the file manually, reselect the species in the list and click the Download button instead.", "Delete Failed",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    downloadButtonActionPerformed(null);
                }
            }
        } catch (IOException ex) {
            peptideShakerGUI.catchException(ex);
        }
    }//GEN-LAST:event_updateButtonActionPerformed

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void unknownSpeciesLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_unknownSpeciesLabelMouseClicked
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/GOEA.html"), "#Species",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_unknownSpeciesLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void unknownSpeciesLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_unknownSpeciesLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_unknownSpeciesLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void unknownSpeciesLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_unknownSpeciesLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_unknownSpeciesLabelMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void ensemblVersionLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ensemblVersionLabelMouseClicked
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/GOEA.html"), "#Ensembl_Version",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ensemblVersionLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void ensemblVersionLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ensemblVersionLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_ensemblVersionLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ensemblVersionLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ensemblVersionLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ensemblVersionLabelMouseExited

    /**
     * Close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton downloadButton;
    private javax.swing.JLabel ensemblVersionLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox speciesJComboBox;
    private javax.swing.JLabel unknownSpeciesLabel;
    private javax.swing.JButton updateButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Clear the old results.
     */
    private void clearOldResults() {

        downloadButton.setEnabled(true);
        updateButton.setEnabled(false);

        goFactory.clearFactory();
        geneFactory.clearFactory();

        try {
            goFactory.closeFiles();
            geneFactory.closeFiles();
        } catch (IOException ex) {
            peptideShakerGUI.catchException(ex);
        }
    }
}
