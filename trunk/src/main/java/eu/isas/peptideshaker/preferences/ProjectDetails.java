package eu.isas.peptideshaker.preferences;

import com.compomics.util.Util;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import com.compomics.util.pride.prideobjects.*;
import java.util.HashMap;

/**
 * This class contains the details about a project.
 *
 * @author Marc Vaudel
 */
public class ProjectDetails implements Serializable {

    /**
     * Serial version UID for post-serialization compatibility.
     */
    static final long serialVersionUID = -2635206350852992221L;
    /**
     * List of the identification files loaded.
     */
    private ArrayList<File> identificationFiles = new ArrayList<File>();
    /**
     * List of the spectrum files
     */
    private HashMap<String, File> spectrumFiles = new HashMap<String, File>();
    /**
     * When the project was created.
     */
    private Date creationDate;
    /**
     * The report created during the loading of the tool 
     */
    private String report;
    /**
     * The PRIDE experiment title.
     */
    private String prideExperimentTitle;
    /**
     * The PRIDE experiment label.
     */
    private String prideExperimentLabel;
    /**
     * The PRIDE experiment project title.
     */
    private String prideExperimentProjectTitle;
    /**
     * The PRIDE experiment description.
     */
    private String prideExperimentDescription;
    /**
     * The PRIDE reference group.
     */
    private ReferenceGroup prideReferenceGroup;
    /**
     * The PRIDE contact group.
     */
    private ContactGroup prideContactGroup;
    /**
     * The  PRIDE sample details.
     */
    private Sample prideSample;
    /**
     * The PRIDE protocol details.
     */
    private Protocol prideProtocol;
    /**
     * The PRIDE instrument details.
     */
    private Instrument prideInstrument;
    /**
     * The PRIDE output folder.
     */
    private String prideOutputFolder;
    

    /**
     * Constructor
     */
    public ProjectDetails() {
    }

    /**
     * Getter for all identification files loaded.
     *
     * @return all identification files loaded
     */
    public ArrayList<File> getIdentificationFiles() {
        return identificationFiles;
    }

    /**
     * Adds an identification file to the list of loaded identification files
     *
     * @param identificationFile the identification file loaded
     */
    public void addIdentificationFiles(File identificationFile) {
        identificationFiles.add(identificationFile);
    }
    
    /**
     * Attaches a spectrum file to the project.
     * Warning: any previous file with the same name will be silently ignored.
     * @param spectrumFile the spectrum file to add
     */
    public void addSpectrumFile(File spectrumFile) {
        String fileName = Util.getFileName(spectrumFile.getAbsolutePath());
        spectrumFiles.put(fileName, spectrumFile);
    }
    
    /**
     * Returns the file corresponding to the given name
     * @param fileName the name of the desired file
     * @return the corresponding file, null if not found.
     */
    public File getSpectrumFile(String fileName) {
        // Compatibility check
        if (spectrumFiles == null) {
            spectrumFiles = new HashMap<String, File>();
        }
        return spectrumFiles.get(fileName);
    }

    /**
     * Getter for the creation date of the project.
     *
     * @return the creation date of the project
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Setter the creation date of the project.
     *
     * @param creationDate the creation date of the project
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the report created during the loading of the project.
     * 
     * @return the report created during the loading of the project
     */
    public String getReport() {
        
        if (report == null) {
            return "(report not saved)";
        }
        
        return report;
    }

    /**
     * Set the report created during the loading of the project.
     * 
     * @param report the report to set
     */
    public void setReport(String report) {
        this.report = report;
    }
    
    /**
     * Returns the PRIDE experiment title.
     * 
     * @return the prideExperimenttitle
     */
    public String getPrideExperimentTitle() {
        return prideExperimentTitle;
    }

    /**
     * Sets the PRIDE experiment title.
     * 
     * @param prideExperimentTitle the prideExperimentTitle to set
     */
    public void setPrideExperimentTitle(String prideExperimentTitle) {
        this.prideExperimentTitle = prideExperimentTitle;
    }

