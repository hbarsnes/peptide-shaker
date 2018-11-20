
package eu.isas.peptideshaker.gui.protein_inference;

import javax.swing.JDialog;

/**
 * Dialog for displaying the selected nodes in a ProteinInferenceGraphPanel.
 * 
 * @author Harald Barsnes
 */
public class ProteinInferenceGraphSelectionDialog extends javax.swing.JDialog {

    /**
     * Empty default constructor
     */
    public ProteinInferenceGraphSelectionDialog() {
    }

    /**
     * Creates a new ProteinInferenceGraphSelectionJDialog.
     * 
     * @param parent the parent dialog
     * @param modal modal or not
     * @param text the text to display
     */
    public ProteinInferenceGraphSelectionDialog(JDialog parent, boolean modal, String text) {
        super(parent, modal);
        initComponents();
        valuesEditorPane.setText(text);
        valuesEditorPane.setCaretPosition(0);
        setLocationRelativeTo(parent);
        setVisible(true);
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
        valuesPanel = new javax.swing.JPanel();
        valuesScrollPane = new javax.swing.JScrollPane();
        valuesEditorPane = new javax.swing.JEditorPane();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Selected Values");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        valuesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins & Peptides"));
        valuesPanel.setMinimumSize(new java.awt.Dimension(400, 400));
        valuesPanel.setOpaque(false);

        valuesEditorPane.setEditable(false);
        valuesEditorPane.setContentType("text/html"); // NOI18N
        valuesScrollPane.setViewportView(valuesEditorPane);

        javax.swing.GroupLayout valuesPanelLayout = new javax.swing.GroupLayout(valuesPanel);
        valuesPanel.setLayout(valuesPanelLayout);
        valuesPanelLayout.setHorizontalGroup(
            valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valuesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valuesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
                .addContainerGap())
        );
        valuesPanelLayout.setVerticalGroup(
            valuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(valuesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valuesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
                .addContainerGap())
        );

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valuesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valuesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the dialog.
     * 
     * @param evt 
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JEditorPane valuesEditorPane;
    private javax.swing.JPanel valuesPanel;
    private javax.swing.JScrollPane valuesScrollPane;
    // End of variables declaration//GEN-END:variables
}
