package edu.stanford.nlp.semparse.open.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.Main;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswer;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerCriteriaMatch;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerInjectiveMatch;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityNearMatch;
import edu.stanford.nlp.semparse.open.dataset.library.JSONDatasetReader;
import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.FrequencyTable;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.ling.QueryTypeTable;
import edu.stanford.nlp.semparse.open.ling.WordNetClusterTable;
import edu.stanford.nlp.semparse.open.ling.WordVectorTable;
import edu.stanford.nlp.semparse.open.model.AdvancedWordVectorParams;
import edu.stanford.nlp.semparse.open.model.AdvancedWordVectorParamsLowRank;
import edu.stanford.nlp.semparse.open.model.LearnerBaseline;
import edu.stanford.nlp.semparse.open.model.LearnerMaxEnt;
import edu.stanford.nlp.semparse.open.model.LearnerMaxEntWithBeamSearch;
import edu.stanford.nlp.semparse.open.model.Params;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGenerator;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.feature.FeatureType;
import edu.stanford.nlp.semparse.open.model.feature.FeatureTypeHoleBased;
import edu.stanford.nlp.semparse.open.model.feature.FeatureTypeLinguisticsBased;
import edu.stanford.nlp.semparse.open.model.feature.FeatureTypeNaiveEntityBased;
import edu.stanford.nlp.semparse.open.model.feature.FeatureTypeNodeBased;
import edu.stanford.nlp.semparse.open.model.feature.FeatureTypeQueryBased;
import edu.stanford.nlp.semparse.open.model.tree.KnowledgeTreeBuilder;
import fig.basic.LogInfo;
import fig.basic.OptionsParser;
import fig.basic.OrderedStringMap;

/**
 * Initialize OptionsParser and load / save models.
 * 
 * The model will be stored at [modelDir]/options.map and [modelDir]/params
 * For example, modelDir can be "./state/execs/???.exec/" (fig state)
 */
public class AllOptions {
  
  public static OptionsParser getOptionsParser() {
    OptionsParser parser = new OptionsParser();
    parser.registerAll(new Object[] {
        "AbstractJSONDatasetReader", JSONDatasetReader.opts,
        "AdvancedWordVectorParams", AdvancedWordVectorParams.opts,
        "AdvancedWordVectorParamsLowRank", AdvancedWordVectorParamsLowRank.opts,
        "BrownClusterTable", BrownClusterTable.opts,
        "CandidateGenerator", CandidateGenerator.opts,
        "CandidateGroup", CandidateGroup.opts,
        "ExpectedAnswer", ExpectedAnswer.opts,
        "ExpectedAnswerInjectiveMatch", ExpectedAnswerInjectiveMatch.opts,
        "ExpectedAnswerCriteriaMatch", ExpectedAnswerCriteriaMatch.opts,
        "FeatureType", FeatureType.opts,
        "FeatureTypeHoleBased", FeatureTypeHoleBased.opts,
        "FeatureTypeNaiveEntityBased", FeatureTypeNaiveEntityBased.opts,
        "FeatureTypeLinguisticsBased", FeatureTypeLinguisticsBased.opts,
        "FeatureTypeNodeBased", FeatureTypeNodeBased.opts,
        "FeatureTypeQueryBased", FeatureTypeQueryBased.opts,
        "FrequencyTable", FrequencyTable.opts,
        "KnowledgeTreeBuilder", KnowledgeTreeBuilder.opts,
        "LearnerBaseline", LearnerBaseline.opts,
        "LearnerMaxEnt", LearnerMaxEnt.opts,
        "LearnerMaxEntWithBeamSearch", LearnerMaxEntWithBeamSearch.opts,
        "LingData", LingData.opts,
        "main", Main.opts,
        "OpenSemanticParser", OpenSemanticParser.opts,
        "Params", Params.opts,
        "QueryTypeTable", QueryTypeTable.opts,
        "TargetEntityNearMatch", TargetEntityNearMatch.opts,
        "WordNetClusterTable", WordNetClusterTable.opts,
        "WordVectorTable", WordVectorTable.opts,
    });
    return parser;
  }
  
