package edu.stanford.nlp.semparse.open.util;

import fig.basic.ListUtils;

/**
 * VectorAverager computes the term-wise average of several vectors.
 * It also computes term-wise minimum and maximum.
 */
public class VectorAverager {
  final int dim;
  double[] aggregate, min, max;
  int count = 0;
  double sumFactors = 0;
  
  public VectorAverager(int numDimensions) {
    dim = numDimensions;
    aggregate = new double[dim];
  }
  
  public void add(double[] vector) {
    if (vector == null) return;
    count++;
    sumFactors++;
    if (min == null) min = vector.clone();
    if (max == null) max = vector.clone();
    for (int i = 0; i < dim; i++) {
      aggregate[i] += vector[i];
    }
  }
  
  public void add(double[] vector, double factor) {
    if (vector == null) return;
    count++;
    sumFactors += factor;
    if (min == null) min = ListUtils.mult(factor, vector);
    if (max == null) max = ListUtils.mult(factor, vector);
    for (int i = 0; i < dim; i++) {
      aggregate[i] += vector[i] * factor;
      min[i] = Math.min(min[i], vector[i] * factor);
      max[i] = Math.max(max[i], vector[i] * factor);
    }
  }
  
  public double[] getSum() {
    return aggregate.clone();
  }
  /*
  public double[] getAverage() {
    if (count == 0) return null;
    double[] answer = new double[dim];
    for (int i = 0; i < dim; i++)
      answer[i] = aggregate[i] / count;
    return answer;
  }
  */
  public double[] getAverage() {
    if (sumFactors < 1e-6) return null;
    double[] answer = new double[dim];
    for (int i = 0; i < dim; i++)
      answer[i] = aggregate[i] / sumFactors;
    return answer;
  }
  
  public double[] getMin() {
    if (count == 0) return null;
    return min.clone();
  }
  
  public double[] getMax() {
    if (count == 0) return null;
    return max.clone();
  }
  
  /** Term-wise largest magnitude **/
  public double[] getAbsMax() {
    if (count == 0) return null;
    double[] absMax = new double[dim];
    for (int i = 0; i < dim; i++) {
      absMax[i] = Math.max(Math.abs(max[i]), Math.abs(min[i]));
    }
    return absMax;
  }
  
  /** Min and Max concatenated **/
  public double[] getMinmax() {
    if (count == 0) return null;
    double[] minmax = new double[dim * 2];
    System.arraycopy(min, 0, minmax, 0, dim);
    System.arraycopy(max, 0, minmax, dim, dim);
    return minmax;
  }
}
