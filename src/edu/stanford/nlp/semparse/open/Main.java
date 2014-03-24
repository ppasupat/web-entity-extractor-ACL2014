package edu.stanford.nlp.semparse.open;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.core.InteractiveDemo;
import edu.stanford.nlp.semparse.open.core.OpenSemanticParser;
import edu.stanford.nlp.semparse.open.core.ParallelizedTrainer;
import edu.stanford.nlp.semparse.open.core.eval.Evaluator;
import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswer;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerCriteriaMatch;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerInjectiveMatch;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityNearMatch;
import edu.stanford.nlp.semparse.open.dataset.library.JSONDatasetReader;
import edu.stanford.nlp.semparse.open.dataset.library.DatasetLibrary;
import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.FrequencyTable;
import edu.stanford.nlp.semparse.open.ling.LingData;
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
import fig.basic.Option;
import fig.basic.OptionsParser;
import fig.basic.OrderedStringMap;
import fig.exec.Execution;

/**
 * Main class for training and testing models.
 * 
 * <h1>Development Mode</h1>
 * <p><blockquote><code>
 *   java edu.stanford.nlp.semparse.open.Main -dataset FAMILY.NAME [-trainFrac 0.8] [-testFrac 0.2] [-saveModel FILENAME] [-folds 1]
 * </blockquote></code></p>
 * <p>Specify 1 dataset to be divided into train + test based on the specified ratio (trainFrac & testFrac).
 * <p>To use the whole dataset as the training data, use options trainFrac = 1 and testFrac = 0.
 * <p>The model can be saved by specifying the saveModel option.
 * <p>To run on multiple random splits in parallel, use the 'folds' option. (Note: saveModel cannot be used when folds > 1)
 * 
 * <h1>Train & Test Mode</h1>
 * <p><blockquote><code>
 *   java edu.stanford.nlp.semparse.open.Main -dataset FAMILY.TRAIN_NAME@TEST_NAME [-saveModel FILENAME]
 * </blockquote></code></p>
 * <p>Specify 2 datasets from the same family (one for train, one for test) separated by the "@" sign.
 * <p>The model can be saved by specifying the saveModel option.
 * 
 * <h1>Test Only Mode</h1>
 * <p><blockquote><code>
 *   java edu.stanford.nlp.semparse.open.Main -loadModel FILENAME -dataset FAMILY.NAME
 * </blockquote></code></p>
 * <p>Load model from file + Test on the specified dataset.
 *  
 * <h1>Interactive Mode</h1>
 * <p><blockquote><code>
 *   java edu.stanford.nlp.semparse.open.Main -loadModel FILENAME
 * </blockquote></code></p>
 * <p>Load model from file + Start interactive mode where the desired query and web page can be entered directly. 
 *   
 * <h1>Experiment Mode</h1>
 * <p><blockquote><code>
 *   java edu.stanford.nlp.semparse.open.experiment.Experiments -experiment NAME
 * </blockquote></code></p>
 * See {@link edu.stanford.nlp.semparse.open.experiment.Experiments Experiments}.
 * 
 */
public class Main implements Runnable {
  public static class Options {
    @Option(gloss="dataset name (format = family.name)") public String dataset = null;
    @Option(gloss="filename for saving a trained model") public String saveModel = null;
    @Option(gloss="filename for loading a trained model") public String loadModel = null;
    @Option(gloss="number of folds") public int folds = 1;
  }
  public static Options opts = new Options();
  
  public static OptionsParser getOptionsParser() {
    OptionsParser parser = new OptionsParser();
    parser.registerAll(new Object[] {
        "AbstractJSONDatasetReader", JSONDatasetReader.opts,
        "AdvancedWordVectorParams", AdvancedWordVectorParams.opts,
        "AdvancedWordVectorParamsLowRank", AdvancedWordVectorParamsLowRank.opts,
        "BrownClusterTable", BrownClusterTable.opts,
        "CandidateGenerator", CandidateGenerator.opts,
        "CandidateGroup", CandidateGroup.opts,
        "CoreRunner", Main.opts,
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
        "OpenSemanticParser", OpenSemanticParser.opts,
        "Params", Params.opts,
        "TargetEntityNearMatch", TargetEntityNearMatch.opts,
        "WordNetClusterTable", WordNetClusterTable.opts,
        "WordVectorTable", WordVectorTable.opts,
    });
    return parser;
  }
  
  public static void main(String args[]) {
    Execution.run(args, new Main(), getOptionsParser());
  }
    
  // ============================================================
  // Overall run script
  // ============================================================

  public void run() {
    if (opts.folds > 1 && (opts.saveModel != null || opts.loadModel != null))
      LogInfo.fail("Cannot save or load a model with folds > 1");
    if (opts.loadModel != null) {
      loadAndTestMode();
    } else {
      trainAndTestMode();
    }
  }

  private void loadAndTestMode() {
    OpenSemanticParser parser = loadModel();
    Dataset dataset = DatasetLibrary.getDataset(opts.dataset);
    if (dataset == null) {
      // Interactive demo mode
      new InteractiveDemo(parser).run();
    } else {
      // Test on the specified data set
      Dataset testDataset = new Dataset();
      testDataset.addTestFromDataset(dataset);
      test(parser, testDataset);
    }
    OpenSemanticParser.cleanUp();
  }

