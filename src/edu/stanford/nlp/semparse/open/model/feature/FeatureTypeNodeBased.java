package edu.stanford.nlp.semparse.open.model.feature;

import java.util.*;

import edu.stanford.nlp.semparse.open.model.FeatureVector;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import edu.stanford.nlp.semparse.open.util.Multiset;
import fig.basic.Option;

/**
 * Extract features by looking at the selected nodes in the knowledge tree.
 */
public class FeatureTypeNodeBased extends FeatureType {
  public static class Options {
    @Option public boolean rangeUseCollapsedTimestamp = true;
    @Option public boolean soaUseIndexedFeatures = false;
    @Option public boolean soaUseNoIndexFeatures = true;
    @Option public boolean soaUseIdClassFeatures = true;
    @Option public boolean soaAverage = false;
  }
  public static Options opts = new Options();
  
  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }

  @Override
  public void extract(CandidateGroup group) {
    extractSelfOrAncestorsFeatures(group);
    extractNodeRangeFeatures(group);
  }
  
  public void extractSelfOrAncestorsFeatures(CandidateGroup group) {
    if (isAllowedDomain("self-or-ancestors")) {
      FeatureVector v = new FeatureVector();
      // Majority id / class / number of children of the nodes and parents
      Set<KNode> currentKNodes = new HashSet<>(group.selectedNodes);
      for (int ancestorCount = 0; ancestorCount < FeatureType.opts.maxAncestorCount; ancestorCount++) {
        Multiset<String> countTag = new Multiset<>(),
                          countId = new Multiset<>(),
                       countClass = new Multiset<>(),
                 countNumChildren = new Multiset<>();
        Multiset<Integer> countChildIndex = new Multiset<>();
        Set<KNode> parents = new HashSet<>();
        for (KNode node : currentKNodes) {
          // Properties of the current node
          countTag.add(node.value);
          String nodeId = node.getAttribute("id");
          if (!nodeId.isEmpty())
            countId.add(nodeId);
          String nodeClass = node.getAttribute("class");
          if (!nodeClass.isEmpty())
            countClass.add(nodeClass);
          // Properties relating to children
          List<KNode> children = node.getChildren();
          int numChildren = children.size();
          countNumChildren.add((numChildren <= 3) ? "" + numChildren : "many");
          // Traverse up to parent
          if (node.parent != null) {
            countChildIndex.add(node.getChildIndex());
            parents.add(node.parent);
          }
        }
        if (parents.isEmpty()) break;
        // Count how many children the parents have
        int countChildrenOfParents = 0;
        for (KNode parent : parents) {
          countChildrenOfParents += parent.countChildren();
        }
        double percentChildrenOfParents = currentKNodes.size() * 1.0 / countChildrenOfParents;
        String domain = "self-or-ancestors";
        // With indexed prefix
        if (opts.soaUseIndexedFeatures) {
          String prefix = "(n-" + ancestorCount + ")-";
          addVotingFeatures(v, domain, prefix + "tag", countTag);
          if (opts.soaUseIdClassFeatures) {
            addVotingFeatures(v, domain, prefix + "id", countId, true);
            addVotingFeatures(v, domain, prefix + "class", countClass, true);
          }
          addVotingFeatures(v, domain, prefix + "num-children", countNumChildren);
          addVotingFeatures(v, domain, prefix + "child-index", countChildIndex, false, false);
          addPercentFeatures(v, domain, prefix + "children-of-parent", percentChildrenOfParents);
          if (parents.size() == 1) v.add(domain, prefix + "same-parent");
        }
        // Without indexed prefix
        if (opts.soaUseNoIndexFeatures) {
          addVotingFeatures(v, domain, "tag", countTag);
          if (opts.soaUseIdClassFeatures) {
            addVotingFeatures(v, domain, "id", countId, true);
            addVotingFeatures(v, domain, "class", countClass, true);
          }
          addVotingFeatures(v, domain, "num-children", countNumChildren);
          addVotingFeatures(v, domain, "child-index", countChildIndex, false, false);
          addPercentFeatures(v, domain, "children-of-parent", percentChildrenOfParents);
          if (parents.size() == 1) v.add(domain, "same-parent");
        }
        // Traverse up the tree
        currentKNodes = parents;
      }
      // Add features
      if (opts.soaAverage) {
        for (Map.Entry<String, Double> entry : v.toMap().entrySet()) {
          group.features.addFromString(entry.getKey(), entry.getValue() / FeatureType.opts.maxAncestorCount);
        }
      } else {
        group.features.add(v);
      }
    }
  }
   
  public void extractNodeRangeFeatures(CandidateGroup group) {
    if (isAllowedDomain("node-range")) {
      List<KNode> selectedKNodes = group.selectedNodes;
      // Find root
      KNode root = selectedKNodes.get(0);
      while (root.timestampIn != 1) {
        root = root.parent;
      }
      int rangeMinTimestamp, rangeMaxTimestamp, pageMaxTimestamp;
      if (opts.rangeUseCollapsedTimestamp) {
        // Use only timestampIn (collapsed) 
        rangeMinTimestamp = selectedKNodes.get(0).timestampInCollapsed;
        rangeMaxTimestamp = selectedKNodes.get(selectedKNodes.size() - 1).timestampInCollapsed;
        pageMaxTimestamp = root.timestampOut / 2;
      } else {
        // Use the original timestamps
        rangeMinTimestamp = selectedKNodes.get(0).timestampIn;
        rangeMaxTimestamp = selectedKNodes.get(selectedKNodes.size() - 1).timestampOut;
        pageMaxTimestamp = root.timestampOut;
      }
      addPercentFeatures(group.features, "node-range", "length",
          (rangeMaxTimestamp - rangeMinTimestamp) * 1.0 / pageMaxTimestamp);
      addPercentFeatures(group.features, "node-range", "start",
          (rangeMinTimestamp) * 1.0 / pageMaxTimestamp);
      addPercentFeatures(group.features, "node-range", "end",
          (rangeMaxTimestamp) * 1.0 / pageMaxTimestamp);
    }
  }

}
