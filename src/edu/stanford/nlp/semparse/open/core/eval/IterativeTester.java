package edu.stanford.nlp.semparse.open.core.eval;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.Main;
import edu.stanford.nlp.semparse.open.core.OpenSemanticParser;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.exec.Execution;

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

  public static class EvaluatorStatistics {
    final int numExamples, numSuccess, numNormalFail, numSuperFail, numFound;
    // These values are in [0,100]
    final double oracle, accuracyAll, accuracyFound;
    final double avgF1onExpectedEntities, avgF1onBest;
    final List<Double> accuracyAtK;
    
    public EvaluatorStatistics(Evaluator evaluator) {
      numExamples = evaluator.numExamples;
      numSuccess = evaluator.numSuccess;
      numNormalFail = evaluator.numNormalFail;
      numSuperFail = evaluator.numSuperFail;
      numFound = evaluator.numFound;
      oracle = numFound * 100.0 / numExamples;
      accuracyAll = numSuccess * 100.0 / numExamples;
      accuracyFound = numSuccess * 100.0 / numFound;
      avgF1onExpectedEntities = evaluator.sumF1onExpectedEntities * 100.0 / numExamples;
      avgF1onBest = evaluator.sumF1onBest * 100.0 / numExamples;
      accuracyAtK = evaluator.getAccuracyAtK(MAX_K);
    }
    
    private static double getAverage(List<Double> stuff) {
      return ListUtils.sum(stuff) / stuff.size();
    }
    
    public static void logAverage(List<EvaluatorStatistics> stats, String prefix) {
      int n = stats.size();
      List<Double>       oracleL = Lists.newArrayList(),
                    accuracyAllL = Lists.newArrayList(),
                  accuracyFoundL = Lists.newArrayList(),
        avgF1onExpectedEntitiesL = Lists.newArrayList(),
                    avgF1onBestL = Lists.newArrayList();
      List<List<Double>> accuracyAtKL = Lists.newArrayList();
      for (int i = 0; i < MAX_K + 1; i++) accuracyAtKL.add(new ArrayList<Double>());
      for (EvaluatorStatistics stat : stats) {
        oracleL.add(stat.oracle);
        accuracyAllL.add(stat.accuracyAll);
        accuracyFoundL.add(stat.accuracyFound);
        avgF1onExpectedEntitiesL.add(stat.avgF1onExpectedEntities);
        avgF1onBestL.add(stat.avgF1onBest);
        List<Double> accuracyAtK = stat.accuracyAtK;
        for (int i = 0; i < MAX_K + 1; i++) {
          accuracyAtKL.get(i).add(accuracyAtK.get(i));
        }
      }
      List<Double> accuracyAtK = Lists.newArrayList();
      for (int i = 0; i < MAX_K + 1; i++) {
        accuracyAtK.add(getAverage(accuracyAtKL.get(i)));
      }
      Execution.putOutput(prefix + ".accuracy", getAverage(accuracyAllL) / 100);
      Execution.putOutput(prefix + ".oracle", getAverage(oracleL) / 100);
      Execution.putOutput(prefix + ".averageF1onTargetEntities", getAverage(avgF1onExpectedEntitiesL) / 100);
      Execution.putOutput(prefix + ".averageF1onBestCandidate", getAverage(avgF1onBestL) / 100);
      Execution.putOutput(prefix + ".accuracyAtK", accuracyAtK.subList(1, MAX_K + 1));
      LogInfo.begin_track("@@@@@ SUMMARY %s (%d folds) @@@@@", prefix, n);
      LogInfo.logs("Oracle: %.3f%% %s", getAverage(oracleL), oracleL);
      LogInfo.logs("Accuracy (vs all): %.3f%% %s", getAverage(accuracyAllL), accuracyAllL);
      LogInfo.logs("Accuracy (vs found): %.3f%% %s", getAverage(accuracyFoundL), accuracyFoundL);
      LogInfo.logs("Average F1 (vs target entities): %.3f%% %s", getAverage(avgF1onExpectedEntitiesL), avgF1onExpectedEntitiesL);
      LogInfo.logs("Average F1 (vs best candidate): %.3f%% %s", getAverage(avgF1onBestL), avgF1onBestL);
      LogInfo.logs("Accuracy @ k : %s", accuracyAtK.subList(1, MAX_K + 1));
      LogInfo.end_track();
    }
  }
  
  public EvaluatorStatistics getLastTrainStat() {
    return trainStats.get(trainStats.size() - 1);
  }
  
  public EvaluatorStatistics getLastTestStat() {
    return testStats.get(testStats.size() - 1);
  }
  
  
  
}
