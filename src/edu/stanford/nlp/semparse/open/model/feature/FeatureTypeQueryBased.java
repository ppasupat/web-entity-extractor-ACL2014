package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.ClusterRepnUtils;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.ling.WordNetClusterTable;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.MemUsage;
import fig.basic.Option;
import fig.basic.T2DoubleMap;
import fig.basic.TDoubleMap;

public class FeatureTypeQueryBased extends FeatureType {
  public static class Options {
    @Option(gloss = "Use Freebase mapping")
    public boolean fbUseFreebaseMapping = false;
    
    @Option(gloss = "Freebase fires binary features")
    public boolean fbBinaryFeatures = false;
    
    @Option(gloss = "Prefixes for Query x Entity features")
    public List<Integer> qxePrefixes = Lists.newArrayList(0, 6);
    
    //@Option(gloss = "Feature types to add for Query x Entity (see source code for types)")
    //public Set<Integer> qxeFeatureTypes = Sets.newHashSet(0,1,2,3,4,5,6,7,8);
  }
  public static Options opts = new Options();

  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }

  @Override
  public void extract(CandidateGroup group) {
    extractFreebaseFeatures(group);
    extractQueryEntityFeatures(group);
  }
  
  // ============================================================
  // Freebase Features
  // ============================================================
  
  protected void extractFreebaseFeatures(CandidateGroup group) {
    if (isAllowedDomain("fb")) {
      // Look for head word or phrase.
      //String type = getHead(group.ex.phrase);
      String type = LingUtils.findHeadWord(group.ex.phrase, true).toLowerCase();
      String typeCluster = ClusterRepnUtils.getRepn(type);

      // Get a distribution over clusters
      //LogInfo.begin_track("%s", group.ex.phrase);
      TDoubleMap<String> clusterDist = new TDoubleMap<String>();
      for (String entity : group.predictedEntities) {
        String entityCluster = ClusterRepnUtils.getRepn(entity);

        if (!opts.fbUseFreebaseMapping) {
          // Use entity features (like cluster)
          String[] parts = entityCluster.split(" ");
          for (String part : parts) clusterDist.incr(part, 1.0 / parts.length);
          if (parts.length == 1)
            clusterDist.incr(entityCluster, 1.0 / group.predictedEntities.size());
          else if (parts.length == 2)
            clusterDist.incr(entityCluster, 1.0 / group.predictedEntities.size());
          else
            clusterDist.incr(parts[0] + "..." +  parts[parts.length-1], 1.0 / group.predictedEntities.size());
        } else {
          // Use the Freebase mapping
          initEntityTypeClusterMap();
          TDoubleMap<String> entityMap = entityTypeClusterMap.getMap(entityCluster, false);

          if (entityMap == null) {
            clusterDist.incr("UNSEEN", 1.0 / group.predictedEntities.size());
          } else {
            clusterDist.incrMap(entityMap, 1.0 / group.predictedEntities.size());
          }
        }
        //logs("%s (%s) | %s (%s) | %s", type, typeCluster, entity, entityCluster, entityMap != null);
      }

      // Add features
      for (TDoubleMap<String>.Entry e : clusterDist) {
        String predTypeCluster = e.getKey();
        double count = e.getValue();
        if (opts.fbBinaryFeatures) {
          group.features.add("fb", typeCluster + " ~ " + predTypeCluster);
          group.features.add("fb", predTypeCluster);
        } else {
          //if (count >= 0.01) logs("large %s (%s) : %s => %s", type, typeCluster, predTypeCluster, count);
          group.features.add("fb", typeCluster + " ~ " + predTypeCluster, count);
          group.features.add("fb", predTypeCluster, count);  // Back off
          //group.features.add("fb", type + " ~ " + predTypeCluster, count);
        }
      }
      //LogInfo.end_track();
    }
  }

  static T2DoubleMap<String, String> entityTypeClusterMap;

  void initEntityTypeClusterMap() {
    if (entityTypeClusterMap != null) return;
    entityTypeClusterMap = new T2DoubleMap<String, String>();
    String path = "/u/nlp/data/open-semparse/scr/freebase/types-entities-cluster2-counts.tsv";
    LogInfo.begin_track("Reading %s", path);
    for (String line : IOUtils.readLinesHard(path)) {
      // Format: <type cluster signature> <entity cluster signature> <count>
      String[] tokens = line.split("\t");
      String typeCluster = tokens[0];
      String entityCluster = tokens[1];
      double count = Double.parseDouble(tokens[2]);
      entityTypeClusterMap.put(entityCluster, typeCluster, count);
    }

    // Normalize to get probability distribution
    for(TDoubleMap<String> entityMap : entityTypeClusterMap.values()) {
      entityMap.incr("UNSEEN", 1);
      entityMap.multAll(1.0 / entityMap.sum());
    }

    LogInfo.logs("Read c-types for %d c-entities (%s)", entityTypeClusterMap.size(), MemUsage.getBytesStr(entityTypeClusterMap));
    LogInfo.end_track();
  }

  /*
  String getHead(String s) {
    String[] tokens = s.split(" ");
    // Strip out prepositional phrase and take the last word
    for (int i = 1; i < tokens.length; i++) {
      if (isPreposition(tokens[i])) return tokens[i-1];
      if (i == tokens.length-1 && tokens[i].matches("\\d+")) return tokens[i-1];  // end with year
    }
    return tokens[tokens.length-1];
  }

  List<String> prepositions = Arrays.asList("by in on at with from to that of".split(" "));
  boolean isPreposition(String s) { return prepositions.contains(s); }
  */
  
  // ============================================================
  // Query x Entity-Statistics Features
  // ============================================================
  
  protected void extractQueryEntityFeatures(CandidateGroup group) {
    if (isAllowedDomain("query-entity")) {
      // Counts!
      Multiset<String> countEntity = HashMultiset.create(),
                  countPhraseShape = HashMultiset.create(),
         countCollapsedPhraseShape = HashMultiset.create(),
                    countWordShape = HashMultiset.create(),
                      countWordPOS = HashMultiset.create(),
                    countEntityPOS = HashMultiset.create(),
           countEntityCollapsedPOS = HashMultiset.create(),
                      countWordNER = HashMultiset.create(),
                   countFirstToken = HashMultiset.create(),
                    countLastToken = HashMultiset.create(),
                     countAnyToken = HashMultiset.create();
      Multiset<Integer> countNumWord = HashMultiset.create();
      // Accumulate statistics
      for (String entity : group.predictedEntities) {
        countEntity.add(entity);
        String wordForm = LingUtils.computePhraseShape(entity);
        countPhraseShape.add(wordForm);
        String[] wordForms = wordForm.split(" ");
        for (String word : wordForms) countWordShape.add(word);
        countNumWord.add(wordForms.length);
        countCollapsedPhraseShape.add(LingUtils.collapse(wordForms));
        LingData lingData = LingData.get(entity);
        if (lingData.length > 0) {
          // POS
          for (String pos : lingData.posTags) countWordPOS.add(pos);
          countEntityPOS.add(LingUtils.join(lingData.posTags));
          countEntityCollapsedPOS.add(LingUtils.collapse(lingData.posTags));
          // NER
          for (String ner : lingData.nerTags) countWordNER.add(ner);
          // Tokens
          if (FeatureTypeLinguisticsBased.opts.lingLemmatizedTokens) {
            countFirstToken.add(lingData.lemmaTokens.get(0));
            countLastToken.add(lingData.lemmaTokens.get(lingData.length - 1));
            for (String token : lingData.lemmaTokens) countAnyToken.add(token);
          } else {
            countFirstToken.add(lingData.tokens.get(0));
            countLastToken.add(lingData.tokens.get(lingData.length - 1));
            for (String token : lingData.tokens) countAnyToken.add(token);
          }
        }
      }
      String headword = LingUtils.findHeadWord(group.ex.phrase, true).toLowerCase();
      List<String> prefixes = Lists.newArrayList();
      for (int x : opts.qxePrefixes) {
        if (x == 0) prefixes.add("head-word = " + headword + " | ");
        else if (x > 0) prefixes.add("head-word ~ " + BrownClusterTable.getClusterPrefix(headword, x) + " | ");
        else if (x == -2) prefixes.add("head-word ~ " + WordNetClusterTable.getCluster(headword) + " | ");
      }
      for (String prefix : prefixes) {
        addEntropyFeatures(group.features, "query-entity", prefix + "entity", countEntity);
        addDuplicationFeatures(group.features, "query-entity", prefix + "entity", countEntity);
        addVotingFeatures(group.features, "query-entity", prefix + "phrase-shape", countPhraseShape);
        addVotingFeatures(group.features, "query-entity", prefix + "word-shape", countWordShape);
        addVotingFeatures(group.features, "query-entity", prefix + "num-word", countNumWord);
        addMeanDeviationFeatures(group.features, "query-entity", prefix + "num-word", countNumWord);
        addVotingFeatures(group.features, "query-entity", prefix + "word-pos", countWordPOS);
        addVotingFeatures(group.features, "query-entity", prefix + "entity-pos", countEntityPOS);
        addVotingFeatures(group.features, "query-entity", prefix + "entity-collapsed-pos", countEntityCollapsedPOS);
        addVotingFeatures(group.features, "query-entity", prefix + "word-ner",  countWordNER);
        addVotingFeatures(group.features, "query-entity", prefix + "first-token", countFirstToken);
        addVotingFeatures(group.features, "query-entity", prefix + "last-token", countLastToken);
        addVotingFeatures(group.features, "query-entity", prefix + "any-token", countAnyToken);
      }
      /*
      for (String prefix : prefixes) {
        if (opts.qxeFeatureTypes.contains(0)) {
          addEntropyFeatures(group.features, "query-entity", prefix + "entity", countEntity);
          addDuplicationFeatures(group.features, "query-entity", prefix + "entity", countEntity);
        }
        if (opts.qxeFeatureTypes.contains(1))
          addVotingFeatures(group.features, "query-entity", prefix + "phrase-shape", countPhraseShape);
        if (opts.qxeFeatureTypes.contains(2))
          addVotingFeatures(group.features, "query-entity", prefix + "word-shape", countWordShape);
        if (opts.qxeFeatureTypes.contains(3))
          addVotingFeatures(group.features, "query-entity", prefix + "num-word", countNumWord);
        if (opts.qxeFeatureTypes.contains(4))
          addMeanDeviationFeatures(group.features, "query-entity", prefix + "num-word", countNumWord);
        if (opts.qxeFeatureTypes.contains(5))
          addVotingFeatures(group.features, "query-entity", prefix + "word-pos", countWordPOS);
        if (opts.qxeFeatureTypes.contains(6))
          addVotingFeatures(group.features, "query-entity", prefix + "entity-pos", countEntityPOS);
        if (opts.qxeFeatureTypes.contains(7))
          addVotingFeatures(group.features, "query-entity", prefix + "entity-collapsed-pos", countEntityCollapsedPOS);
        if (opts.qxeFeatureTypes.contains(8)) {
          addEntropyFeatures(group.features, "query-entity", prefix + "first-token", countFirstToken);
          addEntropyFeatures(group.features, "query-entity", prefix + "last-token", countLastToken);
        }
      }
      */
      /*
      if (opts.qxePrefixes.contains(-1)) {
        double[] headwordVector = WordVectorTable.getVector(headword);
        if (headwordVector != null) {
          FeatureVector v = new FeatureVector();
          addEntropyFeatures(v, "query-entity", "entity", countEntity);
          addDuplicationFeatures(v, "query-entity", "entity", countEntity);
          addVotingFeatures(v, "query-entity", "phrase-shape", countPhraseShape);
          addVotingFeatures(v, "query-entity", "word-shape", countWordShape);
          addVotingFeatures(v, "query-entity", "num-word", countNumWord);
          addMeanDeviationFeatures(v, "query-entity", "num-word", countNumWord);
          addVotingFeatures(v, "query-entity", "word-pos", countWordPOS);
          addVotingFeatures(v, "query-entity", "entity-pos", countEntityPOS);
          addVotingFeatures(v, "query-entity", "entity-collapsed-pos", countEntityCollapsedPOS);
          addEntropyFeatures(v, "query-entity", "first-token", countFirstToken);
          addEntropyFeatures(v, "query-entity", "last-token", countLastToken);
          Map<String, Double> map = v.toMap();
          for (int i = 0; i < headwordVector.length; i++) {
            for (Map.Entry<String, Double> entry : map.entrySet()) {
              group.features.add("query-entity", "[" + i + "]-" + entry.getKey(), entry.getValue() * headwordVector[i]);
            }
          }
        }
      }
      */
    }
  }
}
