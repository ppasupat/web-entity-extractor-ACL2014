package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.*;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.ling.AveragedWordVector;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import edu.stanford.nlp.semparse.open.util.StringSampler;
import fig.basic.Option;

/**
 * A CandidateGroup is a collection of candidates with the same selected KNodes
 * (and thus the same selected entity strings).
 */
public class CandidateGroup {
  public static class Options {
    @Option(gloss = "level of entity string normalization when creating candidate group "
                  + "(0 = none / 1 = whitespace / 2 = simple / 3 = aggressive)")
    public int lateNormalizeEntities = 2;
  }
  public static Options opts = new Options();

  public final Example ex;
  public final List<KNode> selectedNodes;
  public final List<String> predictedEntities;
  final List<Candidate> candidates;
  public FeatureVector features;
  public AveragedWordVector averagedWordVector;
  
  public CandidateGroup(Example ex, List<KNode> selectedNodes) {
    this.ex = ex;
    this.selectedNodes = new ArrayList<>(selectedNodes);
    List<String> entities = new ArrayList<>();
    for (KNode node : selectedNodes) {
      entities.add(LingUtils.normalize(node.fullText, opts.lateNormalizeEntities));
    }
    predictedEntities = new ArrayList<>(entities);
    candidates = new ArrayList<>();
  }
  
  public void initAveragedWordVector() {
    if (averagedWordVector == null)
      averagedWordVector = new AveragedWordVector(predictedEntities);
  }
  
  public int numEntities() {
    return predictedEntities.size();
  }
  
  public int numCandidate() {
    return candidates.size();
  }
  
  public List<Candidate> getCandidates() {
    return Collections.unmodifiableList(candidates);
  }
  
  public Candidate addCandidate(TreePattern pattern) {
    return new Candidate(this, pattern);
  }
  
  public double getReward() {
    return ex.expectedAnswer.reward(this);
  }
  
  // ============================================================
  // Debug Print
  // ============================================================
 
  public String sampleEntities() {
    return StringSampler.sampleEntities(predictedEntities, StringSampler.DEFAULT_LIMIT);
  }

  public String allEntities() {
    return StringSampler.sampleEntities(predictedEntities);
  }

}
