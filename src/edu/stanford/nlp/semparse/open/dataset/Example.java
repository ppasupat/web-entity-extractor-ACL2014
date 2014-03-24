package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import edu.stanford.nlp.semparse.open.ling.AveragedWordVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.tree.KNode;

/**
 * An Example consists of the following:
 * - A phrase (e.g., "us cities")
 * - expectedAnswer: the entities that we'd like to extract from the knowledge tree
 * - A knowledge tree (constructed based on the phrase)
 * - A list of candidate answers
 */
public class Example {
  public String displayId;  // For debugging
  
  public final String phrase;
  public final ExpectedAnswer expectedAnswer;
  public KNode tree;  // Deterministic function of the phrase
  public List<CandidateGroup> candidateGroups;
  public List<Candidate> candidates;  // Candidate predictions
  public AveragedWordVector averagedWordVector;

  public Example(String phrase) {
    this(phrase, null);
  }
  
  public Example(String phrase, ExpectedAnswer expectedAnswer) {
    this.phrase = phrase;
    this.expectedAnswer = expectedAnswer;
  }
  
  @Override public String toString() {
    return "[" + phrase + "]";
  }
  
  public void initAveragedWordVector() {
    if (averagedWordVector == null)
      averagedWordVector = new AveragedWordVector(phrase);
  }
}