    /**
     * Returns the PRIDE experiment label.
     * 
     * @return the prideExperimentLabel
     */
    public String getPrideExperimentLabel() {
        return prideExperimentLabel;
    }

    /**
     * Sets the PRIDE experiment label.
     * 
     * @param prideExperimentLabel the prideExperimentLabel to set
     */
    public void setPrideExperimentLabel(String prideExperimentLabel) {
        this.prideExperimentLabel = prideExperimentLabel;
    }

    /**
     * Returns the PRIDE experiment project title.
     * 
     * @return the prideExperimentProjectTitle
     */
    public String getPrideExperimentProjectTitle() {
        return prideExperimentProjectTitle;
    }

    /**
     * Set the PRIDE experiment project title.
     * 
     * @param prideExperimentProjectTitle the prideExperimentProjectTitle to set
     */
    public void setPrideExperimentProjectTitle(String prideExperimentProjectTitle) {
        this.prideExperimentProjectTitle = prideExperimentProjectTitle;
    }

    /**
     * Returns the PRIDE experiment project description.
     * 
     * @return the prideExperimentDescription
     */
    public String getPrideExperimentDescription() {
        return prideExperimentDescription;
    }

    /**
     * Set the PRIDE experiment project description.
     * 
     * @param prideExperimentDescription the prideExperimentDescription to set
     */
    public void setPrideExperimentDescription(String prideExperimentDescription) {
        this.prideExperimentDescription = prideExperimentDescription;
    }
    
    /**
     * Returns the PRIDE reference group.
     * 
     * @return the prideReferenceGroup
     */
    public ReferenceGroup getPrideReferenceGroup() {
        return prideReferenceGroup;
    }

    /**
     * Set the PRIDE reference group.
     * 
     * @param prideReferenceGroup the prideReferenceGroup to set
     */
    public void setPrideReferenceGroup(ReferenceGroup prideReferenceGroup) {
        this.prideReferenceGroup = prideReferenceGroup;
    }

    /**
     * Returns the PRIDE contact group.
     * 
     * @return the prideContactGroup
     */
    public ContactGroup getPrideContactGroup() {
        return prideContactGroup;
    }

    /**
     * Set the PRIDE contact group.
     * 
     * @param prideContactGroup the prideContactGroup to set
     */
    public void setPrideContactGroup(ContactGroup prideContactGroup) {
        this.prideContactGroup = prideContactGroup;
    }

    /**
     * Returns the PRIDE sample.
     * 
     * @return the prideSample
     */
    public Sample getPrideSample() {
        return prideSample;
    }

    /**
     * Set the PRIDE sample.
     * 
     * @param prideSample the prideSample to set
     */
    public void setPrideSample(Sample prideSample) {
        this.prideSample = prideSample;
    }

    /**
     * Returns the PRIDE protocol.
     * 
     * @return the prideProtocol
     */
    public Protocol getPrideProtocol() {
        return prideProtocol;
    }

    /**
     * Set the PRIDE protocol.
     * 
     * @param prideProtocol the prideProtocol to set
     */
    public void setPrideProtocol(Protocol prideProtocol) {
        this.prideProtocol = prideProtocol;
    }

    /**
     * Returns the PRIDE instrument.
     * 
     * @return the prideInstrument
     */
    public Instrument getPrideInstrument() {
        return prideInstrument;
    }

    /**
     * Set the the PRIDE instrument.
     * 
     * @param prideInstrument the prideInstrument to set
     */
    public void setPrideInstrument(Instrument prideInstrument) {
        this.prideInstrument = prideInstrument;
    }

    /**
     * Returns the PRIDE output folder.
     * 
     * @return the prideOutputFolder
     */
    public String getPrideOutputFolder() {
        return prideOutputFolder;
    }

    /**
     * Set the PRIDE output folder.
     * 
     * @param prideOutputFolder the prideOutputFolder to set
     */
    public void setPrideOutputFolder(String prideOutputFolder) {
        this.prideOutputFolder = prideOutputFolder;
    }
}
