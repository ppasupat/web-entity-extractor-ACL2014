package edu.stanford.nlp.semparse.open.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.feature.FeatureType;
import fig.basic.Fmt;
import fig.basic.LogInfo;
import fig.basic.MapUtils;
import fig.basic.NumUtils;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

public class LearnerMaxEnt implements Learner {
  public static class Options {
    @Option(gloss = "Number of iterations to train")
    public int numTrainIters = 3;
    @Option(gloss = "L2 Regularization")
    public double beta = 0.01;
    @Option(gloss = "L1 Regularization")
    public double lambda = 0;
    @Option(gloss = "Only retain features with parameter weight of at least this magnitude")
    public double pruneSmallFeaturesThreshold = 0;
    @Option(gloss = "Keep features that occur at least this many times")
    public int featureMinimumCount = 0;
    @Option
    public boolean getOnly1CandidatePerGroup = false;
  }
  public static Options opts = new Options();

  protected Params params; // Parameters of the model
  protected AdvancedWordVectorParams advancedWordVectorParams;
  protected IterativeTester iterativeTester;
  
  public boolean beVeryQuiet = false;
  
  // ============================================================
  // Log
  // ============================================================
  
  @Override
  public void logParam() {
    params.log();
    if (advancedWordVectorParams != null) {
      advancedWordVectorParams.log();
    }
  }
  
  @Override
  public void logFeatureWeights(Candidate candidate) {
    LogInfo.begin_track("Features: [sum = %s]", Fmt.D(getScore(candidate)));
    FeatureVector.logFeatureWeights("normal", candidate.getCombinedFeatures(), params);
    if (advancedWordVectorParams != null) {
      advancedWordVectorParams.logFeatureWeights(candidate);
    }
    LogInfo.end_track();
  }

  @Override
  public void logFeatureDiff(Candidate trueCandidate, Candidate predCandidate) {
    double trueScore = getScore(trueCandidate), predScore = getScore(predCandidate);
    LogInfo.begin_track("(TRUE - PRED) Features: [sum = %s = %s - %s]",
        Fmt.D(trueScore - predScore), Fmt.D(trueScore), Fmt.D(predScore));
    FeatureVector.logFeatureDiff("normal", trueCandidate.getCombinedFeatures(),
        predCandidate.getCombinedFeatures(), params);
    if (advancedWordVectorParams != null) {
      advancedWordVectorParams.logFeatureDiff(trueCandidate, predCandidate);
    }
    LogInfo.end_track();
  }
  
  @Override
  public void shutUp() {
    beVeryQuiet = true;
  } 
  
  // ============================================================
  // Predict
  // ============================================================
  
  /**
   * Return a list of (candidate, score) pairs sorted by score.
   */
  @Override
  public List<Pair<Candidate, Double>> getRankedCandidates(Example example) {
    List<Pair<Candidate, Double>> answer = Lists.newArrayList();
    for (Candidate candidate : getCandidates(example)) {
      answer.add(new Pair<Candidate, Double>(candidate, getScore(candidate)));
    }
    Collections.sort(answer, new Pair.ReverseSecondComparator<Candidate, Double>());
    return answer;
  }
  
  protected double getScore(Candidate candidate) {
    return getScore(candidate, AllFeatureMatcher.matcher);
  }
  
  protected double getScore(Candidate candidate, FeatureMatcher matcher) {
    double score = candidate.features.dotProduct(params, matcher);
    score += candidate.group.features.dotProduct(params, matcher);
    if (advancedWordVectorParams != null) {
      score += advancedWordVectorParams.getScore(candidate);
    }
    return score;
  }
  
  // ============================================================
  // Learn
  // ============================================================

  @Override
  public void setIterativeTester(IterativeTester tester) {
    iterativeTester = tester;
  }
  
  @Override
  public void learn(Dataset dataset, FeatureMatcher additionalFeatureMatcher) {
    dataset.cacheRewards();
    // Select features based on count
    FeatureMatcher featureMatcher;
    if (opts.featureMinimumCount > 0) {
      FeatureCountPruner pruner = new FeatureCountPruner(beVeryQuiet);
      LogInfo.begin_track("Removing features with count < %d ...", opts.featureMinimumCount);
      for (Example example : dataset.trainExamples)
        pruner.add(example);
      pruner.applyThreshold(opts.featureMinimumCount);
      LogInfo.end_track();
      featureMatcher = pruner;
    } else {
      featureMatcher = AllFeatureMatcher.matcher;
    }
    // Additional feature filter (for ablation)
    if (additionalFeatureMatcher != null)
      featureMatcher = additionalFeatureMatcher;
    // Learn parameters
    stochasticGradientDescent(dataset.trainExamples, featureMatcher);
    // Prune features will small weights
    params.prune(opts.pruneSmallFeaturesThreshold);
    if (!beVeryQuiet)
      params.write(Execution.getFile("params"));
  }
  
  protected int trainIter;      // 1, 2, ..., opts.numTrainIters

