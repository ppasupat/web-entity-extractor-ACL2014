package edu.stanford.nlp.semparse.open.ling;

import java.util.Collection;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.ling.LingData.POSType;
import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.util.VectorAverager;

/**
 * AveragedWordVector computes and stores averaged neural net word vectors.
 */
public class AveragedWordVector {
  
  // The average (mean) of the word vectors of all tokens
  public double[] averaged;
  // Divide each word vector by word frequency
  public double[] freqWeighted;
  // Use only open POS class words
  public double[] openPOSOnly;
  // Use only open POS class words and divide each word vector by word frequency
  public double[] freqWeightedOpenPOSOnly;
  // Term-wise minimum and maximum
  public double[] min, max, minmax;
  
  public AveragedWordVector(Collection<String> phrases) {
    VectorAverager normalAverager = new VectorAverager(WordVectorTable.numDimensions),
             freqWeightedAverager = new VectorAverager(WordVectorTable.numDimensions),
              openPOSOnlyAverager = new VectorAverager(WordVectorTable.numDimensions),
      freqWeightedOpenPOSAverager = new VectorAverager(WordVectorTable.numDimensions);
    for (String phrase : phrases) {
      LingData lingData = LingData.get(phrase);
      for (int i = 0; i < lingData.length; i++) {
        String token = lingData.tokens.get(i);
        int freq = BrownClusterTable.getSmoothedFrequency(token);
        double[] vector = WordVectorTable.getVector(token);
        normalAverager.add(vector);
        freqWeightedAverager.add(vector, 1.0 / freq);
        if (lingData.posTypes.get(i) == POSType.OPEN) {
          openPOSOnlyAverager.add(vector);
          freqWeightedOpenPOSAverager.add(vector, 1.0 / freq);
        }
      }
    }
    averaged = normalAverager.getAverage();
    freqWeighted = freqWeightedAverager.getAverage();
    openPOSOnly = openPOSOnlyAverager.getAverage();
    freqWeightedOpenPOSOnly = freqWeightedOpenPOSAverager.getAverage();
    min = normalAverager.getMin();
    max = normalAverager.getMax();
    minmax = normalAverager.getMinmax();
  }
  
  public AveragedWordVector(String phrase) {
    // Slightly inefficient, but will not be called often.
    this(Lists.newArrayList(phrase));
  }
  
  public double[] get(boolean freqWeighted, boolean openPOSOnly) {
    if (freqWeighted) {
      return openPOSOnly ? this.freqWeightedOpenPOSOnly : this.freqWeighted;
    } else {
      return openPOSOnly ? this.openPOSOnly : this.averaged;
    }
  }
  
  /**
   * Add general features of the form name...[i]
   * with value = each element of the averaged word vector.
   */
  @Deprecated
  public void addTermwiseFeatures(FeatureVector v, String domain, String name) {
    if (averaged != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "[" + i + "]", averaged[i]);
    if (freqWeighted != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "-freq-weighted[" + i + "]", freqWeighted[i]);
    if (openPOSOnly != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "-open-pos[" + i + "]", openPOSOnly[i]);
  }
  
  /**
   * Add general features of the form name...[i]
   * with value = term-wise product between the averaged word vector and the given vector.
   */
  @Deprecated
  public void addTermwiseFeatures(FeatureVector v, String domain, String name, double[] factor) {
    if (factor == null) return;
    if (averaged != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "[" + i + "]", averaged[i] * factor[i]);
    if (freqWeighted != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "-freq-weighted[" + i + "]", freqWeighted[i] * factor[i]);
    if (openPOSOnly != null)
      for (int i = 0; i < WordVectorTable.numDimensions; i++)
        v.add(domain, name + "-open-pos[" + i + "]", openPOSOnly[i] * factor[i]);
  }
  
  // Too slow and memory consuming
  @Deprecated
  public static void addCrossProductFeatures(FeatureVector v, String domain, String name1, String name2,
      double[] factor1, double[] factor2) {
    if (factor1 == null || factor2 == null) return;
    for (int i = 0; i < WordVectorTable.numDimensions; i++) {
      for (int j = 0; j < WordVectorTable.numDimensions; j++) {
        v.add(domain, name1 + "[" + i + "]*" + name2 + "[" + j + "]", factor1[i] * factor2[j]);
      }
    }
  }
  
}
