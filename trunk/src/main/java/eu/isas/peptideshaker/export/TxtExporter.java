package eu.isas.peptideshaker.export;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Contains methods for exporting the search engine results to text files.
 *
 * @deprecated use the standard reports instead
 * @author Marc Vaudel
 */
public class TxtExporter {

    /**
     * Separator for text export. Hard coded for now, could be user setting.
     */
    private static final String SEPARATOR = "\t";
    /**
     * The experiment to export.
     */
    private MsExperiment experiment;
    /**
     * The sample considered.
     */
    private Sample sample;
    /**
     * The replicate considered.
     */
    private int replicateNumber;
    /**
     * Name of the file containing the identification information at the protein
     * level.
     */
    private String proteinFile;
    /**
     * Name of the file containing the identification information at the peptide
     * level.
     */
    private String peptideFile;
    /**
     * Name of the file containing the identification information at the PSM
     * level.
     */
    private String psmFile;
    /**
     * Name of the file containing the identification information at the peptide
     * assumption level.
     */
    private String assumptionFile;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The search parameters used for the search.
     */
    private SearchParameters searchParameters;

    /**
     * Creates a TxtExporter object.
     *
     * @param experiment the ms experiment
     * @param sample the sample
     * @param replicateNumber the replicate number
     * @param identificationFeaturesGenerator
     * @param searchParameters the search parameters
     */
    public TxtExporter(MsExperiment experiment, Sample sample, int replicateNumber, IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.searchParameters = searchParameters;

        proteinFile = "PeptideShaker_" + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_proteins.txt";
        peptideFile = "PeptideShaker_" + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_peptides.txt";
        psmFile = "PeptideShaker_" + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_psms.txt";
        //assumptionFile = "PeptideShaker " + experiment.getReference() + "_" + sample.getReference() + "_" + replicateNumber + "_assumptions.txt";
    }

    /**
     * Exports the results to text files.
     *
     * @param waitingHandler a waitingHandler displaying progress to the user,
     * can be null
     * @param folder the folder to store the results in.
     * @return true if the export was successful
     */
    public boolean exportResults(WaitingHandler waitingHandler, File folder) {

        try {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Exporting Proteins. Please Wait...");
            }

            Writer proteinWriter = new BufferedWriter(new FileWriter(new File(folder, proteinFile)));
            String content = "Protein" + SEPARATOR + "Equivalent proteins" + SEPARATOR + "Group class" + SEPARATOR + "Gene name" + SEPARATOR + "Chromosome"
                    + SEPARATOR + "n peptides" + SEPARATOR + "n spectra" + SEPARATOR + "n peptides validated" + SEPARATOR + "n spectra validated"
                    + SEPARATOR + "MW" + SEPARATOR + "NSAF" + SEPARATOR + "Sequence coverage" + SEPARATOR + "Observable coverage" + SEPARATOR + "p score"
                    + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR + "Description" + System.getProperty("line.separator");
            proteinWriter.write(content);

            identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

            if (waitingHandler != null) {
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size()
                        + identification.getPeptideIdentification().size()
                        + 2 * identification.getSpectrumIdentificationSize());
            }

            for (String proteinKey : identification.getProteinIdentification()) {

                proteinWriter.write(getProteinLine(proteinKey));

                if (waitingHandler != null) {
                    waitingHandler.increasePrimaryProgressCounter();
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            proteinWriter.close();

            // write the peptides
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Exporting Peptides. Please Wait...");
            }
            Writer peptideWriter = new BufferedWriter(new FileWriter(new File(folder, peptideFile)));
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM location confidence" + SEPARATOR
                    + "n Spectra" + SEPARATOR + "n Spectra Validated" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR
                    + "Validated" + System.getProperty("line.separator");
            peptideWriter.write(content);

            for (String peptideKey : identification.getPeptideIdentification()) {

                peptideWriter.write(getPeptideLine(peptideKey));

                if (waitingHandler != null) {
                    waitingHandler.increasePrimaryProgressCounter();
                }

                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            peptideWriter.close();

            // write the spectra
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Exporting Spectra. Please Wait...");
            }

            Writer spectrumWriter = new BufferedWriter(new FileWriter(new File(folder, psmFile)));
            content = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "D-score" + SEPARATOR + "A-score"
                    + SEPARATOR + "PTM location confidence" + SEPARATOR + "Spectrum Charge" + SEPARATOR + "Identification Charge" + SEPARATOR + "Spectrum"
                    + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)" + SEPARATOR + "Precursor RT" + SEPARATOR + "Precursor mz"
                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + "Isotope" + SEPARATOR + "Mascot Score" + SEPARATOR
                    + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value" + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p"
                    + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + System.getProperty("line.separator");
            spectrumWriter.write(content);

