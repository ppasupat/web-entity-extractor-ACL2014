package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.*;

import edu.stanford.nlp.semparse.open.model.feature.FeatureExtractor;
import edu.stanford.nlp.semparse.open.model.tree.KNode;

public class TreePatternAndRange extends TreePattern {

  /**
   * The selected nodes are originalRecordNodes[i] where rangeStart <= i < rangeEnd
   */
  public final int amountCutStart, amountCutEnd, rangeStart, rangeEnd;

  public TreePatternAndRange(KNode rootNode, Collection<PathEntry> recordPath, Collection<KNode> originalRecordNodes,
      int amountCutStart, int amountCutEnd) {
    super(rootNode, recordPath, originalRecordNodes);
    if (amountCutStart < 0 || amountCutEnd < 0 || amountCutStart + amountCutEnd > originalRecordNodes.size())
      throw new IndexOutOfBoundsException("Invalid range: " +
          "cutStart = " + amountCutStart + ", cutEnd = " + amountCutEnd + ", n = " + originalRecordNodes.size());
    this.amountCutStart = amountCutStart;
    this.amountCutEnd = amountCutEnd;
    this.rangeStart = amountCutStart;
    this.rangeEnd = originalRecordNodes.size() - amountCutEnd;
  }
  
  public TreePatternAndRange(TreePattern treePattern, int amountCutStart, int amountCutEnd) {
    this(treePattern.rootNode, treePattern.recordPath, treePattern.recordNodes, amountCutStart, amountCutEnd);
  }
  
  @Override
  public String toString() {
    return new StringBuilder().append(PathUtils.getXPathString(recordPath))
        .append(" [").append(rangeStart).append(":").append(rangeEnd).append("]").toString();
  }
  
  @Override
  public List<KNode> getNodes() {
    return recordNodes.subList(rangeStart, rangeEnd);
  }

  /* It works, but is painfully slow. */
  @Deprecated
  public static List<Candidate> generateCutRangeCandidates(Candidate candidate) {
    List<Candidate> candidates = new ArrayList<>();
    // Remove a few stuff from both sides
    int n = candidate.numEntities();
    for (int i = 0; i < Math.min(5, n - CandidateGenerator.opts.minNumCandidateEntity); i++) {
      CandidateGroup group = new CandidateGroup(candidate.ex, candidate.group.selectedNodes.subList(1, n));
      candidates.add(group.addCandidate(new TreePatternAndRange(candidate.pattern, i, 0)));
    }
    for (int i = 1; i < Math.min(10, n - CandidateGenerator.opts.minNumCandidateEntity); i++) {
      CandidateGroup group = new CandidateGroup(candidate.ex, candidate.group.selectedNodes.subList(0, n-i));
      candidates.add(group.addCandidate(new TreePatternAndRange(candidate.pattern, 0, i)));
    }
    for (Candidate cutRangeCandidate : candidates) {
      FeatureExtractor.featureExtractor.extract(cutRangeCandidate);
      FeatureExtractor.featureExtractor.extract(cutRangeCandidate.group);
    }
    return candidates;
  }
  
  /**
   * Generate cut-range groups from the given group without actually adding any candidate.
   * Only suitable for experiments only.
   */
  public static List<CandidateGroup> generateDummyCutRangeCandidateGroups(CandidateGroup group) {
    List<CandidateGroup> candidateGroups = new ArrayList<>();
    // Remove a few stuff from both sides
    int n = group.numEntities();
    for (int i = 0; i < Math.min(5, n - CandidateGenerator.opts.minNumCandidateEntity); i++) {
      candidateGroups.add(new CandidateGroup(group.ex, group.selectedNodes.subList(1, n)));
    }
    for (int i = 1; i < Math.min(10, n - CandidateGenerator.opts.minNumCandidateEntity); i++) {
      candidateGroups.add(new CandidateGroup(group.ex, group.selectedNodes.subList(0, n-i)));
    }
    return candidateGroups;
  }
  
}
