package edu.stanford.nlp.semparse.open.core.eval;

import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.Main;
import edu.stanford.nlp.semparse.open.core.OpenSemanticParser;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import fig.basic.LogInfo;

public class IterativeTester {
  private final OpenSemanticParser openSemanticParser;
  private final Dataset dataset;
  public String message = "";
  List<EvaluatorStatistics> trainStats, testStats;
  
  public boolean beVeryQuiet = false;
  public static final int MAX_K = 10;
  
  public IterativeTester(OpenSemanticParser openSemanticParser, Dataset dataset) {
    this.openSemanticParser = openSemanticParser;
    this.dataset = dataset;
    this.trainStats = Lists.newArrayList();
    this.testStats = Lists.newArrayList();
  }
  
  public void run() {
    int oldLogVerbosity = OpenSemanticParser.opts.logVerbosity;
    OpenSemanticParser.opts.logVerbosity = 0;
    trainStats.add(new EvaluatorStatistics(
        openSemanticParser.test(dataset.trainExamples, "[" + message + "] ITERATIVE TEST on TRAINING SET")
        .printScores(beVeryQuiet).putOutput("train")));
    testStats.add(new EvaluatorStatistics(
        openSemanticParser.test(dataset.testExamples, "[" + message + "] ITERATIVE TEST on TEST SET")
        .printScores(beVeryQuiet).putOutput("test")));
    OpenSemanticParser.opts.logVerbosity = oldLogVerbosity;
  }
  
  public void summarize() {
    LogInfo.begin_track("@@@ SUMMARY @@@");
    LogInfo.logs("%7s | %7s %7s %7s | %7s %7s %7s", "iter",
        "tracc", "trora", "traf1", "tsacc", "tsora", "tsaf1");
    for (int i = 0; i < trainStats.size(); i++) {
      EvaluatorStatistics t = trainStats.get(i), s = testStats.get(i);
      double tF1, sF1;
      if ("wiki".equals(Main.opts.dataset.split("[.]")[0])) {
        tF1 = t.avgF1onExpectedEntities;
        sF1 = s.avgF1onExpectedEntities;
      } else {
        tF1 = t.avgF1onBest;
        sF1 = s.avgF1onBest;
      }
      LogInfo.logs("%7s | %7.2f %7.2f %7.2f | %7.2f %7.2f %7.2f", i+1,
          t.accuracyAll, t.oracle, tF1,
          s.accuracyAll, s.oracle, sF1);
    }
    LogInfo.end_track();
  }

  public EvaluatorStatistics getLastTrainStat() {
    return trainStats.get(trainStats.size() - 1);
  }
  
  public EvaluatorStatistics getLastTestStat() {
    return testStats.get(testStats.size() - 1);
  }
  
  
  
}
