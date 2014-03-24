package edu.stanford.nlp.semparse.open.model;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.Fmt;
import fig.basic.LogInfo;

public class AdvancedWordVectorParamsFullRank extends AdvancedWordVectorParams {
  
  double[][] weights;
  
  // Shorthands for word vector dimension
  protected final int dim;
  
  public AdvancedWordVectorParamsFullRank() {
    dim = getDim();
    weights = new double[dim][dim];
    if (Params.opts.initWeightsRandomly) {
      for (int i = 0; i < dim; i++) {
        for (int j = 0; j < dim; j++) {
          weights[i][j] = 2 * Params.opts.initRandom.nextDouble() - 1;
        }
      }
    }
    initGradientStats();
  }
  
  // ============================================================
  // Get score
  // ============================================================

  @Override
  public double getScore(Candidate candidate) {
    return getScore(getX(candidate), getY(candidate));
  }
  
  public double getScore(double[] x, double[] y) {
    if (x == null || y == null)
      return 0;
    double answer = 0;
    for (int i = 0; i < dim; i++) {
      for (int j = 0; j < dim; j++) {
        answer += weights[i][j] * x[i] * y[j];
      }
    }
    return answer;
  }
  
  // ============================================================
  // Compute gradient
  // ============================================================

  @Override
  public AdvancedWordVectorGradient createGradient() {
    return new AdvancedWordVectorGradientFullRank();
  }
  
  class AdvancedWordVectorGradientFullRank implements AdvancedWordVectorGradient {
    protected final double grad[][];

    public AdvancedWordVectorGradientFullRank() {
      grad = new double[dim][dim];
    }

    @Override
    public void addToGradient(Candidate candidate, double factor) {
      addToGradient(getX(candidate), getY(candidate), factor);
    }

    /**
     * Compute the gradient for the word vector pair (x,y) and add it to the
     * accumulative gradient.
     */
    private void addToGradient(double[] x, double[] y, double factor) {
      if (x == null || y == null)
        return;
      for (int i = 0; i < dim; i++) {
        for (int j = 0; j < dim; j++) {
          grad[i][j] += x[i] * y[j] * factor;
        }
      }
    }

    @Override
    public void addL2Regularization(double beta) {
      for (int i = 0; i < dim; i++) {
        for (int j = 0; j < dim; j++) {
          grad[i][j] -= beta * weights[i][j];
        }
      }
    }
  }
  
  // ============================================================
  // Weight update
  // ============================================================
  
  // For AdaGrad
  double[][] sumSquaredGradients;

  // For dual averaging
  double[][] sumGradients;
 
  protected void initGradientStats() {
    if (Params.opts.adaptiveStepSize)
      sumSquaredGradients = new double[dim][dim];
    if (Params.opts.dualAveraging)
      sumGradients = new double[dim][dim];
  }
  
  // Number of stochastic updates we've made so far (for determining step size).
  int numUpdates;

  @Override
  public void update(AdvancedWordVectorGradient gradient) {
    AdvancedWordVectorGradientFullRank grad = (AdvancedWordVectorGradientFullRank) gradient;
    numUpdates++;
    for (int i = 0; i < dim; i++) {
      for (int j = 0; j < dim; j++) {
        double g = grad.grad[i][j];
        if (Math.abs(g) < 1e-6) continue;
        double stepSize;
        if (Params.opts.adaptiveStepSize) {
          sumSquaredGradients[i][j] += g * g;
          stepSize = Params.opts.initStepSize / Math.sqrt(sumSquaredGradients[i][j]);
        } else {
          stepSize = Params.opts.initStepSize / Math.pow(numUpdates, Params.opts.stepSizeReduction);
        }
        if (Params.opts.dualAveraging) {
          sumGradients[i][j] += g;
          weights[i][j] = stepSize * sumGradients[i][j];
        } else {
          weights[i][j] += stepSize * g;
        }
      }
    }
  }

  @Override
  public void applyL1Regularization(double cutoff) {
    if (cutoff <= 0)
      return;
    for (int i = 0; i < dim; i++) {
      for (int j = 0; j < dim; j++) {
        weights[i][j] = Params.L1Cut(weights[i][j], cutoff);
      }
    }
  }
  
  // ============================================================
  // Logging
  // ============================================================

  @Override
  public void log() {
    LogInfo.begin_track("Advanced Word Vector Params");
    for (int i = 0; i < dim; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < dim; j++) {
        sb.append(String.format("%10s ", Fmt.D(weights[i][j])));
      }
      LogInfo.log(sb.toString());
    }
    LogInfo.end_track();
  }

  @Override
  public void logFeatureWeights(Candidate candidate) {
    LogInfo.begin_track("Advanced Word Vector feature weights");
    double[] x = getX(candidate), y = getY(candidate);
    if (x == null) {
      LogInfo.log("NONE: x (query word vector) is null");
    } else if (y == null) {
      LogInfo.log("NONE: y (entities word vector) is null");
    } else {
      LogInfo.logs("Advanced Word Vector: %s", Fmt.D(getScore(x, y)));
    }
    LogInfo.end_track();
  }

  @Override
  public void logFeatureDiff(Candidate trueCandidate, Candidate predCandidate) {
    LogInfo.begin_track("Advanced Word Vector feature weights");
    // The candidates should be from the same example --> assume x are the same
    double[] x = getX(trueCandidate), yTrue = getY(trueCandidate), yPred = getY(predCandidate);
    if (x == null) {
      LogInfo.log("NONE: x (query word vector) is null");
    } else if (yTrue == null) {
      LogInfo.log("NONE: y (entities word vector) is null for trueCandidate");
    } else if (yPred == null) {
      LogInfo.log("NONE: y (entities word vector) is null for predCandidate");
    } else {
      double trueScore = getScore(x, yTrue), predScore = getScore(x, yPred);
      LogInfo.logs("Advanced Word Vector: %s [ %s - %s ]", Fmt.D(trueScore - predScore),
          Fmt.D(trueScore), Fmt.D(predScore));
    }
    LogInfo.end_track();
  }
  
}
