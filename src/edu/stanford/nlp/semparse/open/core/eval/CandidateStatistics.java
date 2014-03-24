package edu.stanford.nlp.semparse.open.core.eval;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.Pair;

public class CandidateStatistics {
  public final Candidate candidate;
  public final int rank, uniqueRank;    // rank and uniqueRank are 1-indexed
  public final double score;
  
  public CandidateStatistics(Candidate candidate, int rank, int uniqueRank, double score) {
    this.candidate = candidate;
    this.rank = rank;
    this.uniqueRank = uniqueRank;
    this.score = score;
  }
  
  /**
   * Convert Pair<Candidate, Double> to CandidateStatistics
   */
  public static List<CandidateStatistics> getRankedCandidateStats(List<Pair<Candidate, Double>> rankedCandidates) {
    List<CandidateStatistics> answer = Lists.newArrayList();
    Set<List<String>> foundPredictedEntities = Sets.newHashSet();
    for (int rank = 0; rank < rankedCandidates.size(); rank++) {
      Pair<Candidate, Double> entry = rankedCandidates.get(rank);
      Candidate candidate = entry.getFirst();
      foundPredictedEntities.add(candidate.predictedEntities);
      answer.add(new CandidateStatistics(candidate, rank + 1, foundPredictedEntities.size(), entry.getSecond()));
    }
    return answer;
  }
}
