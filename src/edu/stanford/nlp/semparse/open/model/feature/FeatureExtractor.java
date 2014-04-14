package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;

/**
 * A FeatureExtractor populate candidate's features.
 * It calls the extract method of different FeatureTypes.
 */
public class FeatureExtractor {

  protected final List<FeatureType> featureTypes = Lists.newArrayList(
      new FeatureTypeNaiveEntityBased(),
      new FeatureTypeNodeBased(),
      new FeatureTypePathBased(),
      new FeatureTypeLinguisticsBased(),
      //new FeatureTypeQueryBased(),
      new FeatureTypeHoleBased()
      //new FeatureTypeCutRange()
      );
  protected final List<FeaturePostProcessor> featurePostProcessors = Lists.newArrayList(
      (FeaturePostProcessor) new FeaturePostProcessorConjoin());
  
  public void extract(Candidate candidate) {
    if (candidate.features != null) return;
    candidate.features = new FeatureVector();
    for (FeatureType featureType : featureTypes) {
      featureType.extract(candidate);
    }
    for (FeaturePostProcessor featurePostProcessor : featurePostProcessors) {
      featurePostProcessor.process(candidate);
    }
  }
  
  public void extract(CandidateGroup group) {
    if (group.features != null) return;
    group.features = new FeatureVector();
    group.features.add("basic", "bias");
    for (FeatureType featureType : featureTypes) {
      featureType.extract(group);
    }
    for (FeaturePostProcessor featurePostProcessor : featurePostProcessors) {
      featurePostProcessor.process(group);
    }
  }
  
  public static final FeatureExtractor featureExtractor = new FeatureExtractor();

}
