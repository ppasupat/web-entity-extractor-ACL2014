package edu.stanford.nlp.semparse.open.model;

import java.util.*;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.util.Multiset;
import fig.basic.LogInfo;

public class FeatureCountPruner implements FeatureMatcher {

  public Multiset<String> counts = new Multiset<>();
  public boolean beVeryQuiet;
  
  public FeatureCountPruner(boolean beVeryQuiet) {
    this.beVeryQuiet = beVeryQuiet;
  }
  
  /**
   * Add features from the example to the count.
   * 
   * The same feature within the same example counts as 1 feature.
   */
  public void add(Example example) {
    if (!beVeryQuiet) LogInfo.begin_track("Collecting features from %s ...", example);
    Set<String> uniqued = new HashSet<>();
    for (Candidate candidate : example.candidates) {
      for (String name : candidate.getCombinedFeatures().keySet()) {
        uniqued.add(name);
      }
    }
    for (String name : uniqued) counts.add(name);
    if (!beVeryQuiet) LogInfo.end_track();
  }
  
  /**
   * Prune the features with count < minimumCount
   */
  public void applyThreshold(int minimumCount) {
    if (!beVeryQuiet) LogInfo.begin_track("Pruning features with count < %d ...", minimumCount);
    if (!beVeryQuiet) LogInfo.logs("Original #Features: %d", counts.elementSet().size());
    counts = counts.getPrunedByCount(minimumCount);
    if (!beVeryQuiet) LogInfo.logs("Pruned #Features: %d", counts.elementSet().size());
    if (!beVeryQuiet) LogInfo.end_track();
  }

  @Override
  public boolean matches(String feature) {
    return counts.contains(feature);
  }
  
}
