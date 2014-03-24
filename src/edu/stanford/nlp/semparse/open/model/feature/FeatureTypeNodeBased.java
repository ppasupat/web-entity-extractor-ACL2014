package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import fig.basic.Option;

/**
 * Extract features by looking at the selected nodes in the knowledge tree.
 */
public class FeatureTypeNodeBased extends FeatureType {
  public static class Options {
    @Option public boolean rangeUseCollapsedTimestamp = true;
    @Option public boolean soaUseIndexedFeatures = false;
    @Option public boolean soaUseNoIndexFeatures = true;
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
      // Majority id / class / number of children of the nodes and parents
      Set<KNode> currentKNodes = Sets.newHashSet(group.selectedNodes);
      for (int ancestorCount = 0; ancestorCount < FeatureType.opts.maxAncestorCount; ancestorCount++) {
        Multiset<String> countTag = HashMultiset.create(),
                          countId = HashMultiset.create(),
                       countClass = HashMultiset.create(),
                 countNumChildren = HashMultiset.create();
        Multiset<Integer> countChildIndex = HashMultiset.create();
        Set<KNode> parents = Sets.newHashSet();
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
          addVotingFeatures(group.features, domain, prefix + "tag", countTag);
          addVotingFeatures(group.features, domain, prefix + "id", countId, true);
          addVotingFeatures(group.features, domain, prefix + "class", countClass, true);
          addVotingFeatures(group.features, domain, prefix + "num-children", countNumChildren);
          addVotingFeatures(group.features, domain, prefix + "child-index", countChildIndex, false, false);
          addPercentFeatures(group.features, domain, prefix + "children-of-parent", percentChildrenOfParents);
          if (parents.size() == 1) group.features.add(domain, prefix + "same-parent");
        }
        // Without indexed prefix
        if (opts.soaUseNoIndexFeatures) {
          addVotingFeatures(group.features, domain, "tag", countTag);
          addVotingFeatures(group.features, domain, "id", countId, true);
          addVotingFeatures(group.features, domain, "class", countClass, true);
          addVotingFeatures(group.features, domain, "num-children", countNumChildren);
          addVotingFeatures(group.features, domain, "child-index", countChildIndex, false, false);
          addPercentFeatures(group.features, domain, "children-of-parent", percentChildrenOfParents);
          if (parents.size() == 1) group.features.add(domain, "same-parent");
        }
        // Traverse up the tree
        currentKNodes = parents;
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
