package edu.stanford.nlp.semparse.open.model.feature;

import java.util.*;

import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import edu.stanford.nlp.semparse.open.util.Multiset;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Base class for all feature types.
 * 
 * A feature type must extends this class and implement 2 methods:
 * - extract(CandidateGroup group) : For features that are common to all candidates in the same group
 * - extract(Candidate candidate) : For other features
 * 
 * This class also provides convenience methods for firing features.
 */
public abstract class FeatureType {
  public static class Options {
    @Option(gloss = "Set of feature domains to exclude")
    public List<String> excludeFeatureDomains = new ArrayList<>();

    @Option(gloss = "Set of feature domains to include")
    public List<String> includeFeatureDomains = new ArrayList<>();
    
    @Option(gloss = "If true, look at excludeFeatureDomains, otherwise look at includeFeatureDomains")
    public boolean useAllFeatures = true;
    
    @Option(gloss = "Number of ancestors of the selected nodes to look at when firing features")
    public int maxAncestorCount = 5;
    
    @Option(gloss = "Number of bins for binning real-valued features")
    public int numBins = 10;
    
    @Option(gloss = "Use bins instead of percent features")
    public boolean useBinsForPercents = false;
    
    @Option(gloss = "Use two features to represent percentage")
    public boolean useQuadraticPercents = false;
    
    @Option(gloss = "Bin entropy features")
    public boolean binEntropyFeatures = false;
    
    @Option(gloss = "Add majoriy percentage feature")
    public boolean addMajorityPercentageFeature = true;
    
    @Option(gloss = "Add [diverse] feature if majority ratio < minMajorityRatio")
    public boolean addDiverseFeature = false;

    @Option(gloss = "Minimum majority ratio (max = 1.0) to use the majority feature")
    public double minMajorityRatio = 0.0;
    
    @Option(gloss = "Use all features instead of aggregation")
    public boolean noAggregation = false;
    
    @Option(gloss = "Bin the mean and deviation features")
    public boolean useDiscreteMeanAndDeviation = true;
  }
  public static Options opts = new Options();
  
  // ============================================================
  // Common methods
  // ============================================================
  
  public abstract void extract(Candidate candidate);
  public abstract void extract(CandidateGroup group);
  
  public static Set<String> registeredNonBasicDomains = new HashSet<>(Arrays.asList(
      // (Structural) Node-based
      "self-or-ancestors", "node-range",
      // (Structural) Path-based
      "path-tail",
      // (Denotation) Naive Entity-based
      "entity",
      // (Denotation) Linguistic Entity-based
      "ling", "cluster", "wordvec",
      // (Denotation) Freebase
      "fb",
      // (Denotation) Token frequency
      "token-freq",
      // (Denotation) Query x Entity
      "query-entity",
      // (Structural) Hole
      "hole",
      // (Denotation) Header and Proximity
      "header"
      ));
  
  public static void checkFeatureTypeOptionsSanity() {
    if (opts.useAllFeatures && !opts.includeFeatureDomains.isEmpty())
      LogInfo.fail("includeFeatureDomains should be empty if useAllFeatures is true.");
    if (opts.useAllFeatures && !opts.includeFeatureDomains.isEmpty())
      LogInfo.fail("excludeFeatureDomains should be empty if useAllFeatures is false.");
    for (String domain : opts.includeFeatureDomains)
      if (!registeredNonBasicDomains.contains(domain))
        LogInfo.fails("Domain \"%s\" in includeFeatureDomains does not exist", domain);
    for (String domain : opts.excludeFeatureDomains)
      if (!registeredNonBasicDomains.contains(domain))
        LogInfo.fails("Domain \"%s\" in excludeFeatureDomains does not exist", domain);
    LogInfo.begin_track("feature domains in use");
    for (String domain : registeredNonBasicDomains)
      if (isAllowedDomain(domain)) LogInfo.logs("DOMAIN ON : %s", domain);
    LogInfo.end_track();
    LogInfo.begin_track("feature domains NOT in use");
    for (String domain : registeredNonBasicDomains)
      if (!isAllowedDomain(domain)) LogInfo.logs("DOMAIN OFF: %s", domain);
    LogInfo.end_track();
  }
  
  public static boolean isAllowedDomain(String domain) {
    if (opts.useAllFeatures)
      return !opts.excludeFeatureDomains.contains(domain);
    else
      return opts.includeFeatureDomains.contains(domain);
  }
  
  public static boolean usingAdvancedWordVectorFeature() {
    return isAllowedDomain("wordvec");
  }
  
