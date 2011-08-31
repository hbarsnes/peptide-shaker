package eu.isas.peptideshaker;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.AdvocateFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.scoring.InputMap;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.gui.WaitingDialog;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.fileimport.FileImporter;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will be responsible for the identification import and the associated calculations
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideShaker {

    /**
     * If set to true, detailed information is sent to the waiting dialog.
     */
    private boolean detailedReport = false;
    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The psm map
     */
    private PsmSpecificMap psmMap;
    /**
     * The peptide map
     */
    private PeptideSpecificMap peptideMap;
    /**
     * The protein map
     */
    private ProteinMap proteinMap;
    /**
     * The id importer will import and process the identifications
     */
    private FileImporter fileImporter = null;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The location of the folder used for serialization of matches
     */
    public final static String SERIALIZATION_DIRECTORY = "matches";

    /**
     * Constructor without mass specification. Calculation will be done on new maps
     * which will be retrieved as compomics utilities parameters.
     *
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        psmMap = new PsmSpecificMap();
        peptideMap = new PeptideSpecificMap();
        proteinMap = new ProteinMap();
    }

    /**
     * Constructor with map specifications.
     *
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     * @param psMaps            the peptide shaker maps
     */
    public PeptideShaker(MsExperiment experiment, Sample sample, int replicateNumber, PSMaps psMaps) {
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        this.psmMap = psMaps.getPsmSpecificMap();
        this.peptideMap = psMaps.getPeptideSpecificMap();
        this.proteinMap = psMaps.getProteinMap();
    }

    /**
     * Method used to import identification from identification result files
     *
     * @param waitingDialog     A dialog to display the feedback
     * @param idFilter          The identification filter to use
     * @param idFiles           The files to import
     * @param spectrumFiles     The corresponding spectra (can be empty: spectra will not be loaded)
     * @param fastaFile         The database file in the fasta format
     * @param searchParameters  The search parameters
     */
    public void importFiles(WaitingDialog waitingDialog, IdFilter idFilter, ArrayList<File> idFiles, ArrayList<File> spectrumFiles, File fastaFile, SearchParameters searchParameters) {

        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);

        if (analysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION) == null) {
            analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification());
            Identification identification = analysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            identification.setInMemory(false);
            identification.setAutomatedMemoryManagement(true);
            identification.setSerializationDirectory(SERIALIZATION_DIRECTORY);
            fileImporter = new FileImporter(this, waitingDialog, analysis, idFilter);
            fileImporter.importFiles(idFiles, spectrumFiles, fastaFile, searchParameters);
        } else {
            fileImporter = new FileImporter(this, waitingDialog, analysis, idFilter);
            fileImporter.importFiles(spectrumFiles);
        }
    }

    /**
     * This method processes the identifications and fills the peptide shaker maps
     *
     * @param inputMap          The input map
     * @param waitingDialog     A dialog to display the feedback
     */
    public void processIdentifications(InputMap inputMap, WaitingDialog waitingDialog) {

        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        if (!identification.memoryCheck()) {
            waitingDialog.appendReport("PeptideShaker is encountering memory issues! See http://peptide-shaker.googlecode.com for help.");
        }
        try {
            waitingDialog.appendReport("Computing assumptions probabilities.");
            inputMap.estimateProbabilities();
            attachAssumptionsProbabilities(inputMap);
            waitingDialog.appendReport("Computing PSMs probabilities.");
            setFirstHit();
            ArrayList<String> modifiedPsms = fillPsmMap(inputMap);
            psmMap.cure();
            psmMap.estimateProbabilities();
            attachSpectrumProbabilities();
            waitingDialog.appendReport("Computing peptide probabilities.");
            identification.buildPeptidesAndProteins();
            fillPeptideMaps();
            peptideMap.cure();
            peptideMap.estimateProbabilities();
            attachPeptideProbabilities();
            waitingDialog.appendReport("Scoring PTMs.");
            scorePSMPTMs(modifiedPsms);
            scorePeptidePTMs();
            waitingDialog.appendReport("Computing protein probabilities.");
            fillProteinMap();
            proteinMap.estimateProbabilities();
            attachProteinProbabilities();
            waitingDialog.appendReport("Trying to resolve protein inference issues.");
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occurred while working on the identification. See the log file for more details.");
        }

        try {
            cleanProteinGroups(waitingDialog);
        } catch (Exception e) {
            waitingDialog.appendReport("An error occured while trying to resolve protein inference issues.");
            e.printStackTrace();
        }
        proteinMap = new ProteinMap();
        try {
            fillProteinMap();
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occurred while working on the identification. See the log file for more details.");
        }
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();

        waitingDialog.appendReport("Validating identifications at 1% FDR.");
        fdrValidation();

        String report = "Identification processing completed.\n\n";
        ArrayList<Integer> suspiciousInput = inputMap.suspiciousInput();
        ArrayList<String> suspiciousPsms = psmMap.suspiciousInput();
        ArrayList<String> suspiciousPeptides = peptideMap.suspiciousInput();
        boolean suspiciousProteins = proteinMap.suspicousInput();

        if (suspiciousInput.size() > 0
                || suspiciousPsms.size() > 0
                || suspiciousPeptides.size() > 0
                || suspiciousProteins) {

            // @TODO: display this in a separate dialog??
            if (detailedReport) {

                report += "The following identification classes retieved non robust statistical estimations, "
                        + "we advice to control the quality of the corresponding matches: \n";

                boolean firstLine = true;

                for (int searchEngine : suspiciousInput) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += AdvocateFactory.getInstance().getAdvocate(searchEngine).getName();
                }

                if (suspiciousInput.size() > 0) {
                    report += " identifications.\n";
                }

                firstLine = true;

                for (String fraction : suspiciousPsms) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += fraction;
                }

                report += " charged spectra.\n";

                firstLine = true;

                for (String fraction : suspiciousPeptides) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        report += ", ";
                    }
                    report += fraction;
                }

                report += " modified peptides.\n";

                if (suspiciousProteins) {
                    report += "proteins. \n";
                }
            }
        }

        waitingDialog.appendReport(report);
        identification.addUrParam(new PSMaps(proteinMap, psmMap, peptideMap));
        waitingDialog.setRunFinished();
    }

    /**
     * Makes a preliminary validation of hits. By default a 1% FDR is used for all maps
     */
    public void fdrValidation() {
        TargetDecoyMap currentMap = proteinMap.getTargetDecoyMap();
        TargetDecoyResults currentResults = currentMap.getTargetDecoyResults();
        currentResults.setClassicalEstimators(true);
        currentResults.setClassicalValidation(true);
        currentResults.setFdrLimit(1.0);
        currentMap.getTargetDecoySeries().getFDRResults(currentResults);

        for (String mapKey : peptideMap.getKeys()) {
            currentMap = peptideMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(1.0);
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        for (int mapKey : psmMap.getKeys().keySet()) {
            currentMap = psmMap.getTargetDecoyMap(mapKey);
            currentResults = currentMap.getTargetDecoyResults();
            currentResults.setClassicalEstimators(true);
            currentResults.setClassicalValidation(true);
            currentResults.setFdrLimit(1.0);
            currentMap.getTargetDecoySeries().getFDRResults(currentResults);
        }

        validateIdentifications();
    }

    /**
     * Processes the identifications if a change occured in the psm map
     *
     * @throws Exception    Exception thrown whenever it is attempted to attach more
     *                      than one identification per search engine per spectrum
     */
    public void spectrumMapChanged() throws Exception {
        peptideMap = new PeptideSpecificMap();
        proteinMap = new ProteinMap();
        attachSpectrumProbabilities();
        fillPeptideMaps();
        peptideMap.cure();
        peptideMap.estimateProbabilities();
        attachPeptideProbabilities();
        fillProteinMap();
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the peptide map
     * @throws Exception    Exception thrown whenever it is attempted to attach
     *                      more than one identification per search engine per spectrum
     */
    public void peptideMapChanged() throws Exception {
        proteinMap = new ProteinMap();
        attachPeptideProbabilities();
        fillProteinMap();
        proteinMap.estimateProbabilities();
        attachProteinProbabilities();
        cleanProteinGroups(null);
    }

    /**
     * Processes the identifications if a change occured in the protein map
     */
    public void proteinMapChanged() {
        attachProteinProbabilities();
    }

    /**
     * This method will flag validated identifications
     */
    public void validateIdentifications() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();

        double proteinThreshold = proteinMap.getTargetDecoyMap().getTargetDecoyResults().getScoreLimit();
        for (String proteinKey : identification.getProteinIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            if (psParameter.getProteinProbabilityScore() <= proteinThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double peptideThreshold;
        for (String peptideKey : identification.getPeptideIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            peptideThreshold = peptideMap.getTargetDecoyMap(peptideMap.getCorrectedKey(psParameter.getSecificMapKey())).getTargetDecoyResults().getScoreLimit();
            if (psParameter.getPeptideProbabilityScore() <= peptideThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }

        double psmThreshold;
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
            psmThreshold = psmMap.getTargetDecoyMap(psmMap.getCorrectedKey(psParameter.getSecificMapKey())).getTargetDecoyResults().getScoreLimit();
            if (psParameter.getPsmProbabilityScore() <= psmThreshold) {
                psParameter.setValidated(true);
            } else {
                psParameter.setValidated(false);
            }
        }
    }

    /**
     * When two different sequences result in the same score for a given search engine, this method will retain the peptide belonging to the protein leading to the most spectra.
     * This method is typically useful for Isoleucine/Leucine issues.
     */
    private void setFirstHit() throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        ArrayList<String> conflictingPSMs = new ArrayList<String>();
        HashMap<String, Integer> spectrumCounting = new HashMap<String, Integer>();
        boolean conflict;
        SpectrumMatch spectrumMatch;
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            conflict = false;
            for (int se : spectrumMatch.getAdvocates()) {
                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(se).get(spectrumMatch.getFirstHit(se).getEValue())) {
                    if (!peptideAssumption.getPeptide().getSequence().equals(spectrumMatch.getFirstHit(se).getPeptide().getSequence())) {
                        conflict = true;
                    }
                    for (String accession : peptideAssumption.getPeptide().getParentProteins()) {
                        if (!spectrumCounting.containsKey(accession)) {
                            spectrumCounting.put(accession, 0);
                        }
                        spectrumCounting.put(accession, spectrumCounting.get(accession) + 1);
                    }
                }
            }
            if (conflict) {
                conflictingPSMs.add(spectrumMatch.getKey());
            }
        }
        SpectrumMatch conflictingPSM;
        PeptideAssumption bestAssumption;
        int maxCount;
        for (String conflictKey : conflictingPSMs) {
            conflictingPSM = identification.getSpectrumMatch(conflictKey);
            maxCount = 0;
            for (int se : conflictingPSM.getAdvocates()) {
                bestAssumption = conflictingPSM.getFirstHit(se);
                for (String accession : bestAssumption.getPeptide().getParentProteins()) {
                    if (spectrumCounting.get(accession) > maxCount) {
                        maxCount = spectrumCounting.get(accession);
                    }
                }
                for (PeptideAssumption peptideAssumption : conflictingPSM.getAllAssumptions(se).get(conflictingPSM.getFirstHit(se).getEValue())) {
                    if (!peptideAssumption.getPeptide().getSequence().equals(conflictingPSM.getFirstHit(se).getPeptide().getSequence())) {
                        for (String accession : peptideAssumption.getPeptide().getParentProteins()) {
                            if (spectrumCounting.get(accession) > maxCount) {
                                bestAssumption = peptideAssumption;
                                maxCount = spectrumCounting.get(accession);
                            }
                        }
                        conflictingPSM.setFirstHit(se, bestAssumption);
                        identification.setMatchChanged(conflictingPSM);
                    }
                }
            }
        }
    }

    /**
     * Fills the psm specific map
     *
     * @param inputMap       The input map
     */
    private ArrayList<String> fillPsmMap(InputMap inputMap) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        HashMap<String, Double> identifications;
        HashMap<Double, PeptideAssumption> peptideAssumptions;
        PSParameter psParameter;
        PeptideAssumption peptideAssumption, bestAssumption;
        SpectrumMatch spectrumMatch;
        ArrayList<String> modifiedPsms = new ArrayList<String>();
        if (inputMap.isMultipleSearchEngines()) {
            for (String spectrumKey : identification.getSpectrumIdentification()) {
                psParameter = new PSParameter();
                identifications = new HashMap<String, Double>();
                peptideAssumptions = new HashMap<Double, PeptideAssumption>();
                String id;
                double p, pScore = 1;
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                    p = psParameter.getSearchEngineProbability();
                    pScore = pScore * p;
                    id = peptideAssumption.getPeptide().getKey();
                    if (identifications.containsKey(id)) {
                        p = identifications.get(id) * p;
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    } else {
                        identifications.put(id, p);
                        peptideAssumptions.put(p, peptideAssumption);
                    }
                }
                double pMin = Collections.min(identifications.values());
                psParameter.setSpectrumProbabilityScore(pScore);
                psParameter.setSecificMapKey(psmMap.getKey(spectrumMatch) + "");
                identification.addMatchParameter(spectrumKey, psParameter);
                bestAssumption = peptideAssumptions.get(pMin);
                spectrumMatch.setBestAssumption(bestAssumption);
                identification.setMatchChanged(spectrumMatch);
                if (Peptide.isModified(bestAssumption.getPeptide().getKey())) {
                    modifiedPsms.add(spectrumKey);
                }
                psmMap.addPoint(pScore, spectrumMatch);
            }
        } else {
            double eValue;
            for (String spectrumKey : identification.getSpectrumIdentification()) {
                psParameter = new PSParameter();
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (int searchEngine : spectrumMatch.getAdvocates()) {
                    peptideAssumption = spectrumMatch.getFirstHit(searchEngine);
                    eValue = peptideAssumption.getEValue();
                    psParameter.setSpectrumProbabilityScore(eValue);
                    spectrumMatch.setBestAssumption(peptideAssumption);
                    identification.setMatchChanged(spectrumMatch);
                    if (Peptide.isModified(peptideAssumption.getPeptide().getKey())) {
                        modifiedPsms.add(spectrumKey);
                    }
                    psmMap.addPoint(eValue, spectrumMatch);
                }
                psParameter.setSecificMapKey(psmMap.getKey(spectrumMatch) + "");
                identification.addMatchParameter(spectrumKey, psParameter);
            }
        }
        return modifiedPsms;
    }

    /**
     * Attaches the spectrum posterior error probabilities to the peptide assumptions
     */
    private void attachAssumptionsProbabilities(InputMap inputMap) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        ArrayList<Double> eValues;
        double previousP, newP;
        SpectrumMatch spectrumMatch;
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            for (int searchEngine : spectrumMatch.getAdvocates()) {
                eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(searchEngine).keySet());
                Collections.sort(eValues);
                previousP = 0;
                for (double eValue : eValues) {
                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(searchEngine).get(eValue)) {
                        psParameter = new PSParameter();
                        newP = inputMap.getProbability(searchEngine, eValue);
                        if (newP > previousP) {
                            psParameter.setSearchEngineProbability(newP);
                            previousP = newP;
                        } else {
                            psParameter.setSearchEngineProbability(previousP);
                        }
                        peptideAssumption.addUrParam(psParameter);
                    }
                }
            }
            identification.setMatchChanged(spectrumMatch);
        }
    }

    /**
     * Attaches the spectrum posterior error probabilities to the spectrum matches
     */
    private void attachSpectrumProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (String spectrumKey : identification.getSpectrumIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
            psParameter.setPsmProbability(psmMap.getProbability(psParameter.getSecificMapKey(), psParameter.getPsmProbabilityScore()));
        }
    }

    /**
     * Attaches scores to possible PTM locations to spectrum matches 
     */
    public void scorePSMPTMs(ArrayList<String> inspectedSpectra) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        SpectrumMatch spectrumMatch;
        for (String spectrumKey : inspectedSpectra) {
            spectrumMatch = identification.getSpectrumMatch(spectrumKey);
            scorePTMs(spectrumMatch);
        }
    }

    /**
     * Attaches scores to possible PTM locations to peptide matches
     */
    public void scorePeptidePTMs() throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PeptideMatch peptideMatch;
        for (String peptideKey : identification.getPeptideIdentification()) {
            if (Peptide.isModified(peptideKey)) {
                peptideMatch = identification.getPeptideMatch(peptideKey);
                scorePTMs(peptideMatch);
            }
        }
    }

    /**
     * Scores the PTMs for a peptide match
     */
    public void scorePTMs(PeptideMatch peptideMatch) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSPtmScores psmScores, peptideScores = new PSPtmScores();
        PtmScoring spectrumScoring;
        ArrayList<String> variableModifications = new ArrayList<String>();
        SpectrumMatch spectrumMatch;
        for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
            if (modificationMatch.isVariable()
                    && modificationMatch.getTheoreticPtm().getType() == PTM.MODAA
                    && !variableModifications.contains(modificationMatch.getTheoreticPtm().getName())) {
                variableModifications.add(modificationMatch.getTheoreticPtm().getName());
            }
        }
        if (variableModifications.size() > 0) {
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                for (String modification : variableModifications) {
                    if (!peptideScores.containsPtm(modification)) {
                        peptideScores.addPtmScoring(modification, new PtmScoring(modification));
                    }
                    psmScores = (PSPtmScores) spectrumMatch.getUrParam(peptideScores);
                    if (psmScores != null) {
                        spectrumScoring = psmScores.getPtmScoring(modification);
                        if (spectrumScoring != null) {
                            peptideScores.getPtmScoring(modification).addAll(spectrumScoring);
                        }
                    }
                }
            }
            peptideMatch.addUrParam(peptideScores);
            identification.setMatchChanged(peptideMatch);
        }
    }

    /**
     * Scores PTM locations for a desired spectrumMatch
     * 
     * @param spectrumMatch The spectrum match of interest
     * @throws Exception    exception thrown whenever an error occurred while reading/writing the an identification match
     */
    public void scorePTMs(SpectrumMatch spectrumMatch) throws Exception {
        attachDeltaScore(spectrumMatch);
        attachAScore(spectrumMatch);
    }
    
    /**
     * Scores the PTM locations using the delta score
     * @param spectrumMatch the spectrum match of interest
     * @throws Exception    exception thrown whenever an error occurred while reading/writing the an identification match
     */
    private void attachDeltaScore(SpectrumMatch spectrumMatch) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        PSParameter psParameter = new PSParameter();
        double p1, p2;
        String mainSequence, modificationName;
        ArrayList<String> modifications;
        HashMap<String, ArrayList<Integer>> modificationProfiles = new HashMap<String, ArrayList<Integer>>();
        PSPtmScores ptmScores;
        PtmScoring ptmScoring;
        ptmScores = new PSPtmScores();
        psParameter = (PSParameter) spectrumMatch.getBestAssumption().getUrParam(psParameter);
        p1 = psParameter.getSearchEngineProbability();
        if (p1 < 1) {
            mainSequence = spectrumMatch.getBestAssumption().getPeptide().getSequence();
            p2 = 1;
            modifications = new ArrayList<String>();
            for (ModificationMatch modificationMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
                if (modificationMatch.isVariable()
                        && modificationMatch.getTheoreticPtm().getType() == PTM.MODAA) {
                    modificationName = modificationMatch.getTheoreticPtm().getName();
                    if (!modifications.contains(modificationName)) {
                        modifications.add(modificationName);
                        modificationProfiles.put(modificationName, new ArrayList<Integer>());
                    }
                    modificationProfiles.get(modificationName).add(modificationMatch.getModificationSite());
                }
            }
            if (modifications.size() > 0) {
                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                    if (peptideAssumption.getRank() > 1 && peptideAssumption.getPeptide().getSequence().equals(mainSequence)) {
                        psParameter = (PSParameter) peptideAssumption.getUrParam(psParameter);
                        if (psParameter.getSearchEngineProbability() < p2) {
                            p2 = psParameter.getSearchEngineProbability();
                        }
                    }
                }
                for (String mod : modifications) {
                    ptmScoring = new PtmScoring(mod);
                    ptmScoring.addDeltaScore(modificationProfiles.get(mod), (p2 - p1) * 100);
                    ptmScores.addPtmScoring(mod, ptmScoring);
                }
                spectrumMatch.addUrParam(ptmScores);
                identification.setMatchChanged(spectrumMatch);
            }
        }
    }
    
    /**
     * Scores the PTM locations using the delta score
     * @param spectrumMatch the spectrum match of interest
     * @throws Exception    exception thrown whenever an error occurred while reading/writing the an identification match
     */
    private void attachAScore(SpectrumMatch spectrumMatch) throws Exception {
        
    }

    /**
     * Fills the peptide specific map
     */
    private void fillPeptideMaps() throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch;
        for (String peptideKey : identification.getPeptideIdentification()) {
            probaScore = 1;
            peptideMatch = identification.getPeptideMatch(peptideKey);
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, psParameter);
                probaScore = probaScore * psParameter.getPsmProbability();
            }
            psParameter = new PSParameter();
            psParameter.setPeptideProbabilityScore(probaScore);
            psParameter.setSecificMapKey(peptideMap.getKey(peptideMatch));
            identification.addMatchParameter(peptideKey, psParameter);
            peptideMap.addPoint(probaScore, peptideMatch);
        }
    }

    /**
     * Attaches the peptide posterior error probabilities to the peptide matches
     */
    private void attachPeptideProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        for (String peptideKey : identification.getPeptideIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            psParameter.setPeptideProbability(peptideMap.getProbability(psParameter.getSecificMapKey(), psParameter.getPeptideProbabilityScore()));
        }
    }

    /**
     * Fills the protein map
     */
    private void fillProteinMap() throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        double probaScore;
        PSParameter psParameter = new PSParameter();
        ProteinMatch proteinMatch;
        for (String proteinKey : identification.getProteinIdentification()) {
            probaScore = 1;
            proteinMatch = identification.getProteinMatch(proteinKey);
            for (String peptideKey : proteinMatch.getPeptideMatches()) {
                psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
                probaScore = probaScore * psParameter.getPeptideProbability();
            }
            psParameter = new PSParameter();
            psParameter.setProteinProbabilityScore(probaScore);
            identification.addMatchParameter(proteinKey, psParameter);
            proteinMap.addPoint(probaScore, proteinMatch.isDecoy());
        }
    }

    /**
     * Attaches the protein posterior error probability to the protein matches
     */
    private void attachProteinProbabilities() {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        double proteinProbability;
        for (String proteinKey : identification.getProteinIdentification()) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            proteinProbability = proteinMap.getProbability(psParameter.getProteinProbabilityScore());
            psParameter.setProteinProbability(proteinProbability);
        }
    }

    /**
     * Solves protein inference issues when possible.
     * @throws Exception    exception thrown whenever it is attempted to attach two different spectrum matches to the same spectrum from the same search engine.
     */
    private void cleanProteinGroups(WaitingDialog waitingDialog) throws Exception {
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        PSParameter psParameter = new PSParameter();
        boolean better;
        ProteinMatch proteinShared, proteinUnique;
        double sharedProteinProbabilityScore, uniqueProteinProbabilityScore;
        ArrayList<String> toRemove = new ArrayList<String>();
        for (String proteinSharedKey : identification.getProteinIdentification()) {
            if (ProteinMatch.getNProteins(proteinSharedKey) > 1) {
                psParameter = (PSParameter) identification.getMatchParameter(proteinSharedKey, psParameter);
                sharedProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                if (sharedProteinProbabilityScore < 1) {
                    better = false;
                    for (String proteinUniqueKey : identification.getProteinIdentification()) {
                        if (ProteinMatch.contains(proteinSharedKey, proteinUniqueKey)) {
                            psParameter = (PSParameter) identification.getMatchParameter(proteinUniqueKey, psParameter);
                            uniqueProteinProbabilityScore = psParameter.getProteinProbabilityScore();
                            proteinUnique = identification.getProteinMatch(proteinUniqueKey);
                            proteinShared = identification.getProteinMatch(proteinSharedKey);
                            for (String sharedPeptideKey : proteinShared.getPeptideMatches()) {
                                proteinUnique.addPeptideMatch(sharedPeptideKey);
                            }
                            identification.setMatchChanged(proteinUnique);
                            if (uniqueProteinProbabilityScore <= sharedProteinProbabilityScore) {
                                better = true;
                            }
                        }
                    }
                    if (better) {
                        toRemove.add(proteinSharedKey);
                    }
                }
            }
        }
        for (String proteinKey : toRemove) {
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            proteinMap.removePoint(psParameter.getProteinProbabilityScore(), ProteinMatch.isDecoy(proteinKey));
            identification.removeMatch(proteinKey);
        }
        int nSolved = toRemove.size();
        int nLeft = 0;
        String mainKey = null;
        boolean similarityFound, allSimilar;
        ArrayList<String> primaryDescription, secondaryDescription, accessions;
        for (String proteinKey : identification.getProteinIdentification()) {
            accessions = new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(proteinKey)));
            Collections.sort(accessions);
            mainKey = accessions.get(0);
            if (accessions.size() > 1) {
                similarityFound = false;
                allSimilar = false;
                psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
                for (int i = 0; i < accessions.size() - 1; i++) {
                    primaryDescription = parseDescription(accessions.get(i));
                    for (int j = i + 1; j < accessions.size(); j++) {
                        secondaryDescription = parseDescription(accessions.get(j));
                        if (getSimilarity(primaryDescription, secondaryDescription)) {
                            similarityFound = true;
                            mainKey = accessions.get(i);
                            break;
                        }
                    }
                    if (similarityFound) {
                        break;
                    }
                }
                if (similarityFound) {
                    allSimilar = true;
                    for (String key : accessions) {
                        if (!mainKey.equals(key)) {
                            primaryDescription = parseDescription(mainKey);
                            secondaryDescription = parseDescription(key);
                            if (!getSimilarity(primaryDescription, secondaryDescription)) {
                                allSimilar = false;
                                break;
                            }
                        }
                    }
                }
                if (!similarityFound) {
                    psParameter.setGroupClass(PSParameter.UNRELATED);
                    nLeft++;
                } else if (!allSimilar) {
                    psParameter.setGroupClass(PSParameter.ISOFORMS_UNRELATED);
                    nSolved++;
                } else {
                    psParameter.setGroupClass(PSParameter.ISOFORMS);
                    nSolved++;
                }
            }
            if (ProteinMatch.getNProteins(proteinKey) > 1) {
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                if (!proteinMatch.getMainMatch().equals(mainKey)) {
                    proteinMatch.setMainMatch(mainKey);
                    identification.setMatchChanged(proteinMatch);
                }
            }
        }
        if (waitingDialog != null) {
            waitingDialog.appendReport(nSolved + " conflicts resolved. " + nLeft + " protein groups remaining.");
        }
    }

    /**
     * Parses a protein description retaining only words longer than 3 characters
     * @param proteinAccession the accession of the inspected protein
     * @return description words longer than 3 characters
     */
    private ArrayList<String> parseDescription(String proteinAccession) {
        String description = "";
        try {
            sequenceFactory.getHeader(proteinAccession).getDescription();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String component : description.split(" ")) {
            if (component.length() > 3) {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * Simplistic method comparing protein descriptions. Returns true if both descriptions are of same length and present more than half similar words.
     * @param primaryDescription    The parsed description of the first protein
     * @param secondaryDescription  The parsed description of the second protein
     * @return  a boolean indicating whether the descriptions are similar
     */
    private boolean getSimilarity(ArrayList<String> primaryDescription, ArrayList<String> secondaryDescription) {
        if (primaryDescription.size() == secondaryDescription.size()) {
            int nMatch = 0;
            for (int i = 0; i < primaryDescription.size(); i++) {
                if (primaryDescription.get(i).equals(secondaryDescription.get(i))) {
                    nMatch++;
                }
            }
            if (nMatch >= primaryDescription.size() / 2) {
                return true;
            }
        }
        return false;
    }
}
