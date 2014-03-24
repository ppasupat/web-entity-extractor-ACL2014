package edu.stanford.nlp.semparse.open.model;

public class FeatureDomainPruner implements FeatureMatcher {
  
  public enum FeatureDomainPrunerType { ONLY_ALLOW_DOMAIN, ONLY_DISALLOW_DOMAIN };

  public final String domainPrefix;
  public final FeatureDomainPrunerType type;
  
  public FeatureDomainPruner(String domain, FeatureDomainPrunerType type) {
    this.domainPrefix = domain + " :: ";
    this.type = type;
  }
  
  @Override
  public boolean matches(String feature) {
    // Always allow "basic"
    if (feature.startsWith("basic :: ")) return true;
    boolean matched = feature.startsWith(domainPrefix);
    if (type == FeatureDomainPrunerType.ONLY_ALLOW_DOMAIN) {
      return matched;
    } else {
      return !matched;
    }
  }
}
