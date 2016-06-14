package edu.stanford.nlp.semparse.open;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.stanford.nlp.semparse.open.core.AllOptions;
import edu.stanford.nlp.semparse.open.core.InteractiveDemo;
import edu.stanford.nlp.semparse.open.core.OpenSemanticParser;
import edu.stanford.nlp.semparse.open.core.ParallelizedTrainer;
import edu.stanford.nlp.semparse.open.core.eval.Evaluator;
import edu.stanford.nlp.semparse.open.core.eval.EvaluatorStatistics;
import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.library.DatasetLibrary;
import edu.stanford.nlp.semparse.open.util.Parallelizer;
import fig.basic.LogInfo;
import fig.basic.Option;
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
  
  public static void main(String args[]) {
    Execution.run(args, new Main(), AllOptions.getOptionsParser());
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
  
  public void loadAndTestMode() {
    loadAndTestMode(DatasetLibrary.getDataset(opts.dataset));
  }

  public void loadAndTestMode(Dataset dataset) {
    OpenSemanticParser parser = AllOptions.loadModel(opts.loadModel);
    if (dataset == null) {
      // Interactive demo mode
      new InteractiveDemo(parser).run();
    } else {
      // Test on the specified data set
      new OpenSemanticParser().preTrain(dataset);
      testCombined(parser, dataset);
    }
    OpenSemanticParser.cleanUp();
  }
  
  public void trainAndTestMode() {
    trainAndTestMode(DatasetLibrary.getDataset(opts.dataset));
  }

  public void trainAndTestMode(Dataset dataset) {
    // Train + Test (possibly many folds)
    if (dataset == null)
      LogInfo.fail("Must specify either a dataset to train on or a model to load.");
    Execution.putOutput("numTrainExamples", dataset.trainExamples.size());
    Execution.putOutput("numTestExamples", dataset.testExamples.size());
    OpenSemanticParser.init();
    new OpenSemanticParser().preTrain(dataset);
    dataset.cacheRewards();
    List<IterativeTester> iterativeTesters = (opts.folds > 1) ? runParallel(dataset) : runSingle(dataset);
    OpenSemanticParser.cleanUp();
    summarize(iterativeTesters);
  }

  // ============================================================
  // Parallelization
  // ============================================================

  private List<IterativeTester> runSingle(Dataset dataset) {
    List<IterativeTester> iterativeTesters = new ArrayList<>();
    iterativeTesters.add(trainAndTest(dataset).getIterativeTester());
    return iterativeTesters;
  }

  private List<IterativeTester> runParallel(Dataset dataset) {
    // Shuffle dataset
    List<ParallelizedTrainer> tasks = new ArrayList<>();
    Dataset shuffled = dataset;
    for (int i = 0; i < opts.folds; i++) {
      tasks.add(new ParallelizedTrainer(shuffled, i != 0));
      shuffled = shuffled.getNewShuffledDataset();
    }
    // Turn off logging temporarily
    int oldLogVerbosity = OpenSemanticParser.opts.logVerbosity;
    OpenSemanticParser.opts.logVerbosity = 0;
    // Train in parallel
    List<Future<OpenSemanticParser>> parsers = Parallelizer.runAndReturnStuff(tasks);
    OpenSemanticParser.opts.logVerbosity = oldLogVerbosity;
    // Accumulate OpenSemanticParser and test on the first random split
    List<IterativeTester> iterativeTesters = new ArrayList<>();
    try {
      for (int i = 0; i < opts.folds; i++)
        iterativeTesters.add(parsers.get(i).get().getIterativeTester());
      test(parsers.get(0).get(), dataset);
    } catch (ExecutionException | InterruptedException e) {
      LogInfo.fail(e);
    }
    return iterativeTesters;
  }

  // ============================================================
  // Train / Test
  // ============================================================

  private OpenSemanticParser train(Dataset dataset) {
    OpenSemanticParser parser = new OpenSemanticParser();
    parser.train(dataset);
    if (opts.saveModel != null)
      AllOptions.saveModel(opts.saveModel, parser);
    return parser;
  }
  
  /**
   * Test on both training set and test set
   */
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
  
  /**
   * Combine everything into the "test" dataset and test on it
   */
  private OpenSemanticParser testCombined(OpenSemanticParser parser, Dataset dataset) {
    dataset = new Dataset().addTestFromDataset(dataset);
    Evaluator testEvaluator = parser.test(dataset.testExamples, "TEST SET");
    LogInfo.begin_track("### Error Analysis ###");
    testEvaluator.printDetails();
    LogInfo.end_track();
    LogInfo.begin_track("### Summary ###");
    testEvaluator.printScores();
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
      List<EvaluatorStatistics> trainStats = new ArrayList<>(), testStats = new ArrayList<>();
      for (IterativeTester tester : iterativeTesters) {
        trainStats.add(tester.getLastTrainStat());
        testStats.add(tester.getLastTestStat());
      }
      EvaluatorStatistics.logAverage(trainStats, "train");
      EvaluatorStatistics.logAverage(testStats, "test");
    }
  }

}
