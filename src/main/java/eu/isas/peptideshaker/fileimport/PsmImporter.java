package eu.isas.peptideshaker.fileimport;

import com.compomics.util.experiment.io.identification.idfilereaders.MascotIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.XTandemIdfileReader;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.gui.parameters.identification.IdentificationAlgorithmParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.parameters.identification.tool_specific.AndromedaParameters;
import com.compomics.util.parameters.identification.tool_specific.OmssaParameters;
import com.compomics.util.parameters.identification.tool_specific.XtandemParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.ModificationSiteMapping;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.AndromedaIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.MsAmandaIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.MzIdentMLIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.PepxmlIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.TideIdfileReader;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.experiment.identification.utils.ModificationUtils;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.io.identification.idfilereaders.NovorIdfileReader;
import com.compomics.util.experiment.io.identification.idfilereaders.OnyaseIdfileReader;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.scoring.psm_scoring.BestMatchSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import static eu.isas.peptideshaker.fileimport.FileImporter.MOD_MASS_TOLERANCE;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class can be used to import PSMs from search engine results.
 *
 * @author Marc Vaudel
 */
public class PsmImporter {

    /**
     * The modification factory.
     */
    private final ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private final SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Indicates whether the check for X!Tandem modifications was done.
     */
    private boolean xTandemModsCheck = false;
    /**
     * The number of first hits.
     */
    private long nPSMs = 0;
    /**
     * The total number of peptide assumptions.
     */
    private long nPeptideAssumptionsTotal = 0;
    /**
     * The progress of the import.
     */
    private int progress = 0;
    /**
     * The number of PSMs which did not pass the import filters.
     */
    private int psmsRejected = 0;
    /**
     * The number of PSMs which were rejected due to a protein issue.
     */
    private int proteinIssue = 0;
    /**
     * The number of PSMs which were rejected due to a peptide issue.
     */
    private int peptideIssue = 0;
    /**
     * The number of PSMs which were rejected due to a precursor issue.
     */
    private int precursorIssue = 0;
    /**
     * The number of PSMs which were rejected due to a modification parsing
     * issue.
     */
    private int modificationIssue = 0;
    /**
     * The number of retained first hits.
     */
    private int nRetained = 0;
    /**
     * The number of peptides where no protein was found.
     */
    private int missingProteins = 0;
    /**
     * The id file reader where the PSMs are from.
     */
    private final IdfileReader fileReader;
    /**
     * The identification file where the PSMs are from.
     */
    private final File idFile;
    /**
     * List of ignored modifications.
     */
    private final HashSet<Integer> ignoredModifications = new HashSet<>(2);
    /**
     * The maximal peptide mass error found in ppm.
     */
    private double maxPeptideErrorPpm = 0;
    /**
     * The maximal peptide mass error found in Da.
     */
    private double maxPeptideErrorDa = 0;
    /**
     * The maximal tag mass error found in ppm.
     */
    private double maxTagErrorPpm = 0;
    /**
     * The maximal tag mass error found in Da.
     */
    private double maxTagErrorDa = 0;
    /**
     * The amount added per amino acid residue as part of the reference mass
     * when converting Dalton tolerances to ppm.
     */
    private final double amountPerAminoAcidResidue = 100.0;
    /**
     * List of charges found.
     */
    private final HashSet<Integer> charges = new HashSet<>();
    /**
     * List of one hit wonders.
     */
    private final HashSet<String> singleProteinList;
    /**
     * Map of proteins found several times with the number of times they
     * appeared as first hit.
     */
    private final HashMap<String, Integer> proteinCount;
    /**
     * The identification object database.
     */
    private final Identification identification;
    /**
     * The input map.
     */
    private final InputMap inputMap;
    /**
     * The exception handler.
     */
    private final ExceptionHandler exceptionHandler;
    /**
     * The identification parameters.
     */
    private final IdentificationParameters identificationParameters;
    /**
     * The sequence provider.
     */
    private final SequenceProvider sequenceProvider;

    /**
     * Constructor.
     *
     * @param identificationParameters the identification parameters
     * @param fileReader the reader of the file which the matches are imported
     * from
     * @param idFile the file which the matches are imported from
     * @param identification the identification object where to store the
     * matches
     * @param inputMap the input map to use for scoring
     * @param proteinCount the protein count of this project
     * @param singleProteinList list of one hit wonders for this project
     * @param sequenceProvider the protein sequence provider
     * @param exceptionHandler handler for exceptions
     */
    public PsmImporter(IdentificationParameters identificationParameters, IdfileReader fileReader, File idFile,
            Identification identification, InputMap inputMap, HashMap<String, Integer> proteinCount, HashSet<String> singleProteinList,
            SequenceProvider sequenceProvider, ExceptionHandler exceptionHandler) {

        this.identificationParameters = identificationParameters;
        this.fileReader = fileReader;
        this.idFile = idFile;
        this.identification = identification;
        this.inputMap = inputMap;
        this.proteinCount = proteinCount;
        this.singleProteinList = singleProteinList;
        this.sequenceProvider = sequenceProvider;
        this.exceptionHandler = exceptionHandler;

    }

