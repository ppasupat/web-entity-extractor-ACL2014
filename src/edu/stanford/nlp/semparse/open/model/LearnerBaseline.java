package edu.stanford.nlp.semparse.open.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.PathEntry;
import fig.basic.LogInfo;
import fig.basic.MapUtils;
import fig.basic.Option;
import fig.basic.Pair;
import fig.basic.ValueComparator;

/**
 * Baseline classifier.
 */
public class LearnerBaseline implements Learner {
  public static class Options {
    @Option public int baselineSuffixLength = 5;
    @Option public int baselineMaxNumPatterns = 10000;
    @Option public boolean baselineUseMaxSize = false;    // false = use most frequent
    @Option public IndexType baselineIndexType = IndexType.STAR;
    @Option public boolean baselineBagOfTags = true;
  }
  public static Options opts = new Options();
  
  public enum IndexType {NONE, STAR, FULL};
  
  protected IterativeTester iterativeTester;
  public boolean beVeryQuiet = false;
  
  /*
   * IDEA:
   * - Look at the training data and record the most frequent tree pattern (suffix)
   * - For a test example, find a suffix that matches -- maybe choose the longest one 
   */
  
  // Map from suffix to count
  Map<List<String>, Integer> goodPathCounts;
  
  // ============================================================
  // Log
  // ============================================================
  
  @Override
  public void logParam() {
    LogInfo.begin_track("Params");
    if (goodPathCounts == null) {
      LogInfo.log("No parameters.");
    } else {
      List<Map.Entry<List<String>, Integer>> entries = Lists.newArrayList(goodPathCounts.entrySet());
      Collections.sort(entries, new ValueComparator<List<String>, Integer>(true));
      for (Map.Entry<List<String>, Integer> entry : entries) {
        LogInfo.logs("%8d : %s", entry.getValue(), entry.getKey());
      }
    }
    LogInfo.end_track();
  }

  @Override
  public void logFeatureWeights(Candidate candidate) {
    LogInfo.log("Using BASELINE Learner - no features");
  }
  
  @Override
  public void logFeatureDiff(Candidate trueCandidate, Candidate predCandidate) {
    LogInfo.log("Using BASELINE Learner - no features");
  }
  

  @Override
  public void shutUp() {
    beVeryQuiet = true;
  }
  
  // ============================================================
  // Predict
  // ============================================================
  
  @Override
  public List<Pair<Candidate, Double>> getRankedCandidates(Example example) {
    List<Pair<Candidate, Double>> answer = Lists.newArrayList();
    for (Candidate candidate : example.candidates) {
      double score = getScore(candidate);
      answer.add(new Pair<Candidate, Double>(candidate, score));
    }
    Collections.sort(answer, new Pair.ReverseSecondComparator<Candidate, Double>());
    return answer;
  }
  
  
  protected double getScore(Candidate candidate) {
    List<String> suffix = getPathSuffix(candidate);
    Integer frequency = goodPathCounts.get(suffix);
    if (frequency == null) return 0;
    return opts.baselineUseMaxSize ? candidate.predictedEntities.size() : frequency;
  }
  
  // ============================================================
  // Learn
  // ============================================================
  
  @Override
  public void setIterativeTester(IterativeTester tester) {
    this.iterativeTester = tester;
  }
  
  @Override
  public void learn(Dataset dataset, FeatureMatcher additionalFeatureMatcher) {
    Map<List<String>, Integer> pathCounts = Maps.newHashMap();
    dataset.cacheRewards();
    // Learn good tree patterns (path suffix)
    if (!beVeryQuiet) LogInfo.begin_track("Learning tree patterns ...");
    for (Example ex : dataset.trainExamples) {
      for (Candidate candidate : ex.candidates) {
        if (candidate.getReward() > 0) {
          // Good candidate -- remember the tree pattern
          MapUtils.incr(pathCounts, getPathSuffix(candidate));
        }
      }
    }
    // Sort by count
    List<Map.Entry<List<String>, Integer>> entries = Lists.newArrayList(pathCounts.entrySet());
    Collections.sort(entries, new ValueComparator<List<String>, Integer>(true));
    // Retain the top n paths
    int n = Math.min(opts.baselineMaxNumPatterns, entries.size());
    goodPathCounts = Maps.newHashMap();
    for (Map.Entry<List<String>, Integer> entry : entries.subList(0, n)) {
      goodPathCounts.put(entry.getKey(), entry.getValue());
    }
    if (!beVeryQuiet) LogInfo.logs("Found %d path patterns.", goodPathCounts.size());
    if (!beVeryQuiet) LogInfo.end_track();
    iterativeTester.run();
  }
  
  private List<String> getPathSuffix(Candidate candidate) {
    return getPathSuffix(candidate.pattern.getPath());
  }
  
  private List<String> getPathSuffix(List<PathEntry> path) {
    List<String> suffix = Lists.newArrayList();
    int startIndex = Math.max(0, path.size() - opts.baselineSuffixLength);
    for (PathEntry entry : path.subList(startIndex, path.size())) {
      String strEntry = "";
      switch (opts.baselineIndexType) {
      case NONE: strEntry = entry.tag; break;
      case STAR: strEntry = entry.tag + (entry.isIndexed() ? "[*]" : ""); break;
      case FULL: strEntry = entry.toString(); break;
      }
      suffix.add(strEntry.intern());
    }
    if (opts.baselineBagOfTags)
      Collections.sort(suffix);
    return suffix;
  }
  
  // ============================================================
  // Persistence
  // ============================================================

  @Override
  public void saveModel(String path) {
    LogInfo.fail("Not implemented");
  }

  @Override
  public void loadModel(String path) {
    LogInfo.fail("Not implemented");
  }
  
}
