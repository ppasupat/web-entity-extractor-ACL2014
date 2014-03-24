package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.candidate.PathEntry;
import edu.stanford.nlp.semparse.open.model.candidate.PathUtils;
import edu.stanford.nlp.semparse.open.model.candidate.TreePattern;

/**
 * Extract features by looking at the XPath
 */
public class FeatureTypePathBased extends FeatureType {

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
      
      for (int ancestorCount = 1; ancestorCount <= opts.maxAncestorCount; ancestorCount++) {
        if (ancestorCount <= path.size()) {
          candidate.features.add("path-tail", PathUtils.getXPathSuffixStringNoIndex(path, ancestorCount));
          PathEntry entry = path.get(path.size() - ancestorCount);
          String prefix;
          prefix = "(n-" + ancestorCount + ")-";
          candidate.features.add("path-tail", prefix + "tag = " + entry.tag);
          candidate.features.add("path-tail", prefix + "indexed = " + (entry.index != -1));
          candidate.features.add("path-tail", prefix + "tag-indexed = " + entry.tag + " " + (entry.index != -1));
          candidate.features.add("path-tail", "tail-tag = " + entry.tag);
          candidate.features.add("path-tail", "tail-indexed = " + (entry.index != -1));
          candidate.features.add("path-tail", "tail-tag-indexed = " + entry.tag + " " + (entry.index != -1));
        }
      }
    }
  }

}
