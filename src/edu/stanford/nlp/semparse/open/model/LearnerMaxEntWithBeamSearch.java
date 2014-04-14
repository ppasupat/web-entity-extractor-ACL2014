package edu.stanford.nlp.semparse.open.model;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;

public class LearnerMaxEntWithBeamSearch extends LearnerMaxEnt {
  public static class Options {
    @Option public int beamSize = 500;
    @Option public int beamTrainStartIter = 1;
    @Option public String beamCandidateType = "cutrange";
  }
  public static Options opts = new Options();
  
  @Override
  protected List<Candidate> getCandidates(Example example) {
    if (trainIter <= opts.beamTrainStartIter) {
      return super.getCandidates(example);
    } else {
      return getBeamSearchedCandidates(example);
    }
  }
  
  protected List<Candidate> getBeamSearchedCandidates(Example example) {
    List<Pair<Candidate, Double>> rankedCandidates = super.getRankedCandidates(example);
    rankedCandidates = rankedCandidates.subList(0, Math.min(opts.beamSize, rankedCandidates.size()));
    List<Candidate> derivedCandidates = Lists.newArrayList();
    for (Pair<Candidate, Double> entry : rankedCandidates) {
      derivedCandidates.addAll(getDerivedCandidates(entry.getFirst()));
    }
    return derivedCandidates;
  }
  
  protected List<Candidate> getDerivedCandidates(Candidate original) {
    switch (opts.beamCandidateType) {
    case "cutrange":
      LogInfo.fails("... not implemented yet ...");
      //return TreePatternAndRange.generateCutRangeCandidates(original);
      return null;
    case "endcut":
      LogInfo.fails("... not implemented yet ...");
      return null;
    default:
      LogInfo.fails("Unrecognized beam candidate type: %s", opts.beamCandidateType);
      return null;
    }
  }
  
  @Override
  public List<Pair<Candidate, Double>> getRankedCandidates(Example example) {
    List<Pair<Candidate, Double>> answer = Lists.newArrayList();
    for (Candidate candidate : getBeamSearchedCandidates(example)) {
      double score = candidate.features.dotProduct(params);
      answer.add(new Pair<Candidate, Double>(candidate, score));
    }
    Collections.sort(answer, new Pair.ReverseSecondComparator<Candidate, Double>());
    return answer;
  }

}