  // ============================================================
  // Helper Methods
  // ============================================================

  /**
   * Add features ">= 1", ">= 2", ">= 4", ">= 8", etc.
   */
  protected void addQuantizedFeatures(FeatureVector v, String domain, String name, double value) {
    value = Math.min(value, Integer.MAX_VALUE);
    for (int i = 1; i <= value; i *= 2)
      v.add(domain, name + " >= " + i);
  }

  /**
   * Add features ">= 0%", ">= 20%", ">= 40%", etc.
   */
  protected void addPercentFeatures(FeatureVector v, String domain, String name, double value) {
    if (opts.useBinsForPercents) {
      addBinFeatures(v, domain, name, value);
      return;
    } else if (opts.useQuadraticPercents) {
      if (value > 1) {
        LogInfo.logs("WTF? %s %s %f", domain, name, value);
        value = 1;
      }
      if (value < 0) {
        LogInfo.logs("WTF? %s %s %f", domain, name, value);
        value = 0;
      }
      v.add(domain, name, value);
      v.add(domain, name + "^2", value * value);
      return;
    }
    value = value * 100;
    if (value > 100) value = 100;
    for (int i = 0; i <= value; i += 20)
      v.add(domain, name + " >= " + i + "%");
  }
  
  /**
   * Add features "in [0/10,1/10]", "in [1/10,2/10]", ...
   * Values outside range [0,1] become 0 or 1.
   */
  private void addBinFeatures(FeatureVector v, String domain, String name, double value) {
    if (value < 0) value = 0;
    if (value >= 0.9999) value = 0.9999;
    int bin = ((int) (value * opts.numBins));
    v.add(domain, String.format("%s in [%d/%d,%d/%d]", name, bin, opts.numBins, bin + 1, opts.numBins)); 
  }
  
  /**
   * Add features "~ [word]" for all words in value.
   */
  protected void addBagOfWordFeatures(FeatureVector v, String domain, String name, String value) {
    for (String word : LingUtils.getBagOfWords(value)) {
      v.add(domain, name + " ~ " + word);
    }
  }
  
  /**
   * Add features "~ [word1]|[word2]" for all word1 in value1 and word2 in value2.
   */
  protected void addBagOfWordFeatures(FeatureVector v, String domain, String name, String value1, String value2) {
    Set<String> bag1 = LingUtils.getBagOfWords(value1),
                bag2 = LingUtils.getBagOfWords(value2);
    for (String word1 : bag1) {
      for (String word2 : bag2) {
        v.add(domain, name + " ~ " + word1 + "|" + word2);
      }
    }
  }