  /**
   * Perform stochastic gradient descent to learn the parameters using the maximum entropy model.
   */
  protected void stochasticGradientDescent(Collection<Example> examples, FeatureMatcher featureMatcher) {
    params = new Params();
    advancedWordVectorParams = FeatureType.usingAdvancedWordVectorFeature() ? 
        AdvancedWordVectorParams.create() : null;
    
    for (trainIter = 1; trainIter <= opts.numTrainIters; trainIter++) {
      if (!beVeryQuiet) {
        LogInfo.begin_track("Iteration %d/%d", trainIter, opts.numTrainIters);
        Execution.putOutput("currIter", trainIter);
      }
      for (Example example : examples) {
        if (!beVeryQuiet) Execution.putOutput("currExample", example.displayId);
        boolean updated = gradientUpdate(getCandidates(example), featureMatcher);
        if (!updated) {
          if (!beVeryQuiet) LogInfo.logs("Skip %s ...", example);
        } else {
          if (!beVeryQuiet) LogInfo.logs("Computed gradient for example %s ...", example);
          performL1Regularization(opts.lambda / examples.size());
        }
      }
      if (iterativeTester != null) {
        iterativeTester.message = "Iteration " + trainIter + "/" + opts.numTrainIters;
        iterativeTester.run();
      }
      if (!beVeryQuiet)
        LogInfo.end_track();
    }
    
    // Summarize
    if (iterativeTester != null && !beVeryQuiet) {
      iterativeTester.summarize();
    }
  }
  
  protected List<Candidate> getCandidates(Example example) {
    if (!opts.getOnly1CandidatePerGroup) {
      return example.candidates;
    } else {
      List<Candidate> candidates = Lists.newArrayList();
      for (CandidateGroup group : example.candidateGroups) {
        if (group.numCandidate() > 0) {
          candidates.add(group.getCandidates().get(0));
        }
      }
      return candidates;
    }
  }
  
  protected void performL1Regularization(double cutoff) {
    if (cutoff <= 0) return;
    params.applyL1Regularization(cutoff);
    if (advancedWordVectorParams != null)
      advancedWordVectorParams.applyL1Regularization(cutoff);
  }

  /**
   * Compute the gradient and update the parameters.
   * If there are no good candidates, do not update the parameters and return false.
   * Otherwise, return true.
   * 
   * If the score function is g(x,y,params), then the gradient is
   *    sum_y [ { gradient of g(x,y,params) } * expectationDiff(x,y,params) ]
   * where expectationDiff(x,y,params) is
   *    normalized (exp[g(x,y,params)]*R(y)) - normalized (exp[g(x,y,params)])
   */
  protected boolean gradientUpdate(List<Candidate> candidates, FeatureMatcher featureMatcher) {
    double[] expectationDiff = computeExpectationDiff(candidates, featureMatcher);
    if (expectationDiff == null) {
      // No good candidate -- skip example
      return false;
    }
    
    // Compute the gradient
    Map<String, Double> gradient = Maps.newHashMap();
    for (int i = 0; i < expectationDiff.length; i++) {
      Candidate candidate = candidates.get(i);
      candidate.group.features.increment(expectationDiff[i], gradient, featureMatcher);
      candidate.features.increment(expectationDiff[i], gradient, featureMatcher);
    }
    // Regularization
    if (opts.beta != 0) {
      for (String featureName : gradient.keySet()) {
        MapUtils.incr(gradient, featureName, (- opts.beta) * params.getWeight(featureName));
      }
    }
    // Perform gradient updates
    params.update(gradient);
    
    if (advancedWordVectorParams != null) {
      // Compute the gradient
      AdvancedWordVectorGradient advGradient = advancedWordVectorParams.createGradient();
      for (int i = 0; i < expectationDiff.length; i++) {
        Candidate candidate = candidates.get(i);
        advGradient.addToGradient(candidate, expectationDiff[i]);
      }
      // Regularization
      if (opts.beta != 0) {
        advGradient.addL2Regularization(opts.beta);
      }
      advancedWordVectorParams.update(advGradient);
    }
    
    return true;
  }
  
  /**
   * For each candidate i, compute
   *   expectationDiff[i] = normalized (exp[g(x,y[i],params)]*R(y)) - normalized (exp[g(x,y[i],params)])
   * 
   * Return null if any denominator for normalization is 0.
   */
  protected double[] computeExpectationDiff(List<Candidate> candidates, FeatureMatcher featureMatcher) {
    int n = candidates.size();
    double expScore[] = new double[n], expScoreTimesReward[] = new double[n];
    for (int i = 0; i < n; i++) {
      Candidate candidate = candidates.get(i);
      double prediction = getScore(candidate, featureMatcher);
      expScore[i] = prediction;
      expScoreTimesReward[i] = expScore[i] + Math.log(candidate.getReward());
    }

    // Exponentiate and normalize.
    // Skip this example if there are no good candidates.
    if (!NumUtils.expNormalize(expScore)) return null;
    if (!NumUtils.expNormalize(expScoreTimesReward)) return null;
    
    // Sum up the expectations
    double[] expectationDiff = new double[n];
    for (int i = 0; i < n; i++) {
      expectationDiff[i] = expScoreTimesReward[i] - expScore[i];
    }
    return expectationDiff;
  }
  
  // ============================================================
  // Persistence
  // ============================================================

  @Override
  public void saveModel(String path) {
    params.write(path);
  }

  @Override
  public void loadModel(String path) {
    params = new Params();
    params.read(path);
  }
}