            for (String spectrumFile : identification.getSpectrumFiles()) {
                identification.loadSpectrumMatches(spectrumFile, waitingHandler);
                identification.loadSpectrumMatchParameters(spectrumFile, new PSParameter(), waitingHandler);
                for (String spectrumKey : identification.getSpectrumIdentification(spectrumFile)) {

                    spectrumWriter.write(getSpectrumLine(spectrumKey));

                    if (waitingHandler != null) {
                        waitingHandler.increasePrimaryProgressCounter();
                    }

                    if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                        break;
                    }
                }
            }

            spectrumWriter.close();

            // write the assumptions
//            if (progressDialog != null) {
//                progressDialog.setTitle("Exporting Assumptions. Please Wait...");
//            }
//            
//            Writer assumptionWriter = new BufferedWriter(new FileWriter(new File(folder, assumptionFile)));
//            content = "Search Engine" + SEPARATOR + "Rank" + SEPARATOR + "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR
//                    + "Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
//                    + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR + "Isotope" + SEPARATOR + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
//                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + System.getProperty("line.separator");
//            assumptionWriter.write(content);
//            for (String spectrumKey : identification.getSpectrumIdentification()) {
//                assumptionWriter.write(getAssumptionLines(spectrumKey));
//                if(progressDialog != null){
//                  progressDialog.setValue(++progress);
//                    }
//            }
//            
//            assumptionWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exports the protein match as a line of text.
     *
     * @param proteinMatch the protein match to export
     * @return the protein match as a line of text
     */
    private String getProteinLine(String proteinKey) throws Exception {

        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
        StringBuilder line = new StringBuilder();
        line.append(proteinMatch.getMainMatch()).append(SEPARATOR);

        for (String otherAccession : proteinMatch.getTheoreticProteinsAccessions()) {
            if (!otherAccession.equals(proteinMatch.getMainMatch())) {
                line.append(otherAccession).append(" ");
            }
        }

        line.append(SEPARATOR).append(probabilities.getProteinInferenceClass()).append(SEPARATOR);

        // add gene name and chromosome number
        String geneName = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getGeneName();
        String chromosomeNumber = geneFactory.getChromosomeForGeneName(geneName);
        if (geneName != null && !identification.getProteinMatch(proteinKey).isDecoy()) {
            line.append(geneName);
        }
        line.append(SEPARATOR);
        if (chromosomeNumber != null && !identification.getProteinMatch(proteinKey).isDecoy()) {
            line.append(chromosomeNumber);
        }
        line.append(SEPARATOR);

        // peptide count
        line.append(proteinMatch.getPeptideCount()).append(SEPARATOR);

        int nSpectra = 0;
        int nValidatedPeptides = 0;
        int nValidatedPsms = 0;
        PSParameter psParameter = new PSParameter();

        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), psParameter, null);

        for (String peptideKey : proteinMatch.getPeptideMatches()) {

            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
            nSpectra += peptideMatch.getSpectrumCount();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (psParameter.getMatchValidationLevel().isValidated()) {

                nValidatedPeptides++;

                identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {

                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                    if (psParameter.getMatchValidationLevel().isValidated()) {
                        nValidatedPsms++;
                    }
                }
            }
        }

        line.append(nSpectra).append(SEPARATOR).append(nValidatedPeptides).append(SEPARATOR).append(nValidatedPsms).append(SEPARATOR);

        try {
            line.append(sequenceFactory.computeMolecularWeight(proteinMatch.getMainMatch())).append(SEPARATOR);
            line.append(identificationFeaturesGenerator.getSpectrumCounting(proteinKey)).append(SEPARATOR);
            line.append(identificationFeaturesGenerator.getSequenceCoverage(proteinKey, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy()) * 100).append(SEPARATOR);
            line.append(identificationFeaturesGenerator.getObservableCoverage(proteinKey)).append(SEPARATOR);
        } catch (Exception e) {
            line.append("protein not found ").append(SEPARATOR).append(SEPARATOR);
        }

        try {
            line.append(probabilities.getProteinProbabilityScore()).append(SEPARATOR).append(probabilities.getProteinProbability()).append(SEPARATOR);
        } catch (Exception e) {
            line.append(SEPARATOR).append(SEPARATOR);
        }

        if (proteinMatch.isDecoy()) {
            line.append("1").append(SEPARATOR);
        } else {
            line.append("0").append(SEPARATOR);
        }

        line.append(probabilities.getMatchValidationLevel());
        if (!probabilities.getReasonDoubtful().equals("")) {
            line.append(" (").append(probabilities.getReasonDoubtful()).append(")");
        }
        line.append(SEPARATOR);

        try {
            line.append(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription());
        } catch (Exception e) {
            line.append("Protein not found");
        }

        line.append(System.getProperty("line.separator"));

        return line.toString();
    }

    /**
     * Exports the peptide match as a line of text. Note: proteins must be set
     * for the peptide.
     *
     * @param peptideMatch the peptide match to export
     * @return the peptide match as a line of text
     */
    private String getPeptideLine(String peptideKey) throws Exception {

        StringBuilder line = new StringBuilder();
        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

        for (String protein : peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
            line.append(protein).append(" ");
        }

        line.append(SEPARATOR).append(peptideMatch.getTheoreticPeptide().getSequence()).append(SEPARATOR);

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }

        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);

        for (String mod : modifications) {
            if (line.length() > 0) {
                line.append(", ");
            }

            boolean first2 = true;
            line.append(mod).append("(");

            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    line.append(", ");
                }
                line.append(aa);
            }

            line.append(")");
        }

        line.append(SEPARATOR);

        for (String mod : modifications) {

            if (line.length() > 0) {
                line.append(", ");
            }

            PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            line.append(mod).append(" (");

            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                boolean firstSite = true;
                for (int site : ptmScoring.getOrderedPtmLocations()) {
                    if (firstSite) {
                        firstSite = false;
                    } else {
                        line.append(", ");
                    }
                    int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                    if (ptmConfidence == PtmScoring.NOT_FOUND) {
                        line.append(site).append(": Not Scored"); // Well this should not happen
                    } else if (ptmConfidence == PtmScoring.RANDOM) {
                        line.append(site).append(": Random");
                    } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                        line.append(site).append(": Doubtfull");
                    } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                        line.append(site).append(": Confident");
                    } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                        line.append(site).append(": Very Confident");
                    }
                }
            } else {
                line.append("Not Scored");
            }

            line.append(")");
        }

        line.append(SEPARATOR).append(peptideMatch.getSpectrumCount()).append(SEPARATOR);

        PSParameter probabilities = new PSParameter();
        int nSpectraValidated = 0;

        identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), probabilities, null);
        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
            probabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, probabilities);
            if (probabilities.getMatchValidationLevel().isValidated()) {
                nSpectraValidated++;
            }
        }

        line.append(nSpectraValidated).append(SEPARATOR);
        probabilities = (PSParameter) identification.getPeptideMatchParameter(peptideKey, probabilities);

        line.append(probabilities.getPeptideProbabilityScore()).append(SEPARATOR).append(probabilities.getPeptideProbability()).append(SEPARATOR);

        if (peptideMatch.getTheoreticPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
            line.append("1").append(SEPARATOR);
        } else {
            line.append("0").append(SEPARATOR);
        }

        line.append(probabilities.getMatchValidationLevel());
        if (!probabilities.getReasonDoubtful().equals("")) {
            line.append(" (").append(probabilities.getReasonDoubtful()).append(")");
        }

        line.append(System.getProperty("line.separator"));

        return line.toString();
    }

    /**
     * Exports the spectrum match as a line of text. Note: proteins must be set
     * for the best assumption
     *
     * @param spectrumMatch the spectrum match to export
     *
     * @return the spectrum match as a line of text
     */
    private String getSpectrumLine(String psmKey) throws Exception {

        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
        Peptide bestAssumption = spectrumMatch.getBestPeptideAssumption().getPeptide();

        StringBuilder line = new StringBuilder();

        for (String protein : bestAssumption.getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
            line.append(protein).append(" ");
        }

        line.append(SEPARATOR).append(bestAssumption.getSequence()).append(SEPARATOR);

        HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

        for (ModificationMatch modificationMatch : bestAssumption.getModificationMatches()) {
            if (modificationMatch.isVariable()) {
                if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                    modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                }
                modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
            }
        }

        boolean first = true;
        ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
        Collections.sort(modifications);

        for (String mod : modifications) {
            if (first) {
                first = false;
            } else {
                line.append(", ");
            }

            boolean first2 = true;
            line.append(mod).append("(");

            for (int aa : modMap.get(mod)) {
                if (first2) {
                    first2 = false;
                } else {
                    line.append(", ");
                }
                line.append(aa);
            }

            line.append(")");
        }

        line.append(SEPARATOR);

        PSPtmScores ptmScores = new PSPtmScores();

        first = true;

        for (String mod : modifications) {

            if (spectrumMatch.getUrParam(ptmScores) != null) {

                if (first) {
                    first = false;
                } else {
                    line.append(", ");
                }

                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                line.append(mod).append(" (");

                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                    boolean firstSite = true;
                    ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getDSites());
                    Collections.sort(sites);
                    for (int site : sites) {
                        if (firstSite) {
                            firstSite = false;
                        } else {
                            line.append(", ");
                        }
                        line.append(site).append(": ").append(ptmScoring.getDeltaScore(site));
                    }
                } else {
                    line.append("Not Scored");
                }

                line.append(")");
            }
        }
        line.append(SEPARATOR);

        first = true;

        for (String mod : modifications) {

            if (spectrumMatch.getUrParam(ptmScores) != null) {

                if (first) {
                    first = false;
                } else {
                    line.append(", ");
                }
                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                line.append(mod).append(" (");

                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                    boolean firstSite = true;
                    ArrayList<Integer> sites = new ArrayList<Integer>(ptmScoring.getProbabilisticSites());
                    Collections.sort(sites);
                    for (int site : sites) {
                        if (firstSite) {
                            firstSite = false;
                        } else {
                            line.append(", ");
                        }
                        line.append(site).append(": ").append(ptmScoring.getProbabilisticScore(site));
                    }
                } else {
                    line.append("Not Scored");
                }

                line.append(")");
            }
        }
        line.append(SEPARATOR);

        first = true;

        for (String mod : modifications) {

            if (spectrumMatch.getUrParam(ptmScores) != null) {

                if (first) {
                    first = false;
                } else {
                    line.append(", ");
                }

                ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                line.append(mod).append(" (");

                if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {
                    PtmScoring ptmScoring = ptmScores.getPtmScoring(mod);
                    boolean firstSite = true;
                    for (int site : ptmScoring.getOrderedPtmLocations()) {
                        if (firstSite) {
                            firstSite = false;
                        } else {
                            line.append(", ");
                        }
                        int ptmConfidence = ptmScoring.getLocalizationConfidence(site);
                        if (ptmConfidence == PtmScoring.NOT_FOUND) {
                            line.append(site).append(": Not Scored"); // Well this should not happen
                        } else if (ptmConfidence == PtmScoring.RANDOM) {
                            line.append(site).append(": Random");
                        } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                            line.append(site).append(": Doubtfull");
                        } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                            line.append(site).append(": Confident");
                        } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                            line.append(site).append(": Very Confident");
                        }
                    }
                } else {
                    line.append("Not Scored");
                }

                line.append(")");
            }
        }

        line.append(SEPARATOR);
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
        line.append(precursor.getPossibleChargesAsString()).append(SEPARATOR);
        line.append(spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value).append(SEPARATOR);
        line.append(fileName).append(SEPARATOR);
        line.append(spectrumTitle).append(SEPARATOR);

        ArrayList<String> fileNames = new ArrayList<String>();

        for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions()) {
            if (assumption instanceof PeptideAssumption) {
                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(bestAssumption, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                    if (!fileNames.contains(assumption.getIdentificationFile())) {
                        fileNames.add(assumption.getIdentificationFile());
                    }
                }
            }
        }

        Collections.sort(fileNames);

        for (String name : fileNames) {
            line.append(name).append(" ");
        }

        line.append(SEPARATOR);
        line.append(precursor.getRt()).append(SEPARATOR);
        line.append(precursor.getMz()).append(SEPARATOR);
        line.append(spectrumMatch.getBestPeptideAssumption().getPeptide().getMass()).append(SEPARATOR);
        line.append(Math.abs(spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), true, true))).append(SEPARATOR);
        line.append(Math.abs(spectrumMatch.getBestPeptideAssumption().getIsotopeNumber(precursor.getMz()))).append(SEPARATOR);
        Double mascotEValue = null, omssaEValue = null, xtandemEValue = null;
        double mascotScore = 0;

        for (int se : spectrumMatch.getAdvocates()) {
            for (double eValue : spectrumMatch.getAllAssumptions(se).keySet()) {
                for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                    if (assumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                        if (peptideAssumption.getPeptide().isSameSequenceAndModificationStatus(bestAssumption, PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                            if (se == Advocate.Mascot.getIndex()) {
                                if (mascotEValue == null || mascotEValue > eValue) {
                                    mascotEValue = eValue;
                                    mascotScore = ((MascotScore) assumption.getUrParam(new MascotScore(0))).getScore();
                                }
                            } else if (se == Advocate.OMSSA.getIndex()) {
                                if (omssaEValue == null || omssaEValue > eValue) {
                                    omssaEValue = eValue;
                                }
                            } else if (se == Advocate.XTandem.getIndex()) {
                                if (xtandemEValue == null || xtandemEValue > eValue) {
                                    xtandemEValue = eValue;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mascotEValue != null) {
            line.append(mascotScore);
        }

        line.append(SEPARATOR);

        if (mascotEValue != null) {
            line.append(mascotEValue);
        }

        line.append(SEPARATOR);

        if (omssaEValue != null) {
            line.append(omssaEValue);
        }

        line.append(SEPARATOR);

        if (xtandemEValue != null) {
            line.append(xtandemEValue);
        }

        line.append(SEPARATOR);
        PSParameter probabilities = new PSParameter();
        probabilities = (PSParameter) identification.getSpectrumMatchParameter(psmKey, probabilities);

        line.append(probabilities.getPsmProbabilityScore()).append(SEPARATOR).append(probabilities.getPsmProbability()).append(SEPARATOR);

        if (spectrumMatch.getBestPeptideAssumption().getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
            line.append("1").append(SEPARATOR);
        } else {
            line.append("0").append(SEPARATOR);
        }

        line.append(probabilities.getMatchValidationLevel());
        if (!probabilities.getReasonDoubtful().equals("")) {
            line.append(" (").append(probabilities.getReasonDoubtful()).append(")");
        }

        line.append(System.getProperty("line.separator"));

        return line.toString();
    }

    /**
     * Exports the peptide assumptions from a peptide spectrum match as lines of
     * text.
     *
     * @param spectrumMatch the spectrum match to export
     * @param searchParameters the parameters used for the identification
     *
     * @return the peptide assumptions from a peptide spectrum match as lines of
     * text
     */
    private String getAssumptionLines(String spectrumKey, SearchParameters searchParameters) throws Exception {

        // @TODO: would it be faster to send the output directly to the buffered writer than going via a string??
        String line = ""; // @TODO: replace by StringBuilder
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
        ArrayList<Integer> searchEngines = spectrumMatch.getAdvocates();
        Collections.sort(searchEngines);
        String fileName = Spectrum.getSpectrumFile(spectrumMatch.getKey());
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
        Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);

        for (int se : searchEngines) {

            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(se).keySet());
            Collections.sort(eValues);
            int rank = 1;

            for (double eValue : eValues) {
                for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                    if (assumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;

                        if (se == Advocate.Mascot.getIndex()) {
                            line += "M" + SEPARATOR;
                        } else if (se == Advocate.OMSSA.getIndex()) {
                            line += "O" + SEPARATOR;
                        } else if (se == Advocate.XTandem.getIndex()) {
                            line += "X" + SEPARATOR;
                        }

                        line += rank + SEPARATOR;

                        ArrayList<String> accessions = peptideAssumption.getPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy());
                        for (String protein : accessions) {
                            line += protein + " ";
                        }

                        line += SEPARATOR;
                        line += peptideAssumption.getPeptide().getSequence() + SEPARATOR;

                        for (ModificationMatch mod : peptideAssumption.getPeptide().getModificationMatches()) {
                            if (mod.isVariable()) {
                                line += mod.getTheoreticPtm() + "(" + mod.getModificationSite() + ") ";
                            }
                        }

                        line += SEPARATOR;
                        line += assumption.getIdentificationCharge().value + SEPARATOR;
                        line += spectrumTitle + SEPARATOR;
                        line += fileName + SEPARATOR;
                        line += assumption.getIdentificationFile() + SEPARATOR;
                        line += spectrumMatch.getBestPeptideAssumption().getPeptide().getMass() + SEPARATOR;
                        line += spectrumMatch.getBestPeptideAssumption().getDeltaMass(precursor.getMz(), true) + SEPARATOR;
                        line += spectrumMatch.getBestPeptideAssumption().getIsotopeNumber(precursor.getMz()) + SEPARATOR;

                        if (se == Advocate.Mascot.getIndex()) {
                            MascotScore score = (MascotScore) assumption.getUrParam(new MascotScore(0));
                            line += score.getScore() + SEPARATOR;
                            line += assumption.getScore() + SEPARATOR;
                        } else {
                            line += SEPARATOR + SEPARATOR;
                        }

                        if (se == Advocate.OMSSA.getIndex()) {
                            line += assumption.getScore() + "";
                        }

                        line += SEPARATOR;

                        if (se == Advocate.XTandem.getIndex()) {
                            line += assumption.getScore() + "";
                        }

                        line += SEPARATOR;

                        PSParameter probabilities = new PSParameter();
                        probabilities = (PSParameter) assumption.getUrParam(probabilities);

                        try {
                            line += assumption.getScore() + SEPARATOR
                                    + probabilities.getSearchEngineProbability() + SEPARATOR;
                        } catch (Exception e) {
                            line += SEPARATOR + SEPARATOR;
                        }

                        if (peptideAssumption.getPeptide().isDecoy(PeptideShaker.MATCHING_TYPE, searchParameters.getFragmentIonAccuracy())) {
                            line += "1" + SEPARATOR;
                        } else {
                            line += "0" + SEPARATOR;
                        }

                        try {
                            line += probabilities.getMatchValidationLevel();
                            if (!probabilities.getReasonDoubtful().equals("")) {
                                line += " (" + probabilities.getReasonDoubtful() + ")";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        line += System.getProperty("line.separator");
                        rank++;
                    }
                }
            }
        }

        return line;
    }
}