  // ============================================================
  // Persistence with model
  // ============================================================
  
  public static String getOptionsMapFilename(String modelDir) {
    return new File(modelDir, "options.map").getPath();
  }
  
  public static String getParamsFilename(String modelDir) {
    return new File(modelDir, "params").getPath();
  }
  
  public static OpenSemanticParser loadModel(String modelDir) {
    try {
      // Load options
      String optionsMapFilename = getOptionsMapFilename(modelDir);
      LogInfo.log("Loading options from " + optionsMapFilename);
      String tempFilename = prepareWhitelistedOptionsFile(optionsMapFilename);
      getOptionsParser().parseOptionsFile(tempFilename);
      // Load parameters
      String paramsFilename = getParamsFilename(modelDir);
      LogInfo.log("Loading parameters from " + paramsFilename);
      OpenSemanticParser.init();
      OpenSemanticParser parser = new OpenSemanticParser();
      parser.load(paramsFilename);
      return parser;
    } catch (IOException e) {
      LogInfo.fail(e);
    }
    return null;
  }
  
  private static String prepareWhitelistedOptionsFile(String optionsMapFilename) throws IOException {
    File tempFile = File.createTempFile("options", ".whitelisted.tmp");
    LogInfo.logs("Copying whitelisted options %s > %s", optionsMapFilename, tempFile.getPath());
    tempFile.deleteOnExit();
    try (BufferedReader reader = new BufferedReader(new FileReader(optionsMapFilename));
        PrintWriter writer = new PrintWriter(tempFile)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        String[] tokens = line.split("\\s+", 2);
        if (isImportantOption(tokens[0])) {
          writer.println(line);
        }
      }
    }
    return tempFile.getPath();
  }
  
  public static void saveModel(String modelDir, OpenSemanticParser parser) {
    try {
      if (!new File(modelDir).mkdirs()) {
        throw new RuntimeException("Cannot create directory " + modelDir);
      }
      // Save options
      String optionsMapFilename = getOptionsMapFilename(modelDir);
      LogInfo.log("Saving options to " + optionsMapFilename);
      OrderedStringMap allOptions = getOptionsParser().getOptionPairs();
      OrderedStringMap importantOptions = new OrderedStringMap();
      for (String key : allOptions.keys()) {
        if (isImportantOption(key))
          importantOptions.put(key, allOptions.get(key));
      }
      importantOptions.printHard(optionsMapFilename);
      // Save parameters
      String paramsFilename = getParamsFilename(modelDir);
      LogInfo.log("Saving parameters to " + paramsFilename);
      parser.save(paramsFilename);
    } catch (RuntimeException e) {
      LogInfo.warning(e);
      LogInfo.warnings("Cannot save to %s/{options.map,params}, but will continue anyway.", modelDir);
    }
  }
  
  private static Set<String> importantClasses = Sets.newHashSet(
       "AdvancedWordVectorParams",
       "BrownClusterTable",
       "CandidateGenerator",
       "CandidateGroup",
       "FeatureType",
       "FeatureTypeHoleBased",
       "FeatureTypeNaiveEntityBased",
       "FeatureTypeLinguisticsBased",
       "FeatureTypeNodeBased",
       "FeatureTypeQueryBased",
       "FrequencyTable",
       "KnowledgeTreeBuilder",
       "LearnerBaseline",
       "LearnerMaxEntWithBeamSearch",
       "QueryTypeTable",
       "TargetEntityNearMatch",
       "WordNetClusterTable",
       "WordVectorTable"
       );
  private static Set<String> importantOptions = Sets.newHashSet(
       "AdvancedWordVectorParamsLowRank.vecRank",
       "LingData.annotators",
       "LingData.useAnnotators",
       "LingData.caseSensitive",
       "OpenSemanticParser.learner"
       );

  /**
   * Return true if the option is important for prediction (not for learning parameters).
   */
  public static boolean isImportantOption(String key) {
    return importantClasses.contains(key.split("\\.")[0]) || importantOptions.contains(key);
  }

}
