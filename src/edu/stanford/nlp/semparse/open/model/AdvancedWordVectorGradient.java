package edu.stanford.nlp.semparse.open.model;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;

public interface AdvancedWordVectorGradient {

  public void addToGradient(Candidate candidate, double factor);
  public void addL2Regularization(double beta);
  
}
