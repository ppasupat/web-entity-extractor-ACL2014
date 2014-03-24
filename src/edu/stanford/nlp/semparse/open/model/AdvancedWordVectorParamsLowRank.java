package edu.stanford.nlp.semparse.open.model;

import java.util.Random;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.Fmt;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.basic.Option;

public class AdvancedWordVectorParamsLowRank extends AdvancedWordVectorParams {
  public static class Options {
    @Option(gloss = "Rank of advanced word vector feature parameters (ignored when full rank)")
    public int vecRank = 5;
    
    @Option(gloss = "Randomly initialize the weights (ignored when full rank)")
    public boolean vecInitWeightsRandomly = true;
    
    @Option(gloss = "Randomly initialize the weights (ignored when full rank)")
    public Random vecInitRandom = new Random(1);
  }
  public static Options opts = new Options();
  
  // A = sum{u[i] v[i]^T} (i = 0, ..., rank - 1)
  protected final double[][] u, v;

  // Shorthands for rank and word vector dimension
  protected final int rank, dim;

  public AdvancedWordVectorParamsLowRank() {
    rank = opts.vecRank;
    dim = getDim();
    u = new double[rank][dim];
    v = new double[rank][dim];
    if (opts.vecInitWeightsRandomly) {
      for (int i = 0; i < rank; i++) {
        for (int j = 0; j < dim; j++) {
          u[i][j] = 2 * opts.vecInitRandom.nextDouble() - 1;
          v[i][j] = 2 * opts.vecInitRandom.nextDouble() - 1;
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

  /**
   * Return the score = x^T A y = sum{(u[i]^T x)(v[i]^T y)}
   */
  public double getScore(double[] x, double[] y) {
    if (x == null || y == null)
      return 0;
    double answer = 0;
    for (int i = 0; i < rank; i++) {
      answer += ListUtils.dot(u[i], x) * ListUtils.dot(v[i], y);
    }
    return answer;
  }

  // ============================================================
  // Compute gradient
  // ============================================================
  
  @Override
  public AdvancedWordVectorGradient createGradient() {
    return new AdvancedWordVectorGradientLowRank();
  }
  
  class AdvancedWordVectorGradientLowRank implements AdvancedWordVectorGradient {
    protected final double gradU[][], gradV[][];

    public AdvancedWordVectorGradientLowRank() {
      gradU = new double[rank][dim];
      gradV = new double[rank][dim];
    }

    @Override
    public void addToGradient(Candidate candidate, double factor) {
      addToGradient(getX(candidate), getY(candidate), factor);
    }

    /**
     * Compute the gradient for the word vector pair (x,y) and add it to the
     * accumulative gradient.
     * 
     * The gradient of (x^T A y) with respect to u[i] is (v[i]^T y) x The
     * gradient of (x^T A y) with respect to v[i] is (u[i]^T x) y
     */
    private void addToGradient(double[] x, double[] y, double factor) {
      if (x == null || y == null)
        return;
      for (int i = 0; i < rank; i++) {
        ListUtils.incr(gradU[i], factor * ListUtils.dot(v[i], y), x);
        ListUtils.incr(gradV[i], factor * ListUtils.dot(u[i], x), y);
      }
    }

    @Override
    public void addL2Regularization(double beta) {
      for (int i = 0; i < rank; i++) {
        ListUtils.incr(gradU[i], -beta, u[i]);
        ListUtils.incr(gradV[i], -beta, v[i]);
      }
    }
  }

  // ============================================================
  // Weight update
  // ============================================================

  // For AdaGrad
  double[][] sumSquaredGradientsU, sumSquaredGradientsV;

  // For dual averaging
  double[][] sumGradientsU, sumGradientsV;

  protected void initGradientStats() {
    if (Params.opts.adaptiveStepSize) {
      sumSquaredGradientsU = new double[rank][dim];
      sumSquaredGradientsV = new double[rank][dim];
    }
    if (Params.opts.dualAveraging) {
      sumGradientsU = new double[rank][dim];
      sumGradientsV = new double[rank][dim];
    }
  }

  // Number of stochastic updates we've made so far (for determining step size).
  int numUpdates;

  /**
   * Update u and v with the gradient
   */
  @Override
  public void update(AdvancedWordVectorGradient gradient) {
    AdvancedWordVectorGradientLowRank g = (AdvancedWordVectorGradientLowRank) gradient;
    numUpdates++;
    for (int i = 0; i < rank; i++) {
      if (Params.opts.adaptiveStepSize) {
        ListUtils.incr(sumSquaredGradientsU[i], 1, ListUtils.sq(g.gradU[i]));
        ListUtils.incr(sumSquaredGradientsV[i], 1, ListUtils.sq(g.gradV[i]));
      }
      if (Params.opts.dualAveraging) {
        ListUtils.incr(sumGradientsU[i], 1, g.gradU[i]);
        ListUtils.incr(sumGradientsV[i], 1, g.gradV[i]);
      }
      for (int j = 0; j < dim; j++) {
        double stepSizeU, stepSizeV;
        if (Params.opts.adaptiveStepSize) {
          stepSizeU = Params.opts.initStepSize / Math.sqrt(sumSquaredGradientsU[i][j]);
          stepSizeV = Params.opts.initStepSize / Math.sqrt(sumSquaredGradientsV[i][j]);
        } else {
          stepSizeU = stepSizeV = Params.opts.initStepSize / Math.pow(numUpdates, Params.opts.stepSizeReduction);
        }
        if (Params.opts.dualAveraging) {
          u[i][j] = stepSizeU * sumGradientsU[i][j];
          v[i][j] = stepSizeV * sumGradientsV[i][j];
        } else {
          u[i][j] += stepSizeU * g.gradU[i][j];
          v[i][j] += stepSizeV * g.gradV[i][j];
        }
      }
    }
  }

  /**
   * Apply L1 regularization: - If weight > cutoff, then weight := weight -
   * cutoff - If weight < -cutoff, then weight := weight + cutoff - Otherwise,
   * weight := 0
   * 
   * @param cutoff
   *          regularization parameter (>= 0)
   */
  @Override
  public void applyL1Regularization(double cutoff) {
    if (cutoff <= 0)
      return;
    for (int i = 0; i < rank; i++) {
      for (int j = 0; j < dim; j++) {
        u[i][j] = Params.L1Cut(u[i][j], cutoff);
        v[i][j] = Params.L1Cut(v[i][j], cutoff);
      }
    }
  }

  // ============================================================
  // Logging
  // ============================================================

  @Override
  public void log() {
    LogInfo.begin_track("Advanced Word Vector Params");
    for (int i = 0; i < rank; i++) {
      LogInfo.begin_track("u[%d] and v[%d]", i, i);
      for (int j = 0; j < dim; j++) {
        LogInfo.logs("%4d %6s %6s", j, Fmt.D(u[i][j]), Fmt.D(v[i][j]));
      }
      LogInfo.end_track();
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
      for (int i = 0; i < rank; i++) {
        LogInfo.logs("%4d: %6s", i, Fmt.D(ListUtils.dot(u[i], x) * ListUtils.dot(v[i], y)));
      }
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
      for (int i = 0; i < rank; i++) {
        double trueScore = ListUtils.dot(u[i], x) * ListUtils.dot(v[i], yTrue);
        double predScore = ListUtils.dot(u[i], x) * ListUtils.dot(v[i], yPred);
        LogInfo.logs("%4d: %6s [ %s - %s ]", i, Fmt.D(trueScore - predScore),
            Fmt.D(trueScore), Fmt.D(predScore));
      }
    }
    LogInfo.end_track();
  }

}