  /**
   * Add distinct values from the specified Multiset. (Basically, add all keys of the Multiset)
   * 
   * @param bagOfWords    whether to add features for each word of the distinct values
   */
  protected <T> void addDistinctElementFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset,
      boolean bagOfWords) {
    for (T element : multiset.elementSet()) {
      if (bagOfWords) {
        addBagOfWordFeatures(v, domain, name, (String) element);
      } else {
        v.add(domain, name + " = " + element);
      }
    }
  }
  
  // ============================================================
  // KNode-based features
  // ============================================================
  
  /**
   * Add features of the form
   *   [...]-tag = ___
   *   [...]-id = ___
   *   [...]-class = ___ (each class = 1 feature)
   * based on the given KNode
   * 
   * @param bagOfWords    whether to add features for each word in the id and class
   */
  protected void addTagIdClassFeatures(FeatureVector v, String domain, String name, KNode node, boolean bagOfWords) {
    if (node.type != KNode.Type.TAG) return;
    // [...]-tag
    v.add(domain, name + "-tag = " + node.value);
    // [...]-id
    String id = node.getAttribute("id");
    if (!id.isEmpty()) {
      if (bagOfWords)
        addBagOfWordFeatures(v, domain, name + "-id", node.getAttribute("id"));
      else
        v.add(domain, name + "-id = " + id.replaceAll("[0-9]+", "0"));
    }
    // [...]-class
    String classes = node.getAttribute("class");
    if (!classes.isEmpty()) {
      if (bagOfWords)
        addBagOfWordFeatures(v, domain, name + "-class", node.getAttribute("class"));
      else
        for (String className : classes.split("\\s+"))
          v.add(domain, name + "-class = " + className.replaceAll("[0-9]+", "0"));
    }
  }
  
  // ============================================================
  // Discrete Statistics
  // ============================================================

  /**
   * Return true if the Multiset has any repeated entry
   */
  protected boolean hasDuplicateEntry(Multiset<?> multiset) {
    return multiset.entrySet().size() != multiset.size();
  }
  
  /**
   * Return the maximum amount of duplication
   */
  protected <T> int maxDuplication(Multiset<T> multiset) {
    int max = 0;
    for (Map.Entry<T, Integer> entry : multiset.entrySet())
      max = Math.max(max, entry.getValue());
    return max;
  }
  
  /**
   * Add feature [...]-max-duplication
   */
  protected <T> void addDuplicationFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset) {
    int maxDuplication = maxDuplication(multiset);
    addQuantizedFeatures(v, domain, name + "-max-duplication", maxDuplication);
  }

  /**
   * Find the majority value (value that occurs the most often). If more than
   * one values share the same majority count, assign null instead.
   */
  protected <T> T getAbsoluteMajority(Multiset<T> multiset) {
    T majority = null;
    int majorityCount = 0;
    for (Map.Entry<T, Integer> entry : multiset.entrySet()) {
      if (entry.getValue() > majorityCount) {
        majority = entry.getKey();
        majorityCount = entry.getValue();
      } else if (entry.getValue() == majorityCount) {
        majority = null;
      }
    }
    return majority;
  }
  
  /**
   * Compute the entropy of the Multiset
   * 
   * The entropy is H(x) = -sum[p(x) log p(x)]
   * where p(x) = count(x) / size(multiset)
   */
  protected <T> double getEntropy(Multiset<T> multiset) {
    if (multiset.elementSet().size() == 1) return 0.0;
    double entropy = 0.0;
    for (Map.Entry<T, Integer> entry : multiset.entrySet()) {
      double p = 1.0 * entry.getValue() / multiset.size();
      entropy -= p * Math.log(p);
    }
    return entropy;
  }
  
  protected <T> double getNormalizedEntropy(Multiset<T> multiset) {
    if (multiset.size() <= 1) return 0.0;
    return getEntropy(multiset) / Math.log(multiset.size());
  }
  
  /**
   * Add feature [...]-entropy
   */
  protected <T> void addEntropyFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset) {
    double entropy = getEntropy(multiset);
    if (entropy > 0 && multiset.size() > 1) {
      double normalizedEntropy = entropy / Math.log(multiset.size());
      if (opts.binEntropyFeatures)
        addBinFeatures(v, domain, name + "-normalized-entropy", normalizedEntropy);
      else
        v.add(domain, name + "-normalized-entropy", normalizedEntropy);
    }
  }
  
  /**
   * Add features 
   *   [...]-percent-distinct
   *   [...]-identical
   *   [...]-single
   *   [...]-majority
   * 
   * Common cases:
   * - Only one object (and therefore one majority value) --> [...]-distinct + [...]-single + ([...]-majority)
   * - Multiple objects with one majority value --> [...]-majority
   * - Multiple objects with distinct values --> [...]-distinct
   * 
   * @param bagOfWords
   *      break keys of multiset into bag of words (default = false)
   * @param addMajorityEvenIfSingle
   *      add [...]-majority feature even if there is only 1 element in the multiset (default = true)
   */
  protected <T> void addVotingFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset,
      boolean bagOfWords, boolean addMajorityEvenIfSingle) {
    if (opts.noAggregation) {
      for (Map.Entry<T, Integer> entry : multiset.entrySet()) {
        for (int i = 0; i < entry.getValue(); i++)
          v.add(domain, name + " = " + entry.getKey());
      }
      return;
    }
    if (multiset.isEmpty()) return;
    addEntropyFeatures(v, domain, name, multiset);
    //addPercentFeatures(v, domain, name + "-percent-distinct", multiset.elementSet().size() * 1.0 / multiset.size());
    T majority = getAbsoluteMajority(multiset);
    if (majority == null) return;
    double majorityRatio = multiset.count(majority) * 1.0 / multiset.size();
    if (opts.addMajorityPercentageFeature) {
      addPercentFeatures(v, domain, name + "-majority-ratio", majorityRatio);
    }
    if (majorityRatio < opts.minMajorityRatio) {
      if (opts.addDiverseFeature)
        v.add(domain, name + "-diverse");
      return;
    }
    if (majorityRatio >= 0.9) {
      if (multiset.size() > 1) {
        v.add(domain, name + "-identical");
      } else {
        v.add(domain, name + "-single");
      }
    }
    // Majority value
    if (bagOfWords) {
      addBagOfWordFeatures(v, domain, name + "-majority", (String) majority);
    } else {
      if (multiset.size() > 1 || addMajorityEvenIfSingle) {
        v.add(domain, name + "-majority = " + majority);
      }
    }
  }

  protected <T> void addVotingFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset,
      boolean bagOfWords) {
    addVotingFeatures(v, domain, name, multiset, bagOfWords, true);
  }

  protected <T> void addVotingFeatures(FeatureVector v, String domain, String name, Multiset<T> multiset) {
    addVotingFeatures(v, domain, name, multiset, false, true);
  }
    
  // ============================================================
  // Continuous statistics
  // ============================================================

  protected <T extends Number> void addMeanDeviationFeatures(FeatureVector v, String domain, String name,
      Multiset<T> multiset) {
    double mean = 0.0, variance = 0.0;
    for (T element : multiset.elementSet()) {
      double value = element.doubleValue();
      mean += value;
      variance += value * value;
    }
    mean = mean / multiset.size();
    variance = variance / multiset.size() - mean * mean;
    if (opts.useDiscreteMeanAndDeviation) {
      addQuantizedFeatures(v, domain, name + "-mean", mean);
      addQuantizedFeatures(v, domain, name + "-deviation", Math.sqrt(variance));
    } else {
      v.add(domain, name + "-mean", mean);
      v.add(domain, name + "-deviation", Math.sqrt(variance));
    }
  }
  
  // ============================================================
  // Linguistics
  // ============================================================
  
  /**
   * Add features "[...]-lemma ~ [lemma]" for all lemmas in the specified phrase.
   */
  protected void addLemmaFeatures(FeatureVector v, String domain, String name, String phrase, boolean addOnlyOpenPOS) {
    Set<String> lemmas = LingData.get(phrase).getTokens(true, addOnlyOpenPOS);
    for (String lemma : lemmas) {
      v.add(domain, name + "-lemma ~ " + lemma);
    }
  }
  protected void addLemmaFeatures(FeatureVector v, String domain, String name, String phrase) {
    addLemmaFeatures(v, domain, name, phrase, false);
  }

  /**
   * Add features "[...]-lemma ~ [lemma1]|[lemma2]" for all pairs of lemmas in the specified phrases.
   */
  protected void addLemmaFeatures(FeatureVector v, String domain, String name, String phrase1, String phrase2, boolean addOnlyOpenPOS) {
    Set<String> lemmas1 = LingData.get(phrase1).getTokens(true, addOnlyOpenPOS),
                lemmas2 = LingData.get(phrase1).getTokens(true, addOnlyOpenPOS);
    for (String lemma1 : lemmas1) {
      for (String lemma2 : lemmas2) {
        v.add(domain, name + "-lemma ~ " + lemma1 + "|" + lemma2);
        if (lemma1.equals(lemma2)) {
          v.add(domain, name + "-lemma SAME");
        }
      }
    }
  }
  protected void addLemmaFeatures(FeatureVector v, String domain, String name, String phrase1, String phrase2) {
    addLemmaFeatures(v, domain, name, phrase1, phrase2, false);
  }
  
  /**
   * Add features "[...]-cluster ~ [cluster_prefix]" for all tokens in the phrase.
   */
  protected void addClusterFeatures(FeatureVector v, String domain, String name, String phrase) {
    List<String> tokens = LingData.get(phrase).tokens;
    for (String token : tokens) {
      String cluster = BrownClusterTable.getCluster(token);
      if (cluster == null) continue;
      v.add(domain, name + "-cluster = " + cluster);
      for (String prefix : BrownClusterTable.getDefaultClusterPrefixes(cluster)) {
        v.add(domain, name + "-cluster ~ " + prefix);
      }
    }
  }
  
  /**
   * Add features "[...]-cluster ~ [cluster_prefix_1]|[cluster_prefix_2]" for all pairs of tokens in the specified phrases.
   */
  protected void addClusterFeatures(FeatureVector v, String domain, String name, String phrase1, String phrase2) {
    List<String> tokens1 = LingData.get(phrase1).tokens;
    List<String> tokens2 = LingData.get(phrase2).tokens;
    for (String token1 : tokens1) {
      String cluster1 = BrownClusterTable.getCluster(token1);
      if (cluster1 == null) continue;
      for (String token2 : tokens2) {
        String cluster2 = BrownClusterTable.getCluster(token2);
        if (cluster2 == null) continue;
        v.add(domain, name + "-cluster = " + cluster1 + "|" + cluster2);
        for (String prefix : BrownClusterTable.getDefaultClusterPrefixes(cluster1, cluster2)) {
          v.add(domain, name + "-cluster ~ " + prefix);
        }
      }
    }
  }
  
}