    /**
     * Imports PSMs.
     *
     * @param idFileSpectrumMatches the PSMs to import
     * @param nThreads the number of threads to use
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     */
    public void importPsms(LinkedList<SpectrumMatch> idFileSpectrumMatches, int nThreads, WaitingHandler waitingHandler) {

        idFileSpectrumMatches.parallelStream()
                .forEach(spectrumMatch -> importPsm(spectrumMatch, new PeptideSpectrumAnnotator(), waitingHandler));

    }

    /**
     * Imports a PSM.
     *
     * @param algorithmMatch the spectrum match to import
     * @param peptideSpectrumAnnotator the spectrum annotator to use to annotate
     * spectra
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     */
    private void importPsm(SpectrumMatch algorithmMatch, PeptideSpectrumAnnotator peptideSpectrumAnnotator, WaitingHandler waitingHandler) {

        nPSMs++;

        importAssumptions(algorithmMatch, peptideSpectrumAnnotator, waitingHandler);

        long spectrumMatchKey = algorithmMatch.getKey();

        SpectrumMatch dbMatch = identification.getSpectrumMatch(spectrumMatchKey);

        if (dbMatch != null) {

            mergePeptideAssumptions(algorithmMatch.getPeptideAssumptionsMap(), dbMatch.getPeptideAssumptionsMap());
            mergeTagAssumptions(algorithmMatch.getTagAssumptionsMap(), dbMatch.getTagAssumptionsMap());

        } else {

            identification.addObject(algorithmMatch.getKey(), algorithmMatch);

        }

        if (waitingHandler.isRunCanceled()) {
            return;
        }
        waitingHandler.setSecondaryProgressCounter(++progress);
    }

    /**
     * Extracts the assumptions and adds them to the provided map.
     *
     * @param matchAssumptions the match assumptions
     * @param combinedAssumptions the combined assumptions
     */
    private void mergeTagAssumptions(HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> matchAssumptions, HashMap<Integer, TreeMap<Double, ArrayList<TagAssumption>>> combinedAssumptions) {

        for (Entry<Integer, TreeMap<Double, ArrayList<TagAssumption>>> entry : matchAssumptions.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<TagAssumption>> algorithmMap = entry.getValue();
            TreeMap<Double, ArrayList<TagAssumption>> combinedAlgorithmMap = combinedAssumptions.get(algorithm);

            if (combinedAlgorithmMap == null) {

                combinedAssumptions.put(algorithm, algorithmMap);

            } else {

                for (double score : algorithmMap.keySet()) {

                    ArrayList<TagAssumption> scoreAssumptions = algorithmMap.get(score);
                    ArrayList<TagAssumption> combinedScoreAssumptions = combinedAlgorithmMap.get(score);

                    if (combinedScoreAssumptions == null) {

                        combinedAlgorithmMap.put(score, scoreAssumptions);

                    } else {

                        combinedScoreAssumptions.addAll(scoreAssumptions);

                    }
                }
            }
        }
    }

