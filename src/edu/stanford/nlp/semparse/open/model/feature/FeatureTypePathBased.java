package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.candidate.PathEntry;
import edu.stanford.nlp.semparse.open.model.candidate.PathUtils;
import edu.stanford.nlp.semparse.open.model.candidate.TreePattern;
import fig.basic.Option;

/**
 * Extract features by looking at the XPath
 */
public class FeatureTypePathBased extends FeatureType {
  public static class Options {
    @Option public boolean pathFeatureUsePrefix = true;
    @Option public int pathMaxAncestorCount = 0;
    @Option public boolean pathUsePathSuffix = true;
  }
  public static Options opts = new Options();

  @Override
  public void extract(Candidate candidate) {
    extractPathTailFeatures(candidate);
  }
  
  @Override
  public void extract(CandidateGroup group) {
    // Do nothing
  }
  
  protected void extractPathTailFeatures(Candidate candidate) {
    if (isAllowedDomain("path-tail")) {
      TreePattern pattern = candidate.pattern;
      List<PathEntry> path = pattern.getPath();
      int maxCount = opts.pathMaxAncestorCount > 0 ? opts.pathMaxAncestorCount : FeatureType.opts.maxAncestorCount;
      for (int ancestorCount = 1; ancestorCount <= maxCount; ancestorCount++) {
        if (ancestorCount <= path.size()) {
          if (opts.pathUsePathSuffix)
            candidate.features.add("path-tail", PathUtils.getXPathSuffixStringNoIndex(path, ancestorCount));
          PathEntry entry = path.get(path.size() - ancestorCount);
          if (opts.pathFeatureUsePrefix) {
            String prefix = "(n-" + ancestorCount + ")-";
            candidate.features.add("path-tail", prefix + "tag = " + entry.tag);
            candidate.features.add("path-tail", prefix + "indexed = " + (entry.index != -1));
            candidate.features.add("path-tail", prefix + "tag-indexed = " + entry.tag + " " + (entry.index != -1));
          } else {
            candidate.features.add("path-tail", "tag = " + entry.tag);
            candidate.features.add("path-tail", "indexed = " + (entry.index != -1));
            candidate.features.add("path-tail", "tag-indexed = " + entry.tag + " " + (entry.index != -1));
          }
        }
      }
    }
  }

}
