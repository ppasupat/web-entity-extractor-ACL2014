package edu.stanford.nlp.semparse.open.model.feature;

import java.util.regex.Pattern;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.ling.QueryTypeTable;
import edu.stanford.nlp.semparse.open.ling.WordNetClusterTable;
import edu.stanford.nlp.semparse.open.model.FeatureMatcher;
import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import fig.basic.Fmt;
import fig.basic.LogInfo;
import fig.basic.Option;

public class FeaturePostProcessorConjoin extends FeaturePostProcessor {
  public static class Options {
    @Option(gloss = "conjoin features with an abstract representation of the query")
    public boolean useConjoin = false;
    
    @Option public String cjQueryTypeName = null;
    @Option public boolean cjConjoinWithWordNetClusters = false;
    
    @Option public String cjRegExConjoin = "^(ling|entity).*";
    @Option public boolean cjKeepOriginalFeatures = false;
    @Option public double cjScaleConjoinFeatures = 1.0;
  }
  public static Options opts = new Options();
  
  public static void debugPrintOptions() {
    if (opts.cjQueryTypeName != null && !opts.cjQueryTypeName.isEmpty())
      LogInfo.logs("Conjoining query type: %s", opts.cjQueryTypeName);
    else
      LogInfo.log("Conjoining ALL query types");
    if (opts.cjRegExConjoin != null && !opts.cjRegExConjoin.isEmpty())
      LogInfo.logs("Conjoining features matching regex: %s", opts.cjRegExConjoin);
    else
      LogInfo.log("Conjoining ALL features");
    if (opts.cjKeepOriginalFeatures)
      LogInfo.log("... also keep original features");
    if (opts.cjScaleConjoinFeatures != 1.0)
      LogInfo.logs("... also scale conjoined features by %s", Fmt.D(opts.cjScaleConjoinFeatures));
  }

  @Override
  public void process(Candidate candidate) {
    if (!opts.useConjoin) return;
    String prefix = getConjoiningPrefix(candidate.ex);
    candidate.features = getConjoinedFeatureVector(candidate.features, prefix);
  }
  
  @Override
  public void process(CandidateGroup group) {
    if (!opts.useConjoin) return;
    String prefix = getConjoiningPrefix(group.ex);
    group.features = getConjoinedFeatureVector(group.features, prefix);
  }
  
  // ============================================================
  // Compute the abstract representation g(query)
  // ============================================================

  private String getQueryType(Example ex) {
    return getQueryType(ex.phrase);
  }
  
  private String getQueryType(String phrase) {
    String queryType;
    if (opts.cjConjoinWithWordNetClusters) {
      queryType = WordNetClusterTable.getCluster(LingUtils.findHeadWord(phrase, true));
    } else {
      queryType = QueryTypeTable.getQueryType(phrase);
    }
    return "" + queryType;
  }
  
  private String getConjoiningPrefix(Example ex) {
    if (opts.cjQueryTypeName != null && !opts.cjQueryTypeName.isEmpty())
      return opts.cjQueryTypeName.equals(getQueryType(ex)) ? "I" : "O";
    else
      return getQueryType(ex);
  }
  
  // ============================================================
  // Converting feature f to (g(query), f)
  // ============================================================

  class RegExFeatureMatcher implements FeatureMatcher {
    public final Pattern regex;
    public final boolean inverse;
    
    public RegExFeatureMatcher(String regex) {
      this(Pattern.compile(regex), false);
    }
    
    public RegExFeatureMatcher(Pattern regex) {
      this(regex, false);
    }
    
    public RegExFeatureMatcher(String regex, boolean inverse) {
      this(Pattern.compile(regex), inverse);
    }
    
    public RegExFeatureMatcher(Pattern regex, boolean inverse) {
      this.regex = regex;
      this.inverse = inverse;
    }
    
    @Override
    public boolean matches(String feature) {
      boolean match = regex.matcher(feature).matches();
      return inverse ? !match : match;
    }
  }
  
  private FeatureVector getConjoinedFeatureVector(FeatureVector vOld, String queryType) {
    FeatureVector v = new FeatureVector();
    if (opts.cjRegExConjoin != null) {
      FeatureMatcher matcher = new RegExFeatureMatcher(opts.cjRegExConjoin),
          invMatcher = new RegExFeatureMatcher(opts.cjRegExConjoin, true);
      if (opts.cjKeepOriginalFeatures) {
        v.addConjoin(vOld, "ALL");
      } else {
        v.addConjoin(vOld, "ALL", invMatcher);
      }
      if (opts.cjScaleConjoinFeatures != 1.0) {
        v.addConjoin(vOld, queryType, matcher, opts.cjScaleConjoinFeatures);
      } else {
        v.addConjoin(vOld, queryType, matcher);
      }
    } else {
      if (opts.cjKeepOriginalFeatures) v.addConjoin(vOld, "ALL");
      v.addConjoin(vOld, queryType);
    }
    return v;
  }
  
}
