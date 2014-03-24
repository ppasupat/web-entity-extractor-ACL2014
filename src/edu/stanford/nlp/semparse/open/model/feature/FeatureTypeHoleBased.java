package edu.stanford.nlp.semparse.open.model.feature;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.ling.BrownClusterTable;
import edu.stanford.nlp.semparse.open.ling.LingData;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import edu.stanford.nlp.semparse.open.model.candidate.CandidateGroup;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Fire features based on the existence of holes in the selected nodes (or ancestors).
 *
 * This won't work for wildcard PathEntry.
 */
public class FeatureTypeHoleBased extends FeatureType {
  public static class Options {
    @Option public boolean holeUseTag = true;
    @Option public List<Integer> headerPrefixes = Lists.newArrayList();
    @Option public boolean headerBinary = false;
  }
  public static Options opts = new Options();

  @Override
  public void extract(Candidate candidate) {
    // Do nothing
  }

  @Override
  public void extract(CandidateGroup group) {
    extractHoleBasedFeatures(group);
  }
  
  protected void extractHoleBasedFeatures(CandidateGroup group) {
    if (isAllowedDomain("hole") || isAllowedDomain("header")) {
      List<KNode> currentKNodes = group.selectedNodes;
      List<Integer> singleAnyIndexStack = Lists.newArrayList();
      Multiset<String> headers = HashMultiset.create();
      for (int ancestorCount = 0; ancestorCount < FeatureType.opts.maxAncestorCount; ancestorCount++) {
        // Group the nodes based on parents
        List<KNode> parents = Lists.newArrayList();
        Map<KNode, List<KNode>> parentToCurrent = Maps.newHashMap();
        for (KNode node : currentKNodes) {
          List<KNode> siblings = parentToCurrent.get(node.parent);
          if (siblings == null) {
            siblings = Lists.newArrayList();
            parentToCurrent.put(node.parent, siblings);
            parents.add(node.parent);
          }
          siblings.add(node);
        }
        // Investigate each parent
        // Assume that the tags in each level are identical
        // Assume further that the nodes are listed in order
        // TODO Add support for wildcards
        String parentTag = parents.get(0).value, currentTag = currentKNodes.get(0).value;
        boolean anyHoleTop = false, anyHoleMiddle = false, anyHoleBottom = false;
        boolean tagHoleTop = false, tagHoleMiddle = false, tagHoleBottom = false;
        boolean anyAll = false, tagAll = false, single = false;
        Set<Integer> anyIndices = Sets.newHashSet();
        for (KNode parent : parents) {
          List<KNode> siblings = parentToCurrent.get(parent);
          List<KNode> anyChildren = parent.getChildren(), tagChildren = parent.getChildrenOfTag(currentTag);
          // Holes
          int anyTopIndex = anyChildren.indexOf(siblings.get(0)),
              anyBottomIndex = anyChildren.indexOf(siblings.get(siblings.size() - 1)),
              tagTopIndex = tagChildren.indexOf(siblings.get(0)),
              tagBottomIndex = tagChildren.indexOf(siblings.get(siblings.size() - 1));
          if (anyTopIndex == -1 || anyBottomIndex == -1 || tagTopIndex == -1 || tagBottomIndex == -1)
            LogInfo.fails("WTF? %s %s %s %s %d", group.ex, group.predictedEntities, parentTag, currentTag, ancestorCount);
          if (anyTopIndex != 0) anyHoleTop = true;
          if (anyBottomIndex != anyChildren.size() - 1) anyHoleBottom = true;
          if (anyBottomIndex - anyTopIndex + 1 != siblings.size()) anyHoleMiddle = true;
          if (tagTopIndex != 0) tagHoleTop = true;
          if (tagBottomIndex != tagChildren.size() - 1) tagHoleBottom = true;
          if (tagBottomIndex - tagTopIndex + 1 != siblings.size()) tagHoleMiddle = true;
          // Single & All
          if (siblings.size() == 1) {
            single = true;
            anyIndices.add(anyTopIndex);
          } else {
            if (anyChildren.size() == siblings.size()) anyAll = true;
            if (tagChildren.size() == siblings.size()) tagAll = true;
            anyIndices.add(-1);     // Hack to remove anyIndices
          }
        }
        // Fire features
        if (isAllowedDomain("hole")) {
          String prefix = opts.holeUseTag ? (parentTag + "/" + currentTag + "-") : "";
          if (anyAll) group.features.add("hole", prefix + "any-all");
          if (tagAll) group.features.add("hole", prefix + "tag-all");
          if (single) group.features.add("hole", prefix + "single");
          if (anyHoleTop) group.features.add("hole", prefix + "any-hole-top");
          if (anyHoleMiddle) group.features.add("hole", prefix + "any-hole-middle");
          if (anyHoleBottom) group.features.add("hole", prefix + "any-hole-bottom");
          if (tagHoleTop) group.features.add("hole", prefix + "tag-hole-top");
          if (tagHoleMiddle) group.features.add("hole", prefix + "tag-hole-middle");
          if (tagHoleBottom) group.features.add("hole", prefix + "tag-hole-bottom");
        }
        // AnyIndex
        if (anyIndices.size() == 1) {
          int index = anyIndices.toArray(new Integer[1])[0];
          singleAnyIndexStack.add(index);
        } else {
          singleAnyIndexStack.add(-1);
        }
        // Header -- the parallel element at the top hole
        if (isAllowedDomain("header") && anyHoleTop) {
          List<Integer> indices = Lists.newArrayList(singleAnyIndexStack);
          indices.set(indices.size() - 1, 0);       // Choose the top hole
          // Get the parallel element
          KNode node = parents.get(0);
          for (int i = indices.size() - 1; i >= 0; i--) {
            try {
              node = node.getChildren().get(indices.get(i));
            } catch (IndexOutOfBoundsException e) {
              break;
            }
          }
          if (node.fullText != null && !node.fullText.isEmpty()) {
            String header = node.fullText;
            headers.add(header);
          }
        }
        // Go to the next level
        if (parents.isEmpty() || parentTag.equals("html")) break;
        currentKNodes = parents;
      }
      // Header
      if (isAllowedDomain("header")) {
        String headword = LingUtils.findHeadWord(group.ex.phrase).toLowerCase();
        for (Multiset.Entry<String> entry : headers.entrySet()) {
          String header = entry.getElement();
          //LogInfo.logs("%s %s", header, group.sampleEntities());
          LingData headerLingData = LingData.get(header);
          int n = opts.headerBinary ? 1 : entry.getCount();
          for (int i = 0; i < n; i++) {
            for (int j = 0; j < headerLingData.length; j++) {
              String token = headerLingData.tokens.get(j);
              String lemmaToken = headerLingData.lemmaTokens.get(j).toLowerCase();
              for (int prefix : opts.headerPrefixes) {
                if (prefix == 0) {
                  if (lemmaToken.matches("[a-z]+"))
                    group.features.add("header", "header ~ " + lemmaToken);
                } else {
                  String cluster = BrownClusterTable.getClusterPrefix(token, prefix);
                  if (cluster != null)
                    group.features.add("header", "header ~ " + cluster);
                }
              }
            }
            if (headerLingData.lemmaTokens.contains(headword)) {
              group.features.add("header", "match-headword");
            }
          }
        }
      }
    }
  }

}
