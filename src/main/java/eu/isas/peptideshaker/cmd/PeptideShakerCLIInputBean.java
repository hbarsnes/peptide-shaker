package eu.isas.peptideshaker.cmd;

import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.preferences.ModificationProfile;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is a simple bean wrapping the CLI parameters provided in an
 * Options instance.
 *
 * @author Kenny Helsens
 */
public class PeptideShakerCLIInputBean {

    /**
     * The experiment name.
     */
    private String iExperimentID = null;
    /**
     * The sample name.
     */
    private String iSampleID = null;
    /**
     * The replicate number.
     */
    private int replicate = 0;
    /**
     * The spectrum files
     */
    private ArrayList<File> spectrumFiles = null;
    /**
     * The identification files
     */
    private ArrayList<File> idFiles = null;
    /**
     * PeptideShaker output file.
     */
    private File output = null;
    /**
     * csv output directory.
     */
    private File csvDirectory = null;
    /**
     * PeptideShaker pride output file.
     */
    private File prideFile = null;
    /**
     * PSM FDR used for validation.
     */
    private double psmFDR = 1.0;
    /**
     * PSM FLR used for modification localization.
     */
    private double psmFLR = 1.0;
    /**
     * Peptide FDR used for validation.
     */
    private double peptideFDR = 1.0;
    /**
     * Protein FDR used for validation.
     */
    private double proteinFDR = 1.0;
    /**
     * The identification parameters used for the search
     */
    private SearchParameters identificationParameters = null;

    /**
     * Construct a PeptideShakerCLIInputBean from a Apache CLI instance.
     *
     * @param aLine the command line
     */
    public PeptideShakerCLIInputBean(CommandLine aLine) throws FileNotFoundException, IOException, ClassNotFoundException {

        iExperimentID = aLine.getOptionValue(PeptideShakerCLIParams.EXPERIMENT.id);
        iSampleID = aLine.getOptionValue(PeptideShakerCLIParams.SAMPLE.id);
        
        if (aLine.hasOption(PeptideShakerCLIParams.REPLICATE.id)) {
            replicate = new Integer(aLine.getOptionValue(PeptideShakerCLIParams.REPLICATE.id));
        }
        
        spectrumFiles = new ArrayList<File>();
        String filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SPECTRUM_FILES.id);
        for (String file : splitInput(filesTxt)) {
            File testFile = new File(file);
            if (testFile.exists()) {
                spectrumFiles.add(testFile);
            } else {
                throw new FileNotFoundException(file + " not found.");
            }
        }
        
