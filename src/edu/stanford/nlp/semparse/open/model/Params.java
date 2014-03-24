package edu.stanford.nlp.semparse.open.model;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fig.basic.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Params contains the parameters of the model. Currently consists of a map from
 * features to weights.
 *
 * @author Percy Liang
 */
public class Params {
  public static class Options {
    @Option(gloss = "By default, all features have this weight")
    public double defaultWeight = 0;
    @Option(gloss = "Randomly initialize the weights")
    public boolean initWeightsRandomly = false;
    @Option(gloss = "Randomly initialize the weights")
    public Random initRandom = new Random(1);

    @Option(gloss = "Initial step size")
    public double initStepSize = 1;
    @Option(gloss = "How fast to reduce the step size")
    public double stepSizeReduction = 0;
    @Option(gloss = "Use the AdaGrad algorithm (different step size for each coordinate)")
    public boolean adaptiveStepSize = true;
    @Option(gloss = "Use dual averaging")
    public boolean dualAveraging = false;
  }
  public static Options opts = new Options();

  // Discriminative weights
  HashMap<String, Double> weights = new HashMap<String, Double>();
  
  public double getWeight(String f) {
    if (opts.initWeightsRandomly)
      return MapUtils.getDouble(weights, f, 2 * opts.initRandom.nextDouble() - 1);
    else
      return MapUtils.getDouble(weights, f, opts.defaultWeight);
  }

  // ============================================================
  // Weight update
  // ============================================================

  // For AdaGrad
  Map<String, Double> sumSquaredGradients = new HashMap<String, Double>();

  // For dual averaging
  Map<String, Double> sumGradients = new HashMap<String, Double>();

  // Number of stochastic updates we've made so far (for determining step size).
  int numUpdates;

  /**
   * Update weights by adding |gradient| (modified appropriately with step size).
   */
  public void update(Map<String, Double> gradient) {
    numUpdates++;

    for (Map.Entry<String, Double> entry : gradient.entrySet()) {
      String f = entry.getKey();
      double g = entry.getValue();
      if (Math.abs(g) < 1e-6) continue;
      double stepSize;
      if (opts.adaptiveStepSize) {
        MapUtils.incr(sumSquaredGradients, f, g * g);
        stepSize = opts.initStepSize / Math.sqrt(sumSquaredGradients.get(f));
      } else {
        stepSize = opts.initStepSize / Math.pow(numUpdates, opts.stepSizeReduction);
      }
      if (Double.isNaN(stepSize) || Double.isNaN(g)) {
        LogInfo.fails("WTF? %s %s %s", f, g, sumSquaredGradients.get(f));
      }
      if (opts.dualAveraging) {
        if (!opts.adaptiveStepSize && opts.stepSizeReduction != 0)
          throw new RuntimeException("Dual averaging not supported when " +
                                     "step-size changes across iterations for " +
                                     "features for which the gradient is zero");
        MapUtils.incr(sumGradients, f, g);
        MapUtils.set(weights, f, stepSize * sumGradients.get(f));
      } else {
        MapUtils.incr(weights, f, stepSize * g);
      }
    }
  }
  
  public static double L1Cut(double x, double cutoff) {
    return (x > cutoff) ? (x - cutoff) : (x < -cutoff) ? (x + cutoff) : 0;
  }
  
  /**
   * Apply L1 regularization:
   * - If weight > cutoff,  then weight := weight - cutoff
   * - If weight < -cutoff, then weight := weight + cutoff
   * - Otherwise, weight := 0
   * @param cutoff    regularization parameter (>= 0)
   */
  public void applyL1Regularization(double cutoff) {
    if (cutoff <= 0) return;
    for (Map.Entry<String, Double> entry : weights.entrySet()) {
      entry.setValue(L1Cut(entry.getValue(), cutoff));
    }
  }
  
  /**
   * Prune features with small weights
   * @param threshold    the maximum absolute value for weights to be pruned
   */
  public void prune(double threshold) {
    if (threshold <= 0) return;
    Iterator<Map.Entry<String, Double>> iter = weights.entrySet().iterator();
    while (iter.hasNext()) {
      if (Math.abs(iter.next().getValue()) < threshold) iter.remove();
    }
  }
  
  // ============================================================
  // Persistence
  // ============================================================
  
  /**
   * Read parameters from |path|.
   */
  public void read(String path) {
    LogInfo.begin_track("Reading parameters from %s", path);
    try {
      BufferedReader in = IOUtils.openIn(path);
      String line;
      while ((line = in.readLine()) != null) {
        String[] pair = Lists.newArrayList(Splitter.on('\t').split(line)).toArray(new String[2]);
        weights.put(pair[0], Double.parseDouble(pair[1]));
      }
      in.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    LogInfo.logs("Read %s weights", weights.size());
    LogInfo.end_track();
  }
  
  public void write(PrintWriter out) { write(null, out); }

  public void write(String prefix, PrintWriter out) {
    List<Map.Entry<String, Double>> entries = Lists.newArrayList(weights.entrySet());
    Collections.sort(entries, new ValueComparator<String, Double>(true));
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      out.println((prefix == null ? "" : prefix + "\t") + entry.getKey() + "\t" + value);
    }
  }

  public void write(String path) {
    LogInfo.begin_track("Params.write(%s)", path);
    PrintWriter out = IOUtils.openOutHard(path);
    write(out);
    out.close();
    LogInfo.end_track();
  }
  
  // ============================================================
  // Logging
  // ============================================================

  public void log() {
    LogInfo.begin_track("Params");
    List<Map.Entry<String, Double>> entries = Lists.newArrayList(weights.entrySet());
    Collections.sort(entries, new ValueComparator<String, Double>(true));
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      LogInfo.logs("%s\t%s", entry.getKey(), value);
    }
    LogInfo.end_track();
  }
}
