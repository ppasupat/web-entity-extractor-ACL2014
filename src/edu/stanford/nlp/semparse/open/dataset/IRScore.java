package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.util.BipartiteMatcher;

public class IRScore {
  public final int numCorrect, numPredicted, numGold;
  public final double precision, recall, f1;

  public IRScore(int numCorrect, int numPredicted, int numGold) {
    this.numCorrect = numCorrect;
    this.numPredicted = numPredicted;
    this.numGold = numGold;
    precision = (numPredicted == 0) ? 0 : numCorrect * 1.0 / numPredicted;
    recall = numCorrect * 1.0 / numGold;
    f1 = (numCorrect == 0) ? 0 : (2 * precision * recall) / (precision + recall);
  }
  
  public IRScore(List<TargetEntity> expected, List<String> predicted) {
    this(new BipartiteMatcher(expected, predicted).findMaximumMatch(),
        predicted.size(), expected.size());
  }

  @Override
  public String toString() {
    return String.format("[ Precision = %.2f (%d/%d) | Recall = %.2f (%d/%d) | F1 = %.2f ]", precision * 100,
        numCorrect, numPredicted, recall * 100, numCorrect, numGold, f1 * 100);
  }
}