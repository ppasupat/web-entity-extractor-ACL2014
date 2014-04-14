package edu.stanford.nlp.semparse.open.model.feature;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.ling.FrequencyTable;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import fig.basic.MapUtils;
import fig.basic.Option;

/**
 * Extract features by looking at the selected entities (strings).
 */
public class FeatureTypeNaiveEntityBased extends FeatureType {
  public static class Options {
    @Option public boolean useCountEntities = false;
    @Option public boolean addPhraseShapeFeature = true;
    @Option public boolean addCollapsedPhraseShapeFeature = false;
    @Option public boolean useDiscreteCountNumWords = true;
    @Option public boolean useMeanSDCountNumWords = false;
  }
  public static Options opts = new Options();
  
  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }
  
  @Override
  public void extract(CandidateGroup group) {
    extractEntityFeatures(group);
    extractDocumentFrequencyFeatures(group);
  }
  
  protected void  extractEntityFeatures(CandidateGroup group) {
    if (isAllowedDomain("entity")) {
      Multiset<String> countEntity = HashMultiset.create(),
                  countPhraseShape = HashMultiset.create(),
         countCollapsedPhraseShape = HashMultiset.create(),
                    countWordShape = HashMultiset.create();
      Multiset<Integer> countNumWord = HashMultiset.create();
      for (String entity : group.predictedEntities) {
        countEntity.add(entity);
        String wordForm = LingUtils.computePhraseShape(entity);
        countPhraseShape.add(wordForm);
        String[] wordForms = wordForm.split(" ");
        for (String word : wordForms) {
          countWordShape.add(word);
        }
        countNumWord.add(wordForms.length);
        countCollapsedPhraseShape.add(LingUtils.collapse(wordForms));
      }
      if (opts.useCountEntities)
        addQuantizedFeatures(group.features, "entity", "num-entities", group.predictedEntities.size());
      addEntropyFeatures(group.features, "entity", "entity", countEntity);
      addDuplicationFeatures(group.features, "entity", "entity", countEntity);
      if (opts.addPhraseShapeFeature)
        addVotingFeatures(group.features, "entity", "phrase-shape", countPhraseShape);
      if (opts.addCollapsedPhraseShapeFeature)
        addVotingFeatures(group.features, "entity", "collapsed-phrase-shape", countCollapsedPhraseShape);
      addVotingFeatures(group.features, "entity", "word-shape", countWordShape);
      if (opts.useDiscreteCountNumWords)
        addVotingFeatures(group.features, "entity", "num-word", countNumWord);
      if (opts.useMeanSDCountNumWords)
        addMeanDeviationFeatures(group.features, "entity", "num-word", countNumWord);
    }
  }
  
  protected static Set<String> BADWORDS = Sets.newHashSet(
      "com", "new", "about", "my", "home", "search",
      "information", "view", "page", "site", "click",
      "http", "contact", "www", "ord", "free", "now", "subscribe",
      "see", "service", "services", "online", "re", "data",
      "email", "top", "find", "system", "support", "comments",
      "policy", "last", "privacy", "post", "date", "time", "print");
  
  
  protected void extractDocumentFrequencyFeatures(CandidateGroup group) {
    if (isAllowedDomain("token-freq")) {
      // Oracle experiment: use a fixed set of words
      int numTokens = 0, numBadWords = 0, numDigits = 0;
      Map<Integer, Integer> numFrequentWords = Maps.newHashMap();
      for (String entity : group.predictedEntities) {
        for (String token : LingUtils.getAlphaOrNumericTokens(entity)) {
          numTokens++;
          for (Map.Entry<Integer, Set<String>> entry : FrequencyTable.topWordsLists.entrySet()) {
            if (entry.getValue().contains(token)) {
              MapUtils.incr(numFrequentWords, entry.getKey());
            }
          }
          if (BADWORDS.contains(token)) numBadWords++;
          if (token.matches("\\d+")) numDigits++;
        }
      }
      addPercentFeatures(group.features, "token-freq", "bad-words-ratio", numBadWords * 1.0 / numTokens);
      for (Map.Entry<Integer, Integer> entry : numFrequentWords.entrySet()) {
        addPercentFeatures(group.features, "token-freq", "frequent-" + entry.getKey(), entry.getValue() * 1.0 / numTokens);
      }
      addPercentFeatures(group.features, "token-freq", "digits-ratio", numDigits * 1.0 / numTokens);
    }
  }
}
