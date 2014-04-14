package edu.stanford.nlp.semparse.open.model.feature;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import fig.basic.LogInfo;

public abstract class FeaturePostProcessor {

  public abstract void process(Candidate candidate);
  public abstract void process(CandidateGroup group);
  
  public static void checkFeaturePostProcessorOptionsSanity() {
    if (FeaturePostProcessorConjoin.opts.useConjoin) {
      LogInfo.begin_track("Feature post-processor: Conjoin");
      FeaturePostProcessorConjoin.debugPrintOptions();
      LogInfo.end_track();
    }
  }
  
}
