package edu.stanford.nlp.semparse.open.core;

import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.core.eval.CandidateStatistics;
import edu.stanford.nlp.semparse.open.core.eval.EvaluationCase;
import edu.stanford.nlp.semparse.open.core.eval.EvaluationSuccess;
import edu.stanford.nlp.semparse.open.core.eval.Evaluator;
import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.dataset.ExampleCached;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.model.Learner;
import edu.stanford.nlp.semparse.open.model.LearnerBaseline;
import edu.stanford.nlp.semparse.open.model.LearnerMaxEnt;
import edu.stanford.nlp.semparse.open.model.LearnerMaxEntWithBeamSearch;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGenerator;
import edu.stanford.nlp.semparse.open.model.feature.FeaturePostProcessor;
import edu.stanford.nlp.semparse.open.model.feature.FeatureType;
import edu.stanford.nlp.semparse.open.model.tree.KnowledgeTreeBuilder;
import edu.stanford.nlp.semparse.open.util.Parallelizer;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

public class OpenSemanticParser {
  public static class Options {
    @Option(gloss = "Whether to test on training and test data every training iteration")
    public boolean testEveryIteration = true;
    @Option(gloss = "Log parameters after training")
    public boolean logParams = true;
    /**
     * 0 = no log whatsoever
     * 1 = log only tree patterns and scores
     * 2 = add predicted entities and answer keys
     * 3 = add feature vectors for all examples
     */
    @Option(gloss = "Verbosity of the log")
    public int logVerbosity = 3;
    @Option(gloss = "Don't print feature weights for correct answers")
    public boolean ignoreCorrectAnswers = true;
    @Option(gloss = "Learner name (maxent / base / beam)")
    public String learner = "maxent";
    @Option(gloss = "Whether to cheat and use only the candidates that contain the seed answer")
    public boolean useSeed = false;
  }
  public static Options opts = new Options();
  
  private Learner learner;
  private IterativeTester iterativeTester;
  private KnowledgeTreeBuilder knowledgeTreeBuilder = new KnowledgeTreeBuilder();
  private CandidateGenerator candidateGenerator = new CandidateGenerator();
  private static boolean initialized = false;
  
  public static void init() {
    if (initialized) return;
    // Load JavaNLP and other linguistics stuff
    LingData.initModels();
    // Try to load cache
    LingData.loadCache();
    // Check the feature options
    FeatureType.checkFeatureTypeOptionsSanity();
    FeaturePostProcessor.checkFeaturePostProcessorOptionsSanity();
    initialized = true;
  }
  
  public static void cleanUp() {
    // Try to save cache
    LingData.saveCache();
  }
  
  private Learner getLearner() {
    switch (opts.learner) {
      case "maxent":
        //LogInfo.log("Using MaxEnt learner ...");
        return new LearnerMaxEnt();
      case "base":
      case "baseline":
        //LogInfo.log("Using baseline learner ...");
        return new LearnerBaseline();
      case "beam":
      case "beamsearch":
        //LogInfo.logs("Using MaxEnt learner with beam search (beam size = %d) ...",
        //    LearnerMaxEntWithBeamSearch.opts.beamSize);
        return new LearnerMaxEntWithBeamSearch();
    }
    LogInfo.fails("Unknown learner: %s", opts.learner);
    return null;
  }
  
  // ============================================================
  // Preprocessing
  // ============================================================
  
  /**
   * Extract the data (trees / candidates / features) in advance and cache them.
   * It is not necessary to call this function before calling train() or test(), though.
   */
  public void preTrain(Dataset dataset) {
    LogInfo.begin_track("preTrain: %d train, %d test", dataset.trainExamples.size(), dataset.testExamples.size());
    // Process examples in the dataset
    extractData(dataset.trainExamples);
    extractData(dataset.testExamples);
    LogInfo.end_track();
  }
  
  private void extractData(List<Example> examples) {
    List<Example> toExtract = Lists.newArrayList();
    for (Example ex : examples)
      if (ex.tree == null || ex.candidates == null)
        toExtract.add(ex);
    for (int i = 0; i < toExtract.size(); i++)
      toExtract.get(i).displayId = i + "/" + toExtract.size();
    if (toExtract.isEmpty()) return;
    
    if (Parallelizer.opts.numThreads == 1) {
      for (Example ex : toExtract) extractData(ex);
    } else {
      try {
        extractParallel(toExtract);
      } catch (InterruptedException e) {
        LogInfo.fail("Data extraction was interrupted!");
      }
    }
  }
  
  private void extractData(Example ex) {
    LogInfo.begin_track("extractData (%s): %s", ex.displayId, ex.phrase);
    Execution.putOutput("currExample", ex.displayId);
    if (ex.tree == null)
      knowledgeTreeBuilder.buildKnowledgeTree(ex);
    if (ex.candidates == null)
      candidateGenerator.process(ex);
    LogInfo.end_track();
  }
  