        idFiles = new ArrayList<File>();
        filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.IDENTIFICATION_FILES.id);
        for (String file : splitInput(filesTxt)) {
            File testFile = new File(file);
            if (testFile.exists()) {
                idFiles.add(testFile);
            } else {
                throw new FileNotFoundException(file + " not found.");
            }
        }
        
        output = new File(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_OUTPUT.id));
        
        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id)) {
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_CSV.id).trim();
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                csvDirectory = testFile;
                
            } else {
                throw new FileNotFoundException(filesTxt + " not found.");
            }
        }
        
        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id)) {
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDESHAKER_PRIDE.id);
            File testFile = new File(filesTxt);
                prideFile = testFile;
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FDR.id)) {
            psmFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PSM_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PSM_FLR.id)) {
            psmFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PSM_FLR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PEPTIDE_FDR.id)) {
            peptideFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PEPTIDE_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.PROTEIN_FDR.id)) {
            proteinFDR = Double.parseDouble(aLine.getOptionValue(PeptideShakerCLIParams.PROTEIN_FDR.id));
        }

        if (aLine.hasOption(PeptideShakerCLIParams.SEARCH_PARAMETERS.id)) {
            
            filesTxt = aLine.getOptionValue(PeptideShakerCLIParams.SEARCH_PARAMETERS.id);
            File testFile = new File(filesTxt);
            if (testFile.exists()) {
                identificationParameters = SearchParameters.getIdentificationParameters(testFile);
            } else {
                throw new FileNotFoundException(filesTxt + " not found.");
            }
        }

    }

    /**
     * Empty constructor for API usage via other tools.
     */
    public PeptideShakerCLIInputBean() {
    }

    /**
     * Returns the directory for csv output. Null if not set
     * @return the directory for csv output
     */
    public File getCsvDirectory() {
        return csvDirectory;
    }

    /**
     * Sets the directory for csv output
     * @param csvDirectory the directory for csv output
     */
    public void setCsvDirectory(File csvDirectory) {
        this.csvDirectory = csvDirectory;
    }

    /**
     * Returns the experiment name
     * @return the experiment name
     */
    public String getiExperimentID() {
        return iExperimentID;
    }

    /**
     * Sets the experiment name
     * @param iExperimentID the experiment name
     */
    public void setiExperimentID(String iExperimentID) {
        this.iExperimentID = iExperimentID;
    }

    /**
     * Returns the cps output file
     * @return the cps output file
     */
    public File getOutput() {
        return output;
    }

    /**
     * Sets the cps output file
     * @param output the cps output file
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * Returns the PSM FDR in percent.
     * @return the PSM FDR
     */
    public double getPsmFDR() {
        return psmFDR;
    }

    /**
     * Sets the PSM FDR in percent
     * @param iPSMFDR the PSM FDR
     */
    public void setPsmFDR(double psmFDR) {
        this.psmFDR = psmFDR;
    }

    /**
     * Sets the PSM FLR in percent
     * @return the PSM FLR 
     */
    public double getiPsmFLR() {
        return psmFLR;
    }

    /**
     * Sets the PSM FLR in percent
     * @param iPSMFLR the PSM FLR
     */
    public void setPsmFLR(double psmFLR) {
        this.psmFLR = psmFLR;
    }

    /**
     * Returns the peptide FDR in percent
     * @return the peptide FDR
     */
    public double getPeptideFDR() {
        return peptideFDR;
    }

    /**
     * Sets the peptide FDR in percent
     * @param iPeptideFDR the peptide FDR
     */
    public void setPeptideFDR(double peptideFDR) {
        this.peptideFDR = peptideFDR;
    }

    /**
     * Returns the protein FDR in percent
     * @return the protein FDR 
     */
    public double getProteinFDR() {
        return proteinFDR;
    }

    /**
     * Sets the protein FDR in percent
     * @param iProteinFDR the protein FDR
     */
    public void setProteinFDR(double proteinFDR) {
        this.proteinFDR = proteinFDR;
    }

    /**
     * Returns the name of the sample
     * @return the name of the sample
     */
    public String getiSampleID() {
        return iSampleID;
    }

    /**
     * Sets the name of the sample
     * @param iSampleID the name of the sample
     */
    public void setiSampleID(String iSampleID) {
        this.iSampleID = iSampleID;
    }

    /**
     * Returns the identification files
     * @return the identification files
     */
    public ArrayList<File> getIdFiles() {
        return idFiles;
    }

    /**
     * Sets the identification files
     * @param idFiles the identification files
     */
    public void setIdFiles(ArrayList<File> idFiles) {
        this.idFiles = idFiles;
    }

    /**
     * Returns the pride file
     * @return the pride file
     */
    public File getPrideFile() {
        return prideFile;
    }

    /**
     * Sets the pride file
     * @param prideFile the pride file
     */
    public void setPrideFile(File prideFile) {
        this.prideFile = prideFile;
    }

    /**
     * Returns the replicate number
     * @return the replicate number
     */
    public int getReplicate() {
        return replicate;
    }

    /**
     * Sets the replicate number
     * @param replicate the replicate number
     */
    public void setReplicate(int replicate) {
        this.replicate = replicate;
    }

    /**
     * Returns the spectrum files
     * @return the spectrum files
     */
    public ArrayList<File> getSpectrumFiles() {
        return spectrumFiles;
    }

    /**
     * Sets the spectrum files
     * @param spectrumFiles the spectrum files
     */
    public void setSpectrumFiles(ArrayList<File> spectrumFiles) {
        this.spectrumFiles = spectrumFiles;
    }

    /**
     * Returns the identification parameters
     * @return the identification parameters
     */
    public SearchParameters getIdentificationParameters() {
        return identificationParameters;
    }

    /**
     * Sets the identification parameters
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(SearchParameters identificationParameters) {
        this.identificationParameters = identificationParameters;
    }
    
    /**
     * Returns a list of file names for inputs of comma separated files
     * @param cliInput the CLI input
     * @return a list of file names
     */
    public static ArrayList<String> splitInput(String cliInput) {
        ArrayList<String> results = new ArrayList<String>();
        for (String file : cliInput.split(",")) {
                results.add(file.trim());
        }
        return results;
    }
    
}
