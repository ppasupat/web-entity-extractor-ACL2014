package edu.stanford.nlp.semparse.open.model.feature;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;

public class FeatureTypeProximityBased extends FeatureType {

  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }

  @Override
  public void extract(CandidateGroup group) {
    extractQueryProximityFeatures(group);
  }
  
  protected void extractQueryProximityFeatures(CandidateGroup group) {
    if (isAllowedDomain("proximity")) {
      
    }
  }

}
