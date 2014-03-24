package edu.stanford.nlp.semparse.open.model;

import java.util.List;

import edu.stanford.nlp.semparse.open.core.eval.IterativeTester;
import edu.stanford.nlp.semparse.open.dataset.Dataset;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.Pair;

public interface Learner {
  
  // ============================================================
  // Log
  // ============================================================

  public void logParam();
  public void logFeatureWeights(Candidate candidate);
  public void logFeatureDiff(Candidate trueCandidate, Candidate predCandidate);
  public void shutUp();
  
  // ============================================================
  // Predict
  // ============================================================
  
  public List<Pair<Candidate, Double>> getRankedCandidates(Example example);
  
  // ============================================================
  // Learn
  // ============================================================
  
  public void learn(Dataset dataset, FeatureMatcher additionalFeatureMatcher);
  public void setIterativeTester(IterativeTester tester);
  
  // ============================================================
  // Persistence
  // ============================================================
  
  public void saveModel(String path);
  public void loadModel(String path);
  
}
