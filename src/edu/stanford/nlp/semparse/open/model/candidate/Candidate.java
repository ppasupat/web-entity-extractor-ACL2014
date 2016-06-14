package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.*;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.FeatureVector;

/**
 * A Candidate is a possible set of predicted entities.
 */
public class Candidate {
  public final Example ex;
  public final CandidateGroup group;
  public final TreePattern pattern;
  public final List<String> predictedEntities;
  public FeatureVector features;
  
  public Candidate(CandidateGroup group, TreePattern pattern) {
    this.pattern = pattern;
    this.group = group;
    group.candidates.add(this);
    // Perform shallow copy
    this.ex = group.ex;
    this.predictedEntities = group.predictedEntities;
  }
  
  public int numEntities() {
    return group.numEntities();
  }
  
  public double getReward() {
    return group.ex.expectedAnswer.reward(this);
  }
  
  public Map<String, Double> getCombinedFeatures() {
    Map<String, Double> map = new HashMap<>();
    features.increment(1, map);
    group.features.increment(1, map);
    return map;
  }
  
  // ============================================================
  // Debug Print
  // ============================================================
  
  public String sampleEntities() {
    return group.sampleEntities();
  }
  
  public String allEntities() {
    return group.allEntities();
  }
}