package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.core.eval.CandidateStatistics;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityString;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.util.StringSampler;
import fig.basic.LogInfo;
import fig.basic.Option;

public abstract class ExpectedAnswer {
  public static class Options {
    @Option public int logRewardVerbosity = 0;
  }
  public static Options opts = new Options();
  
  public final List<TargetEntity> targetEntities;
  
  public ExpectedAnswer(TargetEntity... targetEntities) {
    this.targetEntities = Lists.newArrayList(targetEntities);
  }
  
  public ExpectedAnswer(List<TargetEntity> targetEntities) {
    this.targetEntities = Lists.newArrayList(targetEntities);
  }
  
  public ExpectedAnswer(String... targetStrings) {
    this.targetEntities = Lists.newArrayList();
    for (String targetString : targetStrings) {
      this.targetEntities.add(new TargetEntityString(targetString));
    }
  }
  
  public int size() {
    return targetEntities.size();
  }
  
  // ============================================================
  // Debug print entities
  // ============================================================
  
  public String sampleEntities() {
    return StringSampler.sampleEntities(targetEntities, StringSampler.DEFAULT_LIMIT);
  }
  
  public String allEntities() {
    return StringSampler.sampleEntities(targetEntities);
  }
  
  // ============================================================
  // Information Retrieval (Precision-Recall-F1) Scores
  // ============================================================
  
  public IRScore getIRScore(Candidate candidate) {
    return getIRScore(candidate.predictedEntities);
  }
  
  abstract public IRScore getIRScore(List<String> predictedEntities);
  
  // ============================================================
  // Reward function
  // ============================================================

  /**
   * Use to control logging verbosity.
   * If logRewardVerbosity == 1, log the reward only when frozenReward is false
   * Otherwise, frozenReward is ignored:
   * - logRewardVerbosity < 1 : don't log
   * - logRewardVerbosity > 1 : always log
   */
  public boolean frozenReward = false;
  
  /**
   * Compute the reward (in the range 0 - 1)
   */
  public double reward(Candidate candidate) {
    double reward = reward(candidate.predictedEntities);
    if (reward > 0 && (opts.logRewardVerbosity >= 2 || (opts.logRewardVerbosity >= 1 && !frozenReward))) {
      LogInfo.logs("reward = %s <<< %s", reward, candidate.sampleEntities());
      LogInfo.logs("         %s", candidate.pattern);
    }
    return reward;
  }
  
  /**
   * Compute the reward (in the range 0 - 1)
   */
  abstract public double reward(List<String> predictedEntities);
  
  // ============================================================
  // Count the number of correct entities
  // ============================================================
  
  protected Map<List<String>, Integer> cachedCountCorrectEntities = new ConcurrentHashMap<>();
  
  /**
   * Count the number of correct entities (cached version)
   */
  public int countCorrectEntities(List<String> predictedEntities) {
    Integer count = cachedCountCorrectEntities.get(predictedEntities);
    if (count == null) {
      count = computeCountCorrectEntities(predictedEntities);
      cachedCountCorrectEntities.put(predictedEntities, count);
    }
    return count;
  }
  
  /**
   * Count the number of correct entities (cached version)
   */
  public int countCorrectEntities(Candidate candidate) {
    return countCorrectEntities(candidate.predictedEntities);
  }
  
  /**
   * Count the number of correct entities (uncached version)
   */
  public int countCorrectEntitiesNoCache(List<String> predictedEntities) {
    return computeCountCorrectEntities(predictedEntities);
  }
  
  /**
   * Count the number of correct entities (uncached version)
   */
  public int countCorrectEntitiesNoCache(Candidate candidate) {
    return computeCountCorrectEntities(candidate.predictedEntities);
  }
  
  /**
   * Count the number of correct entities
   */
  abstract public int computeCountCorrectEntities(List<String> predictedEntities);
  
  // ============================================================
  // Check if the candidate is likely correct
  // ============================================================
  
  /**
   * Return true if the candidate is probably the correct answer.
   * 
   * Since the list of expected entities may be incomplete, we can only estimate
   * whether the given candidate is a correct one.
   */
  public boolean isLikelyCorrect(Candidate candidate) {
    return isLikelyCorrect(candidate.predictedEntities);
  }
  
  /**
   * Return true if the candidate is probably the correct answer.
   * 
   * Since the list of expected entities may be incomplete, we can only estimate
   * whether the given candidate is a correct one.
   */
  abstract public boolean isLikelyCorrect(List<String> predictedEntities);
  
  /**
   * Find the FIRST likely correct candidate in the list.
   * Return null if no candidate is likely correct.
   */
  public CandidateStatistics findFirstTrueCandidate(List<CandidateStatistics> rankedCandidateStats) {
    for (CandidateStatistics candidateStat : rankedCandidateStats) {
      if (isLikelyCorrect(candidateStat.candidate)) {
        return candidateStat;
      }
    }
    return null;
  }
  
  /**
   * Find the MOST likely correct candidate in the list.
   * Among the most likely correct candidates, choose the first one.
   * Return null if no candidate is likely correct.
   */
  public abstract CandidateStatistics findBestCandidate(List<CandidateStatistics> rankedCandidateStats);
  
}
