package edu.stanford.nlp.semparse.open.model;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.LogInfo;

public class FeatureCountPruner implements FeatureMatcher {

  public Multiset<String> counts = HashMultiset.create();
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
    Set<String> uniqued = Sets.newHashSet();
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
    Iterator<String> iter = counts.iterator();
    while (iter.hasNext()) {
      if (counts.count(iter.next()) < minimumCount) iter.remove();
    }
    if (!beVeryQuiet) LogInfo.logs("Pruned #Features: %d", counts.elementSet().size());
    if (!beVeryQuiet) LogInfo.end_track();
  }

  @Override
  public boolean matches(String feature) {
    return counts.contains(feature);
  }
  
}