  private void extractParallel(List<Example> examples) throws InterruptedException {
    List<Runnable> tasks = Lists.newArrayList();
    for (final Example ex : examples) {
      tasks.add(new Runnable() {
        @Override public void run() {
          extractData(ex);
        }
      });
    }
    Parallelizer.run(tasks);
  }

  // ============================================================
  // Train
  // ============================================================
  
  public void train(Dataset dataset, boolean beVeryQuiet) {
    // Extract data
    extractData(dataset.trainExamples);
    // Learn a model
    learner = getLearner();
    if (beVeryQuiet) learner.shutUp();
    if (opts.testEveryIteration) {
      iterativeTester = new IterativeTester(this, dataset);
      learner.setIterativeTester(iterativeTester);
      if (beVeryQuiet) iterativeTester.beVeryQuiet = true;
    }
    learner.learn(dataset, null);
    if (opts.logParams && !beVeryQuiet) learner.logParam();
  }
  
  public void train(Dataset dataset) {
    train(dataset, false);
  }
  
  public IterativeTester getIterativeTester() {
    return iterativeTester;
  }
  
  public void load(String path) {
    learner = getLearner();
    learner.loadModel(path);
  }
  
  public void save(String path) {
    learner.saveModel(path);
  }
  
  // ============================================================
  // Predict + Test + Evaluate
  // ============================================================
  
  public CandidateStatistics predict(String phrase) {
    return predict(new Example(phrase));
  }
  
  public CandidateStatistics predict(String phrase, String url) {
    return predict(new ExampleCached(phrase, url));
  }
  
  public CandidateStatistics predict(Example ex) {
    extractData(ex);
    List<Pair<Candidate, Double>> rankedCandidates = learner.getRankedCandidates(ex);
    List<CandidateStatistics> rankedCandidateStats = CandidateStatistics.getRankedCandidateStats(rankedCandidates);
    CandidateStatistics pred = (rankedCandidateStats.isEmpty()) ? null : rankedCandidateStats.get(0);
    return pred;
  }
  
  public List<Pair<Candidate, Double>> getRankedCandidates(Example ex) {
    extractData(ex);
    return learner.getRankedCandidates(ex);
  }
  
  public Evaluator test(List<Example> examples, String testSuiteName) {
    Evaluator evaluator = new Evaluator(testSuiteName, learner);
    if (examples.isEmpty()) {
      LogInfo.warnings("Cannot test on an empty list %s", testSuiteName);
      return evaluator;
    }
    
    if (opts.logVerbosity > 0)
      LogInfo.begin_track("Testing on %s", testSuiteName);
    
    // Process examples in the dataset
    extractData(examples);
    
    for (Example ex : examples) {
      List<Pair<Candidate, Double>> rankedCandidates = learner.getRankedCandidates(ex);
      if (opts.useSeed) {
        // Only keep the candidates that contain the seed
        List<Pair<Candidate, Double>> filteredRankedCandidates = Lists.newArrayList();
        TargetEntity seed = ex.expectedAnswer.targetEntities.get(1);
        for (Pair<Candidate, Double> pair : rankedCandidates) {
          List<String> predicted = pair.getFirst().predictedEntities;
          if (seed.matchAny(predicted))
            filteredRankedCandidates.add(pair);
        }
        rankedCandidates = filteredRankedCandidates;
      }
      List<CandidateStatistics> rankedCandidateStats = CandidateStatistics.getRankedCandidateStats(rankedCandidates);
      
      CandidateStatistics pred, firstTrue, best;
      pred = (rankedCandidateStats.isEmpty()) ? null : rankedCandidateStats.get(0);
      firstTrue = ex.expectedAnswer.findFirstTrueCandidate(rankedCandidateStats);
      best = ex.expectedAnswer.findBestCandidate(rankedCandidateStats);
      
      // Send to evaluator
      EvaluationCase evaluationCase = evaluator.add(ex, pred, firstTrue, best);
      // Log stuff
      if (opts.logVerbosity > 0) {
        LogInfo.begin_track("Found %d candidates for %s", ex.candidates.size(), ex);
        if (opts.ignoreCorrectAnswers && evaluationCase instanceof EvaluationSuccess) {
          LogInfo.logs("<%s SUCCESS> Rank 1 [Unique Rank 1]: (Total Feature Score = %s)",
              testSuiteName, pred.score);
        } else {
          // Log answer key
          if (opts.logVerbosity >= 2) {
            LogInfo.begin_track("Answer Key (%d entities):", ex.expectedAnswer.size());
            LogInfo.log(ex.expectedAnswer.sampleEntities());
            LogInfo.end_track();
          }
          // Log TRUE and PRED
          evaluationCase.logTrue();
          evaluationCase.logPred();
        }
        LogInfo.end_track();
      }
    }
    
    if (opts.logVerbosity > 0)
      LogInfo.end_track();
    return evaluator;
  }

}
