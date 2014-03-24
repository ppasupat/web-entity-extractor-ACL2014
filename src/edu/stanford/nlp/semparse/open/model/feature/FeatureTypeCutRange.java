package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;

import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.candidate.PathEntry;
import edu.stanford.nlp.semparse.open.model.candidate.TreePatternAndRange;

/**
 * Features on the range-cut candidates
 */
public class FeatureTypeCutRange extends FeatureType {

  @Override
  public void extract(Candidate candidate) {
    extractCutRangeFeatures(candidate);
  }
  
  @Override
  public void extract(CandidateGroup group) {
    // Do nothing
  }
  
  protected void extractCutRangeFeatures(Candidate candidate) {
    if ((candidate.pattern instanceof TreePatternAndRange) && isAllowedDomain("cutrange")) {
      // Amount of range cut
      TreePatternAndRange pattern = (TreePatternAndRange) candidate.pattern;
      if (pattern.amountCutStart + pattern.amountCutEnd > 0) {
        addConjunctiveFeatures(candidate.features, "cutrange", "has-cut", pattern);
        if (pattern.amountCutStart == 1 && pattern.amountCutEnd == 0)
          addConjunctiveFeatures(candidate.features, "cutrange", "cut-first-only", pattern);
        if (pattern.amountCutStart == 0 && pattern.amountCutEnd == 1)
          addConjunctiveFeatures(candidate.features, "cutrange", "cut-last-only", pattern);
        if (pattern.amountCutStart > 0)
          addConjunctiveFeatures(candidate.features, "cutrange", "cut-front", pattern);
        if (pattern.amountCutEnd > 0)
          addConjunctiveFeatures(candidate.features, "cutrange", "cut-back", pattern);
      }
      // What are at the cut points?
      
    }
  }

  protected void addConjunctiveFeatures(FeatureVector v, String domain, String name,
      TreePatternAndRange pattern) {
    List<PathEntry> path = pattern.getPath();
    v.add(domain, name);
    v.add(domain, name + " | tag = " + path.get(path.size() - 1));
  }
  
}
