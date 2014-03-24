package edu.stanford.nlp.semparse.open.core.eval;

import java.util.List;

import fig.basic.Fmt;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.exec.Execution;

public class EvaluatorStatistics {
  final int numExamples, numSuccess, numNormalFail, numSuperFail, numFound;
  // These values are in [0, 1]
  final double oracle, accuracyAll, accuracyFound;
  final double avgF1onExpectedEntities, avgF1onBest;
  final double[] accuracyAtK;
  
  public EvaluatorStatistics(Evaluator evaluator) {
    numExamples = evaluator.numExamples;
    numSuccess = evaluator.numSuccess;
    numNormalFail = evaluator.numNormalFail;
    numSuperFail = evaluator.numSuperFail;
    numFound = evaluator.numFound;
    oracle = numFound * 1.0 / numExamples;
    accuracyAll = numSuccess * 1.0 / numExamples;
    accuracyFound = numSuccess * 1.0 / numFound;
    avgF1onExpectedEntities = evaluator.sumF1onExpectedEntities * 1.0 / numExamples;
    avgF1onBest = evaluator.sumF1onBest * 1.0 / numExamples;
    accuracyAtK = evaluator.getAccuracyAtK(IterativeTester.MAX_K);
  }
  
  private static double getAverage(double[] stuff) {
    return ListUtils.sum(stuff) / stuff.length;
  }
  
  // Divide by n instead of (n-1)
  private static double getVariance(double[] stuff) {
    double sumSq = 0, sum = 0, n = stuff.length;
    for (double x: stuff) { sumSq += x * x; sum += x; }
    return (sumSq / n) - (sum / n) * (sum / n);
  }
  
  // Divide by n instead of (n-1)
  private static double getSD(double[] stuff) {
    return Math.sqrt(getVariance(stuff));
  }
  
  public static void logAverage(List<EvaluatorStatistics> stats, String prefix) {
    int n = stats.size(), K = IterativeTester.MAX_K;
    double[]           oracleL = new double[n],
                  accuracyAllL = new double[n],
                accuracyFoundL = new double[n],
      avgF1onExpectedEntitiesL = new double[n],
                  avgF1onBestL = new double[n];
    double[][]    accuracyAtKL = new double[K + 1][n];
    
    // Compile each statistic into a list
    for (int i = 0; i < n; i++) {
      EvaluatorStatistics stat = stats.get(i);
      oracleL[i] = stat.oracle;
      accuracyAllL[i] = stat.accuracyAll;
      accuracyFoundL[i] = stat.accuracyFound;
      avgF1onExpectedEntitiesL[i] = stat.avgF1onExpectedEntities;
      avgF1onBestL[i] = stat.avgF1onBest;
      for (int j = 0; j <= K; j++)
        accuracyAtKL[j][i] = stat.accuracyAtK[j];
    }
    double[] accuracyAtK = new double[K + 1], accuracyAtKSD = new double[K + 1];
    for (int j = 0; j <= K; j++) {
      accuracyAtK[j] = getAverage(accuracyAtKL[j]);
      accuracyAtKSD[j] = getSD(accuracyAtKL[j]);
    }
    
    // Log the statistics
    LogInfo.begin_track("@@@@@ SUMMARY %s (%d folds) @@@@@", prefix, n);
    logAverageSingle(oracleL, prefix + ".oracle", "Oracle");
    logAverageSingle(accuracyAllL, prefix + ".accuracy", "Accuracy (vs all)");
    logAverageSingle(accuracyFoundL, prefix + ".accuracyFound", "Accuracy (vs found)");
    logAverageSingle(avgF1onExpectedEntitiesL, prefix + ".averageF1onTargetEntities", "Average F1 (vs target entities)");
    logAverageSingle(avgF1onBestL, prefix + ".averageF1onBestCandidate", "Average F1 (vs best candidate)");
    // Ignore accuracy at 0 (which is always 0)
    Execution.putOutput(prefix + ".accuracyAtK", Fmt.D(ListUtils.subArray(accuracyAtK, 1)));
    Execution.putOutput(prefix + ".accuracyAtKSD", Fmt.D(ListUtils.subArray(accuracyAtKSD, 1)));
    LogInfo.logs("Accuracy @ k : %s", Fmt.D(ListUtils.mult(100.0, ListUtils.subArray(accuracyAtK, 1))));
    LogInfo.logs("            +- %s", Fmt.D(ListUtils.mult(100.0, ListUtils.subArray(accuracyAtKSD, 1))));
    LogInfo.end_track();
  }
  
  private static void logAverageSingle(double[] stuff, String outputName, String logName) {
    Execution.putOutput(outputName, getAverage(stuff));
    Execution.putOutput(outputName + "SD", getSD(stuff));
    LogInfo.logs("%s: %.3f%% +- %.3f%%   [%s]", logName, getAverage(stuff) * 100, getSD(stuff) * 100,
        Fmt.D(ListUtils.mult(100.0, stuff)));
  }
}