  private void trainAndTestMode() {
    // Train + Test (possibly many folds)
    Dataset dataset = DatasetLibrary.getDataset(opts.dataset);
    if (dataset == null)
      LogInfo.fail("Must specify either a dataset to train on or a model to load.");
    Execution.putOutput("numTrainExamples", dataset.trainExamples.size());
    Execution.putOutput("numTestExamples", dataset.testExamples.size());
    OpenSemanticParser.init();
    List<IterativeTester> iterativeTesters = (opts.folds > 1) ? runParallel(dataset) : runSingle(dataset);
    OpenSemanticParser.cleanUp();
    summarize(iterativeTesters);
  }

  // ============================================================
  // Parallelization
  // ============================================================

  private List<IterativeTester> runSingle(Dataset dataset) {
    List<IterativeTester> iterativeTesters = Lists.newArrayList();
    iterativeTesters.add(trainAndTest(dataset).getIterativeTester());
    return iterativeTesters;
  }

  private List<IterativeTester> runParallel(Dataset dataset) {
    // Pretrain the dataset
    new OpenSemanticParser().preTrain(dataset);
    // Parallelize training
    int numThread = Math.min(Runtime.getRuntime().availableProcessors(), opts.folds);
    ExecutorService service = Executors.newFixedThreadPool(numThread);
    List<ParallelizedTrainer> tasks = Lists.newArrayList();
    Dataset shuffled = dataset;
    for (int i = 0; i < opts.folds; i++) {
      tasks.add(new ParallelizedTrainer(shuffled, i != 0));
      shuffled = shuffled.getNewShuffledDataset();
    }
    List<Future<OpenSemanticParser>> parsers;
    List<IterativeTester> iterativeTesters = Lists.newArrayList();
    try {
      // Turn off logging temporarily
      int oldLogVerbosity = OpenSemanticParser.opts.logVerbosity;
      OpenSemanticParser.opts.logVerbosity = 0;
      // Invoke all trainers
      parsers = service.invokeAll(tasks);
      for (int i = 0; i < opts.folds; i++)
        iterativeTesters.add(parsers.get(i).get().getIterativeTester());
      service.shutdown();
      service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      // Print results from the 1st run
      OpenSemanticParser.opts.logVerbosity = oldLogVerbosity;
      test(parsers.get(0).get(), dataset);
    } catch (InterruptedException e) {
      LogInfo.fail(e);
    } catch (ExecutionException e) {
      LogInfo.fail(e);
    }
    return iterativeTesters;
  }

  // ============================================================
  // Train / Test
  // ============================================================

  private OpenSemanticParser train(Dataset dataset) {
    OpenSemanticParser parser = new OpenSemanticParser();
    parser.preTrain(dataset);
    parser.train(dataset);
    if (opts.saveModel != null)
      saveModel(parser);
    return parser;
  }

  private OpenSemanticParser test(OpenSemanticParser parser, Dataset dataset) {
    Evaluator trainEvaluator = parser.test(dataset.trainExamples, "TRANING SET");
    Evaluator testEvaluator = parser.test(dataset.testExamples, "TEST SET");
    LogInfo.begin_track("### Error Analysis ###");
    trainEvaluator.printDetails();
    testEvaluator.printDetails();
    LogInfo.end_track();
    LogInfo.begin_track("### Summary ###");
    trainEvaluator.printScores();
    testEvaluator.printScores();
    trainEvaluator.putOutput("train");
    testEvaluator.putOutput("test");
    LogInfo.end_track();
    return parser;
  }

  private OpenSemanticParser trainAndTest(Dataset dataset) {
    return test(train(dataset), dataset);
  }

  private void summarize(List<IterativeTester> iterativeTesters) {
    for (int i = 0; i < opts.folds; i++)
      iterativeTesters.get(i).summarize();
    if (opts.folds > 1) {
      List<IterativeTester.EvaluatorStatistics> trainStats = Lists.newArrayList(), testStats = Lists.newArrayList();
      for (IterativeTester tester : iterativeTesters) {
        trainStats.add(tester.getLastTrainStat());
        testStats.add(tester.getLastTestStat());
      }
      IterativeTester.EvaluatorStatistics.logAverage(trainStats, "train");
      IterativeTester.EvaluatorStatistics.logAverage(testStats, "test");
    }
  }

  // ============================================================
  // Persistence with model
  // ============================================================

  private OpenSemanticParser loadModel() {
    // Load options
    LogInfo.log("Loading options from " + opts.loadModel);
    getOptionsParser().parseOptionsFile(opts.loadModel);
    // Load parameters
    LogInfo.log("Loading parameters from " + opts.loadModel + ".params");
    OpenSemanticParser.init();
    OpenSemanticParser parser = new OpenSemanticParser();
    parser.load(opts.loadModel + ".params");
    return parser;
  }

  private void saveModel(OpenSemanticParser parser) {
    try {
      // Save options
      LogInfo.log("Saving options to " + opts.saveModel);
      OrderedStringMap allOptions = getOptionsParser().getOptionPairs();
      OrderedStringMap importantOptions = new OrderedStringMap();
      for (String key : allOptions.keys()) {
        if (isImportantOption(key))
          importantOptions.put(key, allOptions.get(key));
      }
      importantOptions.printHard(opts.saveModel);
      // Save parameters
      LogInfo.log("Saving parameters to " + opts.saveModel + ".params");
      parser.save(opts.saveModel + ".params");
    } catch (RuntimeException e) {
      LogInfo.warning(e);
      LogInfo.warnings("Cannot save to %s but will continue anyway.", opts.saveModel);
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
  private boolean isImportantOption(String key) {
    return importantClasses.contains(key.split("\\.")[0]) || importantOptions.contains(key);
  }

}
