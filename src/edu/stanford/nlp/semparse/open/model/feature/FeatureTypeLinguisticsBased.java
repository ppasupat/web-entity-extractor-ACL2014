package edu.stanford.nlp.semparse.open.model.feature;

import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import fig.basic.Option;

/**
 * Extract features by looking at linguistic properties
 */
public class FeatureTypeLinguisticsBased extends FeatureType {
  public static class Options {
    @Option public boolean lingLemmatizedTokens = false;
    @Option public boolean lingWordPOS = false; 
    @Option public boolean lingAltWordPOS = true;
    @Option public boolean lingBinWordPOS = true;
    @Option public boolean lingEntityPOS = false;
    @Option public boolean lingCollapsedPOS = true;
  }
  public static Options opts = new Options();

  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }
  
  @Override
  public void extract(CandidateGroup group) {
    extractLingFeatures(group);
    extractClusterFeatures(group);
    //extractFakeWordVectorFeatures(group);
  }
  
  protected void extractLingFeatures(CandidateGroup group) {
    if (isAllowedDomain("ling")) {
      // Counts!
      Multiset<String> countWordPOS = HashMultiset.create(),
                     countEntityPOS = HashMultiset.create(),
            countEntityCollapsedPOS = HashMultiset.create(),
                    countFirstToken = HashMultiset.create(),
                     countLastToken = HashMultiset.create();
      for (String entity : group.predictedEntities) {
        LingData lingData = LingData.get(entity);
        if (lingData.length > 0) {
          for (String pos : lingData.posTags) countWordPOS.add(pos);
          // POS
          countEntityPOS.add(LingUtils.join(lingData.posTags));
          countEntityCollapsedPOS.add(LingUtils.collapse(lingData.posTags));
          // Tokens
          if (opts.lingLemmatizedTokens) {
            countFirstToken.add(lingData.lemmaTokens.get(0));
            countLastToken.add(lingData.lemmaTokens.get(lingData.length - 1));
          } else {
            countFirstToken.add(lingData.tokens.get(0));
            countLastToken.add(lingData.tokens.get(lingData.length - 1));
          }
        }
      }
      if (opts.lingWordPOS)
        addVotingFeatures(group.features, "ling", "word-pos", countWordPOS);
      if (opts.lingAltWordPOS) {
        addEntropyFeatures(group.features, "ling", "word-pos", countWordPOS);
        for (String pos : countWordPOS.elementSet()) {
          if (opts.lingBinWordPOS) {
            addPercentFeatures(group.features, "ling", "word-pos = " + pos,
                countWordPOS.count(pos) * 1.0 / countWordPOS.size());
          } else {
            group.features.add("ling", "word-pos = " + pos);
          }
        }
      }
      if (opts.lingEntityPOS)
        addVotingFeatures(group.features, "ling", "entity-pos", countEntityPOS);
      if (opts.lingCollapsedPOS)
        addVotingFeatures(group.features, "ling", "entity-collapsed-pos", countEntityCollapsedPOS);
      addEntropyFeatures(group.features, "ling", "first-token", countFirstToken);
      addEntropyFeatures(group.features, "ling", "last-token", countLastToken);
    }
  }
  
  protected void extractClusterFeatures(CandidateGroup group) {
    if (isAllowedDomain("cluster")) {
      // Query cluster prefixes 
      Set<String> queryClusters = Sets.newHashSet(),
                 entityPrefixes = Sets.newHashSet();
      for (String token : LingData.get(group.ex.phrase).getTokens(true, true)) {
        queryClusters.addAll(BrownClusterTable.getDefaultClusterPrefixesFromWord(token));
        queryClusters.add(token);  // Also add the raw token
      }
      // Entity cluster
      Multiset<String> entityTokenClusters = HashMultiset.create();
      for (String entity : group.predictedEntities) {
        for (String token : LingData.get(entity).tokens) {
          String cluster = BrownClusterTable.getCluster(token);
          if (cluster != null) {
            entityTokenClusters.add(cluster);
            for (String prefix : BrownClusterTable.getDefaultClusterPrefixes(cluster))
              entityPrefixes.add(prefix);
          }
        }
      }
      // Add features
      for (String queryCluster : queryClusters) {
        for (String prefix : entityPrefixes) {
          group.features.add("cluster", "query = " + queryCluster + " | entity ~ " + prefix);
        }
        // Entity Entropy
        double normalizedEntropy = getNormalizedEntropy(entityTokenClusters);
        group.features.add("cluster", "query = " + queryCluster + " | entity-normalized-entropy", normalizedEntropy);
      }
    }
  }
  
  /** Use to debug the advanced word vector. Basically, this is the slower version. **/
  protected void extractFakeWordVectorFeatures(CandidateGroup group) {
    if (isAllowedDomain("fake-wordvec")) {
      group.ex.initAveragedWordVector();
      group.initAveragedWordVector();
      double[] x = group.ex.averagedWordVector.averaged;
      double[] y = group.averagedWordVector.averaged;
      if (x == null || y == null) return;
      for (int i = 0; i < x.length; i++) {
        for (int j = 0; j < y.length; j++) {
          group.features.add("fake-wordvec", "[" + i + "][" + j + "]", x[i] * y[j]);
        }
      }
    }
  }
  
}
