package edu.stanford.nlp.semparse.open.model;

import edu.stanford.nlp.semparse.open.ling.WordVectorTable;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.Option;

/**
 * Parameters for advanced word vector features.
 */
public abstract class AdvancedWordVectorParams {
  public static class Options {
    @Option(gloss = "Whether to use full rank")
    public boolean vecFullRank = true;
    
    @Option(gloss = "Use pooling (vecOpenPOSOnly and vecFreqWeighted will be ignored)")
    public boolean vecPooling = false;
    
    @Option(gloss = "Only use Open POS words")
    public boolean vecOpenPOSOnly = false;
    
    @Option(gloss = "Use frequency-weighted vectors")
    public boolean vecFreqWeighted = false;
  }
  public static Options opts = new Options();
  
  public static AdvancedWordVectorParams create() {
    if (opts.vecFullRank)
      return new AdvancedWordVectorParamsFullRank();
    else
      return new AdvancedWordVectorParamsLowRank();
  }
  
  protected static int getDim() {
    if (opts.vecPooling) {
      return 2 * WordVectorTable.numDimensions;
    }
    return WordVectorTable.numDimensions;
  }

  protected static double[] getX(Candidate candidate) {
    candidate.ex.initAveragedWordVector();
    if (opts.vecPooling) {
      return candidate.ex.averagedWordVector.minmax;
    }
    return candidate.ex.averagedWordVector.get(opts.vecFreqWeighted, opts.vecOpenPOSOnly);
  }

  protected static double[] getY(Candidate candidate) {
    candidate.group.initAveragedWordVector();
    if (opts.vecPooling) {
      return candidate.group.averagedWordVector.minmax;
    }
    return candidate.group.averagedWordVector.get(opts.vecFreqWeighted, opts.vecOpenPOSOnly);
  }
  
  public abstract double getScore(Candidate candidate);
  
  public abstract AdvancedWordVectorGradient createGradient();
  public abstract void update(AdvancedWordVectorGradient gradient);
  public abstract void applyL1Regularization(double cutoff);
  
  public abstract void log();
  public abstract void logFeatureWeights(Candidate candidate);
  public abstract void logFeatureDiff(Candidate trueCandidate, Candidate predCandidate);
}
