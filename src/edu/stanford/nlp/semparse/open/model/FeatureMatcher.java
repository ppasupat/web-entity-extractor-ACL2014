package edu.stanford.nlp.semparse.open.model;

/**
 * Used to select a subset of features (to update).
 */
public interface FeatureMatcher {
  public boolean matches(String feature);
}

/** Matches all features **/
class AllFeatureMatcher implements FeatureMatcher {
  private AllFeatureMatcher() { }
  
  @Override
  public boolean matches(String feature) { return true; }
  
  public static final AllFeatureMatcher matcher = new AllFeatureMatcher();
}

/** Matches only the specified feature **/
class ExactFeatureMatcher implements FeatureMatcher {
  private final String match;
  public ExactFeatureMatcher(String match) { this.match = match; }
  
  @Override
  public boolean matches(String feature) { return feature.equals(match); }
}

/** Matches only if all feature matchers in the list match **/
class ConjunctiveFeatureMatcher implements FeatureMatcher {
  private final FeatureMatcher[] matchers;
  public ConjunctiveFeatureMatcher(FeatureMatcher... matchers) { this.matchers = matchers; }
  
  @Override
  public boolean matches(String feature) {
    for (FeatureMatcher matcher : matchers)
      if (!matcher.matches(feature)) return false;
    return true;
  }
}