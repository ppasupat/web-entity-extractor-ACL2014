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
import edu.stanford.nlp.semparse.open.model.candidate.PathUtils;
import edu.stanford.nlp.semparse.open.model.candidate.TreePattern;
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
    @Option public boolean useOnlyTables = false;
    @Option public int baselineSuffixLength = 5;
    @Option public boolean baselineUseMaxSize = false;    // false = use most frequent
    @Option public int baselineMaxNumPatterns = 1000;
  }
  public static Options opts = new Options();
  
  protected IterativeTester iterativeTester;
  public boolean beVeryQuiet = false;
  
  /*
   * IDEA:
   * - Look at the training data and record the most frequent tree pattern (suffix)
   * - For a test example, find a suffix that matches -- maybe choose the longest one 
   */
  
  // Map from suffix to count
  Map<List<PathEntry>, Integer> goodPathCounts;
  
  // ============================================================
  // Log
  // ============================================================
  
  @Override
  public void logParam() {
    LogInfo.begin_track("Params");
    if (goodPathCounts == null) {
      LogInfo.log("No parameters.");
    } else {
      List<Map.Entry<List<PathEntry>, Integer>> entries = Lists.newArrayList(goodPathCounts.entrySet());
      Collections.sort(entries, new ValueComparator<List<PathEntry>, Integer>(true));
      for (Map.Entry<List<PathEntry>, Integer> entry : entries) {
        LogInfo.logs("%8d : %s", entry.getValue(), PathUtils.getXPathString(entry.getKey()));
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
      double score = opts.useOnlyTables ? getScoreOnlyTable(candidate) : getScore(candidate);
      answer.add(new Pair<Candidate, Double>(candidate, score));
    }
    Collections.sort(answer, new Pair.ReverseSecondComparator<Candidate, Double>());
    return answer;
  }
  
  
  protected double getScore(Candidate candidate) {
    List<PathEntry> suffix = PathUtils.getXPathSuffix(candidate.pattern.getPath(), opts.baselineSuffixLength);
    Integer frequency = goodPathCounts.get(suffix);
    if (frequency == null) return 0;
    return opts.baselineUseMaxSize ? candidate.predictedEntities.size() : frequency;
  }
  
  /** This is the initial baseline */
  protected static final PathEntry TR = new PathEntry("tr"), TD = new PathEntry("td", 0);
  protected double getScoreOnlyTable(Candidate candidate) {
    TreePattern pattern = candidate.pattern;
    List<PathEntry> path = pattern.getPath();
    int n = path.size();
    if (n >= 2 && path.get(n-2).equals(TR) && path.get(n-1).equals(TD)) {
      return candidate.numEntities();
    } else {
      return 0;
    }
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
    if (opts.useOnlyTables) {
      LogInfo.log("Use only tables -- no training");
      return;
    }
    Map<List<PathEntry>, Integer> pathCounts = Maps.newHashMap();
    LearnerMaxEnt.cacheRewards(dataset);
    // Learn good tree patterns (path suffix)
    LogInfo.begin_track("Learning tree patterns ...");
    for (Example ex : dataset.trainExamples) {
      for (Candidate candidate : ex.candidates) {
        if (candidate.getReward() > 0) {
          // Good candidate -- remember the tree pattern
          List<PathEntry> path = candidate.pattern.getPath(),
              suffix = PathUtils.getXPathSuffix(path, opts.baselineSuffixLength);
          MapUtils.incr(pathCounts, suffix);
        }
      }
    }
    List<Map.Entry<List<PathEntry>, Integer>> entries = Lists.newArrayList(pathCounts.entrySet());
    Collections.sort(entries, new ValueComparator<List<PathEntry>, Integer>(true));
    int n = Math.min(opts.baselineMaxNumPatterns, entries.size());
    goodPathCounts = Maps.newHashMap();
    for (Map.Entry<List<PathEntry>, Integer> entry : entries.subList(0, n)) {
      goodPathCounts.put(entry.getKey(), entry.getValue());
    }
    LogInfo.logs("Found %d path patterns.", goodPathCounts.size());
    LogInfo.end_track();
    iterativeTester.run();
  }
  
  // ============================================================
  // Persistence
  // ============================================================

  @Override
  public void saveModel(String path) {
    // TODO Auto-generated method stub
    LogInfo.fail("Not implemented");
  }

  @Override
  public void loadModel(String path) {
    // TODO Auto-generated method stub
    LogInfo.fail("Not implemented");
  }
  
}