    /**
     * Extracts the assumptions and adds them to the provided map.
     *
     * @param matchAssumptions the match assumptions
     * @param combinedAssumptions the combined assumptions
     */
    private void mergePeptideAssumptions(HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> matchAssumptions, HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> combinedAssumptions) {

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : matchAssumptions.entrySet()) {

            int algorithm = entry.getKey();
            TreeMap<Double, ArrayList<PeptideAssumption>> algorithmMap = entry.getValue();
            TreeMap<Double, ArrayList<PeptideAssumption>> combinedAlgorithmMap = combinedAssumptions.get(algorithm);

            if (combinedAlgorithmMap == null) {

                combinedAssumptions.put(algorithm, algorithmMap);

            } else {

                for (double score : algorithmMap.keySet()) {

                    ArrayList<PeptideAssumption> scoreAssumptions = algorithmMap.get(score);
                    ArrayList<PeptideAssumption> combinedScoreAssumptions = combinedAlgorithmMap.get(score);

                    if (combinedScoreAssumptions == null) {

                        combinedAlgorithmMap.put(score, scoreAssumptions);

                    } else {

                        combinedScoreAssumptions.addAll(scoreAssumptions);

                    }
                }
            }
        }
    }

    /**
     * Import the assumptions. Maps algorithm specific modifications to the
     * generic objects. Relocates aberrant modifications and
     * removes assumptions where not all modifications are mapped. Verifies
     * whether there is a best match for the spectrum according to the search
     * engine score.
     *
     * @param spectrumMatch the spectrum match to import
     * @param assumptions the assumptions to import
     * @param peptideSpectrumAnnotator the spectrum annotator to use to annotate
     * spectra
     * @param waitingHandler waiting handler to display progress and allow
     * canceling the import
     */
    private void importAssumptions(SpectrumMatch spectrumMatch, PeptideSpectrumAnnotator peptideSpectrumAnnotator, WaitingHandler waitingHandler) {
        
        PeptideAssumptionFilter peptideAssumptionFilter = identificationParameters.getPeptideAssumptionFilter();
        SequenceMatchingParameters sequenceMatchingPreferences = identificationParameters.getSequenceMatchingParameters();
        SequenceMatchingParameters modificationSequenceMatchingPreferences = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        String spectrumKey = spectrumMatch.getSpectrumKey();
        String spectrumFileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        HashMap<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> peptideAssumptions = spectrumMatch.getPeptideAssumptionsMap();

        for (Entry<Integer, TreeMap<Double, ArrayList<PeptideAssumption>>> entry : peptideAssumptions.entrySet()) {

            int advocateId = entry.getKey();

            if (advocateId == Advocate.xtandem.getIndex()) {

                verifyXTandemModifications();

            }

            TreeMap<Double, ArrayList<PeptideAssumption>> assumptionsForAdvocate = entry.getValue();

            // Map modifications
            HashSet<Double> scores = new HashSet<>(assumptionsForAdvocate.keySet());

            for (double score : scores) {

                ArrayList<PeptideAssumption> oldAssumptions = assumptionsForAdvocate.get(score);
                ArrayList<PeptideAssumption> newAssumptions = new ArrayList<>(oldAssumptions.size());

                nPeptideAssumptionsTotal += oldAssumptions.size();

                for (PeptideAssumption peptideAssumption : oldAssumptions) {

                    Peptide peptide = peptideAssumption.getPeptide();
                    String peptideSequence = peptide.getSequence();

                    // kick out peptides that are too long or too short
                    if (peptideSequence.length() >= peptideAssumptionFilter.getMinPepLength() && peptideSequence.length() <= peptideAssumptionFilter.getMaxPepLength()) {

                        // map the algorithm specific modifications on utilities modifications
                        // If there are not enough sites to put them all on the sequence, add an unknown modification
                        // Note: this needs to be done for tag based assumptions as well since the protein mapping can return erroneous modifications for some pattern based modifications
                        ModificationParameters modificationProfile = searchParameters.getModificationParameters();

                        ModificationMatch[] modificationMatches = peptide.getVariableModifications();

                        HashMap<Integer, ArrayList<String>> expectedNames = new HashMap<>(modificationMatches.length);
                        HashMap<ModificationMatch, ArrayList<String>> modNames = new HashMap<>(modificationMatches.length);

                        for (ModificationMatch modMatch : modificationMatches) {

                            HashMap<Integer, HashSet<String>> tempNames = new HashMap<>(modificationMatches.length);

                            String seMod = modMatch.getModification();

//                            if (fileReader instanceof OMSSAIdfileReader) {
//
//                                OmssaParameters omssaParameters = (OmssaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex());
//
//                                if (!omssaParameters.hasModificationIndexes()) {
//
//                                    throw new IllegalArgumentException("OMSSA modification indexes not set in the search parameters.");
//
//                                }
//
//                                int omssaIndex;
//
//                                try {
//
//                                    omssaIndex = Integer.parseInt(seMod);
//
//                                } catch (Exception e) {
//
//                                    throw new IllegalArgumentException("Impossible to parse OMSSA modification index " + seMod + ".");
//
//                                }
//
//                                String omssaName = omssaParameters.getModificationName(omssaIndex);
//
//                                if (omssaName == null) {
//
//                                    if (!ignoredModifications.contains(omssaIndex)) {
//
//                                        waitingHandler.appendReport("Impossible to find OMSSA modification of index "
//                                                + omssaIndex + ". The corresponding peptides will be ignored.", true, true);
//
//                                        ignoredModifications.add(omssaIndex);
//
//                                    }
//
//                                }
//
//                                Modification modification = modificationFactory.getModification(omssaName);
//
//                                tempNames = ModificationUtils.getExpectedModifications(modification.getMass(), modificationProfile, peptide, MOD_MASS_TOLERANCE, sequenceProvider, modificationSequenceMatchingPreferences);
//
//                            } else 
                                
                              if (fileReader instanceof AndromedaIdfileReader) {

                                    AndromedaParameters andromedaParameters = (AndromedaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex());

                                    if (!andromedaParameters.hasModificationIndexes()) {

                                        throw new IllegalArgumentException("Andromeda modification indexes not set in the search parameters.");

                                    }

                                    int andromedaIndex;

                                    try {

                                        andromedaIndex = Integer.parseInt(seMod);

                                    } catch (Exception e) {

                                        throw new IllegalArgumentException("Impossible to parse Andromeda modification index " + seMod + ".");

                                    }

                                    String andromedaName = andromedaParameters.getModificationName(andromedaIndex);

                                    if (andromedaName == null) {

                                        if (!ignoredModifications.contains(andromedaIndex)) {

                                            waitingHandler.appendReport("Impossible to find Andromeda modification of index "
                                                    + andromedaIndex + ". The corresponding peptides will be ignored.", true, true);

                                            ignoredModifications.add(andromedaIndex);

                                        }
                                    }

                                    Modification modification = modificationFactory.getModification(andromedaName);
                                    tempNames = ModificationUtils.getExpectedModifications(modification.getMass(), modificationProfile, peptide, MOD_MASS_TOLERANCE, sequenceProvider, modificationSequenceMatchingPreferences);

                                } else if (fileReader instanceof MascotIdfileReader
                                        || fileReader instanceof XTandemIdfileReader
                                        || fileReader instanceof MsAmandaIdfileReader
                                        || fileReader instanceof MzIdentMLIdfileReader
                                        || fileReader instanceof PepxmlIdfileReader
                                        || fileReader instanceof TideIdfileReader) {

                                    String[] parsedName = seMod.split("@");
                                    Double modMass;

                                    try {

                                        modMass = new Double(parsedName[0]);

                                    } catch (Exception e) {

                                        throw new IllegalArgumentException("Impossible to parse \'" + seMod + "\' as a tagged modification.\n"
                                                + "Error encountered in peptide " + peptideSequence + " spectrum " + spectrumTitle + " in spectrum file "
                                                + spectrumFileName + ".\n" + "Identification file: " + idFile.getName());

                                    }

                                    tempNames = ModificationUtils.getExpectedModifications(modMass, modificationProfile, peptide, MOD_MASS_TOLERANCE, sequenceProvider, modificationSequenceMatchingPreferences);

                                } else if (fileReader instanceof DirecTagIdfileReader
                                        || fileReader instanceof NovorIdfileReader
                                        || fileReader instanceof OnyaseIdfileReader) {

                                    Modification modification = modificationFactory.getModification(seMod);

                                    if (modification == null) {

                                        throw new IllegalArgumentException("Modification not recognized spectrum " + spectrumTitle + " of file " + spectrumFileName + ".");

                                    }

                                    tempNames = ModificationUtils.getExpectedModifications(modification.getMass(), modificationProfile, peptide, MOD_MASS_TOLERANCE, sequenceProvider, modificationSequenceMatchingPreferences);

                                } else {

                                    throw new IllegalArgumentException("Modification mapping not implemented for the parsing of " + idFile.getName() + ".");

                                }

                                HashSet<String> allNames = tempNames.values().stream()
                                        .flatMap(nameList -> nameList.stream())
                                        .collect(Collectors.toCollection(HashSet::new));

                                modNames.put(modMatch, new ArrayList<>(allNames));

                                for (int pos : tempNames.keySet()) {

                                    ArrayList<String> namesAtPosition = expectedNames.get(pos);

                                    if (namesAtPosition == null) {

                                        namesAtPosition = new ArrayList<>(2);
                                        expectedNames.put(pos, namesAtPosition);

                                    }

                                    for (String modName : tempNames.get(pos)) {

                                        if (!namesAtPosition.contains(modName)) {

                                            namesAtPosition.add(modName);

                                        }
                                    }
                                }
                        }

                        if (peptide.getVariableModifications().length > 0) {

                            initialModificationMapping(peptide, expectedNames, modNames, searchParameters);

                        }
                        
                        newAssumptions.add(peptideAssumption);

                    } else {

                        peptideIssue++;

                    }
                }

                if (!newAssumptions.isEmpty()) {

                    assumptionsForAdvocate.put(score, newAssumptions);

                } else {

                    assumptionsForAdvocate.remove(score);

                }
            }

            // try to find the best peptide hit passing the initial filters
            PeptideAssumption firstPeptideHit = null;
            PeptideAssumption firstPeptideHitNoProtein = null;
            TagAssumption firstTagHit = null;

            if (!assumptionsForAdvocate.isEmpty()) {

                ArrayList<Double> advocateScores = new ArrayList<>(assumptionsForAdvocate.keySet());
                Collections.sort(advocateScores);

                for (double score : advocateScores) {

                    ArrayList<PeptideAssumption> firstHits = new ArrayList<>(1);
                    ArrayList<PeptideAssumption> firstHitsNoProteins = new ArrayList<>(1);

                    for (PeptideAssumption peptideAssumption : assumptionsForAdvocate.get(score)) {

                        Peptide peptide = peptideAssumption.getPeptide();
                        boolean filterPassed = true;

                        if (!peptideAssumptionFilter.validatePeptide(peptide, sequenceMatchingPreferences, searchParameters.getDigestionParameters())) {

                            filterPassed = false;
                            peptideIssue++;

                        } else if (!peptideAssumptionFilter.validateModifications(peptide, sequenceMatchingPreferences, modificationSequenceMatchingPreferences, searchParameters.getModificationParameters())) {

                            filterPassed = false;
                            modificationIssue++;

                        } else if (!peptideAssumptionFilter.validatePrecursor(peptideAssumption, spectrumKey, spectrumFactory, searchParameters)) {

                            filterPassed = false;
                            precursorIssue++;

                        } else if (!peptideAssumptionFilter.validateProteins(peptide, sequenceMatchingPreferences, sequenceProvider)) {

                            filterPassed = false;
                            proteinIssue++;

                        } else {

                            if (peptide.getProteinMapping().isEmpty()) {

                                missingProteins++;
                                filterPassed = false;

                                if (firstPeptideHitNoProtein != null) {

                                    firstHitsNoProteins.add(peptideAssumption);

                                }
                            }
                        }

                        if (filterPassed) {

                            firstHits.add(peptideAssumption);

                        }
                    }

                    if (!firstHits.isEmpty()) {

                        firstPeptideHit = BestMatchSelection.getBestHit(spectrumKey, firstHits, proteinCount, sequenceProvider, identificationParameters, peptideSpectrumAnnotator);

                    }
                    if (firstPeptideHit != null) {

                        inputMap.addEntry(advocateId, spectrumFileName, firstPeptideHit.getScore(), PeptideUtils.isDecoy(firstPeptideHit.getPeptide(), sequenceProvider));
                        nRetained++;
                        break;

                    } else if (!firstHitsNoProteins.isEmpty()) {

                        // See if a peptide without protein can be a best match
                        firstPeptideHitNoProtein = BestMatchSelection.getBestHit(spectrumKey, firstHits, proteinCount, sequenceProvider, identificationParameters, peptideSpectrumAnnotator);

                    }
                }

                if (firstPeptideHit != null) {

                    checkPeptidesMassErrorsAndCharges(spectrumKey, firstPeptideHit);

                }
                if (firstPeptideHit == null) {

                    // Check if a peptide with no protein can be a good candidate
                    if (firstPeptideHitNoProtein != null) {

                        checkPeptidesMassErrorsAndCharges(spectrumKey, firstPeptideHitNoProtein);

                    } else {

                        // Try to find the best tag hit
                        TreeMap<Double, ArrayList<TagAssumption>> tagsForAdvocate = spectrumMatch.getAllTagAssumptions(advocateId);

                        if (tagsForAdvocate != null) {

                            firstTagHit = tagsForAdvocate.keySet().stream()
                                    .sorted()
                                    .flatMap(score -> tagsForAdvocate.get(score).stream())
                                    .findFirst()
                                    .get();
                            checkTagMassErrorsAndCharge(spectrumKey, firstTagHit);

                        }
                    }
                }

                if (firstPeptideHit == null && firstPeptideHitNoProtein == null && firstTagHit == null) {

                    psmsRejected++;

                }
            }
        }
    }

    /**
     * Makes an initial modification mapping based on the search engine results
     * and the compatibility to the searched modifications.
     *
     * @param peptide the peptide
     * @param expectedNames the expected modifications
     * @param modNames the modification names possible for every modification
     * match
     * @param searchParameters the search parameters
     */
    private void initialModificationMapping(Peptide peptide, HashMap<Integer, ArrayList<String>> expectedNames, HashMap<ModificationMatch, ArrayList<String>> modNames, SearchParameters searchParameters) {
        ModificationParameters modificationProfile = searchParameters.getModificationParameters();
        int peptideLength = peptide.getSequence().length();

        // If a terminal modification cannot be elsewhere lock the terminus
        ModificationMatch nTermModification = null;
        ModificationMatch[] modificationMatches = peptide.getVariableModifications();

        for (ModificationMatch modMatch : modificationMatches) {

                double refMass = getRefMass(modMatch.getModification(), searchParameters);
                int modSite = modMatch.getSite();

                if (modSite == 1) {

                    ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);

                    if (expectedNamesAtSite != null) {

                        ArrayList<String> filteredNamesAtSite = new ArrayList<>(expectedNamesAtSite.size());

                        for (String modName : expectedNamesAtSite) {

                            Modification modification = modificationFactory.getModification(modName);

                            if (Math.abs(modification.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)) {

                                filteredNamesAtSite.add(modName);

                            }
                        }

                        for (String modName : filteredNamesAtSite) {

                            Modification modification = modificationFactory.getModification(modName);

                            if (modification.getModificationType().isNTerm()) {

                                boolean otherPossibleMod = false;

                                for (String tempName : modificationProfile.getAllNotFixedModifications()) {

                                    if (!tempName.equals(modName)) {

                                        Modification tempModification = modificationFactory.getModification(tempName);

                                        if (tempModification.getMass() == modification.getMass() && !tempModification.getModificationType().isNTerm()) {

                                            otherPossibleMod = true;
                                            break;

                                        }
                                    }
                                }

                                if (!otherPossibleMod) {

                                    nTermModification = modMatch;
                                    modMatch.setModification(modName);
                                    break;

                                }
                            }
                        }

                        if (nTermModification != null) {

                            break;

                        }
                    }
                }
        }

        ModificationMatch cTermModification = null;

        for (ModificationMatch modMatch : peptide.getVariableModifications()) {

            if (modMatch != nTermModification) {

                double refMass = getRefMass(modMatch.getModification(), searchParameters);
                int modSite = modMatch.getSite();

                if (modSite == peptideLength) {

                    ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);

                    if (expectedNamesAtSite != null) {

                        ArrayList<String> filteredNamesAtSite = new ArrayList<>(expectedNamesAtSite.size());

                        for (String modName : expectedNamesAtSite) {

                            Modification modification = modificationFactory.getModification(modName);

                            if (Math.abs(modification.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)) {

                                filteredNamesAtSite.add(modName);

                            }
                        }

                        for (String modName : filteredNamesAtSite) {

                            Modification modification = modificationFactory.getModification(modName);

                            if (modification.getModificationType().isCTerm()) {

                                boolean otherPossibleMod = false;

                                for (String tempName : modificationProfile.getAllNotFixedModifications()) {

                                    if (!tempName.equals(modName)) {

                                        Modification tempModification = modificationFactory.getModification(tempName);

                                        if (tempModification.getMass() == modification.getMass() && !tempModification.getModificationType().isCTerm()) {

                                            otherPossibleMod = true;
                                            break;

                                        }
                                    }
                                }

                                if (!otherPossibleMod) {

                                    cTermModification = modMatch;
                                    modMatch.setModification(modName);
                                    break;

                                }
                            }
                        }

                        if (cTermModification != null) {

                            break;

                        }
                    }
                }
            }
        }

        // Map the modifications according to search engine localization
        HashMap<Integer, ArrayList<String>> siteToModMap = new HashMap<>(modificationMatches.length); // Site to ptm name including termini
        HashMap<Integer, ModificationMatch> siteToMatchMap = new HashMap<>(modificationMatches.length); // Site to Modification match excluding termini
        HashMap<ModificationMatch, Integer> matchToSiteMap = new HashMap<>(modificationMatches.length); // Modification match to site excluding termini
        boolean allMapped = true;

        for (ModificationMatch modMatch : modificationMatches) {

            boolean mapped = false;

            if (modMatch != nTermModification && modMatch != cTermModification) {

                double refMass = getRefMass(modMatch.getModification(), searchParameters);
                int modSite = modMatch.getSite();
                boolean terminal = false;
                ArrayList<String> expectedNamesAtSite = expectedNames.get(modSite);

                if (expectedNamesAtSite != null) {

                    ArrayList<String> filteredNamesAtSite = new ArrayList<>(expectedNamesAtSite.size());
                    ArrayList<String> modificationAtSite = siteToModMap.get(modSite);

                    for (String modName : expectedNamesAtSite) {

                        Modification modification = modificationFactory.getModification(modName);

                        if (Math.abs(modification.getMass() - refMass) < searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                && (modificationAtSite == null || !modificationAtSite.contains(modName))) {

                            filteredNamesAtSite.add(modName);

                        }
                    }

                    if (filteredNamesAtSite.size() == 1) {

                        String modName = filteredNamesAtSite.get(0);
                        Modification modification = modificationFactory.getModification(modName);
                        ModificationType modificationType = modification.getModificationType();

                        if (modificationType.isNTerm() && nTermModification == null) {

                            nTermModification = modMatch;
                            mapped = true;

                        } else if (modificationType.isCTerm() && cTermModification == null) {

                            cTermModification = modMatch;
                            mapped = true;

                        } else if (!modificationType.isNTerm() && !modificationType.isCTerm()) {

                            matchToSiteMap.put(modMatch, modSite);
                            siteToMatchMap.put(modSite, modMatch);
                            mapped = true;

                        }

                        if (mapped) {

                            modMatch.setModification(modName);

                            if (modificationAtSite == null) {

                                modificationAtSite = new ArrayList<>(1);
                                siteToModMap.put(modSite, modificationAtSite);

                            }

                            modificationAtSite.add(modName);

                        }
                    }

                    if (!mapped) {

                        if (filteredNamesAtSite.isEmpty()) {

                            filteredNamesAtSite = expectedNamesAtSite;

                        }

                        if (modSite == 1) {

                            Double minDiff = null;
                            String bestPtmName = null;

                            for (String modName : filteredNamesAtSite) {

                                Modification modification = modificationFactory.getModification(modName);

                                if (modification.getModificationType().isNTerm() && nTermModification == null) {

                                    double massError = Math.abs(refMass - modification.getMass());

                                    if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                            && (minDiff == null || massError < minDiff)) {

                                        bestPtmName = modName;
                                        minDiff = massError;

                                    }
                                }
                            }

                            if (bestPtmName != null) {

                                nTermModification = modMatch;
                                modMatch.setModification(bestPtmName);
                                terminal = true;

                                if (modificationAtSite == null) {

                                    modificationAtSite = new ArrayList<>(1);
                                    siteToModMap.put(modSite, modificationAtSite);

                                }

                                modificationAtSite.add(bestPtmName);
                                mapped = true;

                            }

                        } else if (modSite == peptideLength) {

                            Double minDiff = null;
                            String bestModName = null;

                            for (String modName : filteredNamesAtSite) {

                                Modification modification = modificationFactory.getModification(modName);

                                if (modification.getModificationType().isCTerm() && cTermModification == null) {

                                    double massError = Math.abs(refMass - modification.getMass());

                                    if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                            && (minDiff == null || massError < minDiff)) {

                                        bestModName = modName;
                                        minDiff = massError;

                                    }
                                }
                            }

                            if (bestModName != null) {

                                cTermModification = modMatch;
                                modMatch.setModification(bestModName);
                                terminal = true;

                                if (modificationAtSite == null) {

                                    modificationAtSite = new ArrayList<>(1);
                                    siteToModMap.put(modSite, modificationAtSite);

                                }

                                modificationAtSite.add(bestModName);
                                mapped = true;

                            }
                        }

                        if (!terminal) {

                            Double minDiff = null;
                            String bestModName = null;

                            for (String modName : filteredNamesAtSite) {

                                Modification modification = modificationFactory.getModification(modName);
                                ModificationType modificationType = modification.getModificationType();

                                if (!modificationType.isCTerm() && !modificationType.isNTerm() && modNames.get(modMatch).contains(modName) && !siteToMatchMap.containsKey(modSite)) {

                                    double massError = Math.abs(refMass - modification.getMass());

                                    if (massError <= searchParameters.getFragmentIonAccuracyInDaltons(amountPerAminoAcidResidue * peptideLength)
                                            && (minDiff == null || massError < minDiff)) {

                                        bestModName = modName;
                                        minDiff = massError;

                                    }
                                }
                            }

                            if (bestModName != null) {

                                modMatch.setModification(bestModName);

                                if (modificationAtSite == null) {

                                    modificationAtSite = new ArrayList<>(1);
                                    siteToModMap.put(modSite, modificationAtSite);

                                }

                                modificationAtSite.add(bestModName);
                                matchToSiteMap.put(modMatch, modSite);
                                siteToMatchMap.put(modSite, modMatch);
                                mapped = true;

                            }
                        }
                    }
                }
            }

            if (!mapped) {

                allMapped = false;

            }
        }

        if (!allMapped) {

            // Try to correct incompatible localizations
            HashMap<Integer, ArrayList<Integer>> remap = new HashMap<>(0);

            for (ModificationMatch modMatch : peptide.getVariableModifications()) {

                if (modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch)) {

                    int modSite = modMatch.getSite();

                    for (int candidateSite : expectedNames.keySet()) {

                        if (!siteToMatchMap.containsKey(candidateSite)) {

                            for (String modName : expectedNames.get(candidateSite)) {

                                if (modNames.get(modMatch).contains(modName)) {

                                    Modification modification = modificationFactory.getModification(modName);
                                    ModificationType modificationType = modification.getModificationType();

                                    if ((!modificationType.isCTerm() || cTermModification == null)
                                            && (!modificationType.isNTerm() || nTermModification == null)) {

                                        ArrayList<Integer> modSites = remap.get(modSite);

                                        if (modSites == null) {

                                            modSites = new ArrayList<>(2);
                                            remap.put(modSite, modSites);

                                        }

                                        if (!modSites.contains(candidateSite)) {

                                            modSites.add(candidateSite);

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HashMap<Integer, Integer> correctedIndexes = ModificationSiteMapping.alignAll(remap);

            for (ModificationMatch modMatch : peptide.getVariableModifications()) {

                if (modMatch != nTermModification && modMatch != cTermModification && !matchToSiteMap.containsKey(modMatch)) {

                    Integer modSite = correctedIndexes.get(modMatch.getSite());

                    if (modSite != null) {

                        if (expectedNames.containsKey(modSite)) {

                            for (String modName : expectedNames.get(modSite)) {

                                if (modNames.get(modMatch).contains(modName)) {

                                    ArrayList<String> taken = siteToModMap.get(modSite);

                                    if (taken == null || !taken.contains(modName)) {

                                        matchToSiteMap.put(modMatch, modSite);
                                        modMatch.setModification(modName);
                                        modMatch.setSite(modSite);

                                        if (taken == null) {

                                            taken = new ArrayList<>(1);
                                            siteToModMap.put(modSite, taken);

                                        }

                                        taken.add(modName);
                                        break;

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves the peptide maximal mass error and found charge.
     *
     * @param spectrumKey the key of the spectrum match
     * @param peptideAssumption the peptide assumption
     */
    private synchronized void checkPeptidesMassErrorsAndCharges(String spectrumKey, PeptideAssumption peptideAssumption) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);

        maxTagErrorPpm = Math.max(maxTagErrorPpm,
                Math.abs(peptideAssumption.getDeltaMass(precursorMz, true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection())));

        maxTagErrorDa = Math.max(maxTagErrorDa,
                Math.abs(peptideAssumption.getDeltaMass(precursorMz, false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection())));

        charges.add(peptideAssumption.getIdentificationCharge());

        for (String protein : peptideAssumption.getPeptide().getProteinMapping().navigableKeySet()) {

            Integer count = proteinCount.get(protein);

            if (count != null) {

                proteinCount.put(protein, count + 1);

            } else {

                if (singleProteinList.contains(protein)) {

                    singleProteinList.remove(protein);
                    proteinCount.put(protein, 2);

                } else {

                    singleProteinList.add(protein);

                }
            }
        }
    }

    /**
     * Saves the maximal precursor error and charge.
     *
     * @param spectrumKey the key of the spectrum match
     * @param tagAssumption the tag assumption
     */
    private synchronized void checkTagMassErrorsAndCharge(String spectrumKey, TagAssumption tagAssumption) {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        double precursorMz = spectrumFactory.getPrecursorMz(spectrumKey);

        maxTagErrorPpm = Math.max(maxTagErrorPpm,
                Math.abs(tagAssumption.getDeltaMass(precursorMz, true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection())));

        maxTagErrorDa = Math.max(maxTagErrorDa,
                Math.abs(tagAssumption.getDeltaMass(precursorMz, false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection())));

        charges.add(tagAssumption.getIdentificationCharge());

    }

    /**
     * Verifies that the modifications targeted by the quick acetyl and quick
     * pyrolidone are included in the search parameters.
     */
    private synchronized void verifyXTandemModifications() {

        if (!xTandemModsCheck) {

            SearchParameters searchParameters = identificationParameters.getSearchParameters();
            ModificationParameters modificationProfile = searchParameters.getModificationParameters();
            IdentificationAlgorithmParameter algorithmParameter = searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex());

            if (algorithmParameter != null) {

                XtandemParameters xtandemParameters = (XtandemParameters) algorithmParameter;

                if (xtandemParameters.isProteinQuickAcetyl() && !modificationProfile.contains("Acetylation of protein N-term")) {

                    Modification modification = ModificationFactory.getInstance().getModification("Acetylation of protein N-term");

                    if (!modificationProfile.getRefinementVariableModifications().contains(modification.getName())) {

                        modificationProfile.addRefinementVariableModification(modification);

                    }
                }

                String[] pyroMods = {"Pyrolidone from E", "Pyrolidone from Q", "Pyrolidone from carbamidomethylated C"};

                if (xtandemParameters.isQuickPyrolidone()) {

                    for (String modName : pyroMods) {

                        if (!modificationProfile.getRefinementVariableModifications().contains(modName)) {

                            Modification modification = ModificationFactory.getInstance().getModification(modName);
                            modificationProfile.addRefinementVariableModification(modification);

                        }
                    }
                }
            }

            xTandemModsCheck = true;

        }
    }

    /**
     * Returns the mass indicated by the identification algorithm for the given
     * modification. 0 if not found.
     *
     * @param seModName the name according to the identification algorithm
     * @param searchParameters the search parameters
     *
     * @return the mass of the modification
     */
    private double getRefMass(String seModName, SearchParameters searchParameters) {

        // Try utilities modifications
        Modification modification = modificationFactory.getModification(seModName);

        if (modification == null) {

            // Try mass@AA
            int atIndex = seModName.indexOf("@");
            if (atIndex > 0) {

                return Double.parseDouble(seModName.substring(0, atIndex));

            } else {

                // Try OMSSA indexes
                try {

                    int omssaIndex = Integer.parseInt(seModName);
                    OmssaParameters omssaParameters = (OmssaParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex());
                    String omssaName = omssaParameters.getModificationName(omssaIndex);

                    if (omssaName != null) {

                        modification = modificationFactory.getModification(omssaName);

                        if (modification != null) {

                            return modification.getMass();

                        }
                    }

                } catch (Exception e) {
                    // could not be parsed as OMSSA modification
                }
            }

        } else {

            return modification.getMass();

        }

        throw new IllegalArgumentException("Modification mass could not be parsed from modification name " + seModName + ".");

    }

    /**
     * Returns the number of PSMs processed.
     *
     * @return the number of PSMs processed
     */
    public long getnPSMs() {

        return nPSMs;

    }

    /**
     * Returns the total number of peptide assumptions parsed.
     *
     * @return the total number of peptide assumptions parsed
     */
    public long getnPeptideAssumptionsTotal() {
        return nPeptideAssumptionsTotal;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters.
     *
     * @return the number of PSMs which did not pass the import filters
     */
    public int getPsmsRejected() {
        return psmsRejected;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * protein issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * protein issue
     */
    public int getProteinIssue() {
        return proteinIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * peptide issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * peptide issue
     */
    public int getPeptideIssue() {
        return peptideIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * precursor issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * precursor issue
     */
    public int getPrecursorIssue() {
        return precursorIssue;
    }

    /**
     * Returns the number of PSMs which did not pass the import filters due to a
     * modification parsing issue.
     *
     * @return the number of PSMs which did not pass the import filters due to a
     * modification parsing issue
     */
    public int getModificationIssue() {
        return modificationIssue;
    }

    /**
     * Returns the number of PSMs where a protein was missing.
     *
     * @return the number of PSMs where a protein was missing
     */
    public int getMissingProteins() {
        return missingProteins;
    }

    /**
     * Returns the number of PSMs retained after filtering.
     *
     * @return the number of PSMs retained after filtering
     */
    public int getnRetained() {
        return nRetained;
    }

    /**
     * Returns the different charges found.
     *
     * @return the different charges found
     */
    public HashSet<Integer> getCharges() {
        return charges;
    }

    /**
     * Returns the maximal peptide mass error found in ppm.
     *
     * @return the maximal peptide mass error found in ppm
     */
    public double getMaxPeptideErrorPpm() {
        return maxPeptideErrorPpm;
    }

    /**
     * Returns the maximal peptide mass error found in Da.
     *
     * @return the maximal peptide mass error found in Da
     */
    public double getMaxPeptideErrorDa() {
        return maxPeptideErrorDa;
    }

    /**
     * Returns the maximal tag mass error found in ppm.
     *
     * @return the maximal tag mass error found in ppm
     */
    public double getMaxTagErrorPpm() {
        return maxTagErrorPpm;
    }

    /**
     * Returns the maximal tag mass error found in Da.
     *
     * @return the maximal tag mass error found in Da
     */
    public double getMaxTagErrorDa() {
        return maxTagErrorDa;

    }
}
