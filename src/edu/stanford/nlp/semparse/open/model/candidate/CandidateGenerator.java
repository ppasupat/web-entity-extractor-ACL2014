package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.feature.FeatureExtractor;
import edu.stanford.nlp.semparse.open.model.tree.KNode;
import fig.basic.LogInfo;
import fig.basic.Option;

public class CandidateGenerator {
  public static class Options {
    @Option(gloss = "Maximum number of XPath entries to toggle the indices")
    public int maxTweakDepth = 8;
    
    @Option(gloss = "Minimum number of selected entities to be considered a valid candidate")
    public int minNumCandidateEntity = 2;
    
    @Option(gloss = "Use the advanced tree traverser")
    public boolean useAdvancedTreeTraverser = false;
    
    @Option(gloss = "Maximum number of XPath entries that can be toggled to wildcard")
    public int allowWildcards = 0;
    
    @Option(gloss = "Maximum number of XPath entries that can be end-cut")
    public int allowEndCuts = 0;
    
    @Option(gloss = "Maximum depth of XPath entries that can be advancedly tweaked")
    public int maxAdvancedTweakDepth = 4;
  }
  public static Options opts = new Options();
  
  public final List<String> BLACKLISTED_TAGS = Lists.newArrayList(
    "html", "head", "body", "script", "noscript", "link", "style"
  );
  
  public void process(Example ex) {
    if (ex.candidates != null) {
      LogInfo.warnings("Example %s already has a candidate list", ex);
      return;
    }
    LogInfo.begin_track("Extracting candidates ...");
    ex.candidateGroups = Lists.newArrayList();
    ex.candidates = Lists.newArrayList();
    new CandidatePopulator(ex).populateCandidates();
    LogInfo.logs("Found %d candidates (%d groups)", ex.candidates.size(), ex.candidateGroups.size());
    LogInfo.end_track();
    LogInfo.begin_track("Extracting features ...");
    for (CandidateGroup group : ex.candidateGroups)
      FeatureExtractor.featureExtractor.extract(group);
    for (Candidate candidate : ex.candidates)
      FeatureExtractor.featureExtractor.extract(candidate);
    LogInfo.end_track();
  }
  
  // ============================================================
  // Find candidates
  // ============================================================
  
  class CandidatePopulator {
    Example ex;
    
    public CandidatePopulator(Example ex) {
      this.ex = ex;
    }

    void populateCandidates() {
      populateCandidates(ex.tree);
    }
    
    private void populateCandidates(KNode rootNode) {
      // Only start from the top <html> tags
      if (rootNode.type != KNode.Type.TAG || !rootNode.value.equals("html")) {
        for (KNode child : rootNode.getChildren()) {
          populateCandidates(child);
        }
        return;
      }
      // Traverse the knowledge tree and collect all possible paths
      TreeTraverser traverser = opts.useAdvancedTreeTraverser ? new AdvancedTreeTraverser(rootNode)
          : new BasicTreeTraverser(rootNode);
      Map<List<KNode>, CandidateGroup> nodesToCandidateGroup = Maps.newHashMap();
      for (ImmutableList<PathEntry> path : traverser.getFoundPaths()) {
        // Execute the path and check if the path is valid.
        List<KNode> nodes = Lists.newArrayList();
        PathUtils.executePath(path, rootNode, nodes);
        if (nodes.size() > opts.minNumCandidateEntity) {
          CandidateGroup group = nodesToCandidateGroup.get(nodes);
          if (group == null) {
            ex.candidateGroups.add(group = new CandidateGroup(ex, nodes));
            nodesToCandidateGroup.put(nodes, group);
          }
          ex.candidates.add(group.addCandidate(new TreePattern(rootNode, path, group.selectedNodes)));
        }
      }
    }
  }
  
  interface TreeTraverser {
    public Collection<ImmutableList<PathEntry>> getFoundPaths();
  }

  class BasicTreeTraverser implements TreeTraverser {
    List<PathEntry> ancestors;
    Set<ImmutableList<PathEntry>> foundPaths;
    
    public BasicTreeTraverser(KNode rootNode) {
      ancestors = Lists.newArrayList();
      foundPaths = Sets.newHashSet();
      traverseTree(rootNode);
    }
    
    private void traverseTree(KNode currentNode) {
      if (currentNode.parent.countChildren(currentNode.value) > 1)
        ancestors.add(new PathEntry(currentNode.value, currentNode.getChildIndexOfSameTag()));
      else
        ancestors.add(new PathEntry(currentNode.value));
      // Process current node
      if (!isBlacklisted(currentNode))
        tweakPaths(1);
      // Traverse children
      for (KNode child : currentNode.getChildren()) {
        if (child.type == KNode.Type.TAG) traverseTree(child);
      }
      ancestors.remove(ancestors.size() - 1);
    }
    
    private boolean isBlacklisted(KNode node) {
      if (node.fullText == null || node.fullText.isEmpty()) return true;
      if (BLACKLISTED_TAGS.contains(node.value)) return true;
      return false;
    }
    
    /**
     * Toggle the indices of the xpath entries.
     * For example, /html/body/div[3]/a[1] will produce
     * - /html/body/div[3]/a[1]
     * - /html/body/div[3]/a
     * - /html/body/div/a[1]
     * - /html/body/div/a
     * 
     * Implemented using recursion on depth:
     * The toggled xpath entry is xpath[xpath.length - depth]
     * (depth = 1, 2, ..., opts.maxTweakDepth)
     * 
     * Exception: the first entry (html) will not be toggled.
     */
    private void tweakPaths(int depth) {
      if (depth > opts.maxTweakDepth || depth >= ancestors.size()) {
        foundPaths.add(ImmutableList.copyOf(ancestors));
        return;
      }
      tweakPaths(depth + 1);
      PathEntry swap = ancestors.get(ancestors.size() - depth);
      if (swap.index != -1) {
        ancestors.set(ancestors.size() - depth, swap.getNoIndexVersion());
        tweakPaths(depth + 1);
        ancestors.set(ancestors.size() - depth, swap);
      }
    }
    
    @Override
    public Collection<ImmutableList<PathEntry>> getFoundPaths() {
      return foundPaths;
    }
  }
  
  // ============================================================
  // Advanced Tree Traverser
  // ============================================================
  
  class PathEntryAugmented {
    public final String tag;
    public final int childIndex, childIndexOfTag;     // 0-indexed
    public final int numSiblings, numSiblingsOfTag;   // including self too
    
    public PathEntryAugmented(KNode node) {
      this.tag = node.value;
      int countChildIndex = -1, countChildIndexOfTag = -1, countNumSiblings = 0, countNumSiblingsOfTag = 0;
      for (KNode sibling : node.parent.getChildren()) {
        if (sibling == node) {
          countChildIndex = countNumSiblings;
          countChildIndexOfTag = countNumSiblingsOfTag;
        }
        if (sibling.type == KNode.Type.TAG) {
          countNumSiblings++;
          if (node.value.equals(sibling.value)) countNumSiblingsOfTag++;
        }
      }
      childIndex = countChildIndex;
      childIndexOfTag = countChildIndexOfTag;
      numSiblings = countNumSiblings;
      numSiblingsOfTag = countNumSiblingsOfTag;
      if (countChildIndex == -1 || countChildIndexOfTag == -1)
        LogInfo.fails("WTF? %s %s %s", node, tag, node.fullText);
    }
    
    @Override
    public String toString() {
      return String.format("%s[%d/%d]", tag, childIndexOfTag, numSiblingsOfTag);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == this)
        return true;
      if (obj == null || obj.getClass() != this.getClass())
        return false;
      PathEntryAugmented that = (PathEntryAugmented) obj;
      return (this.tag.equals(that.tag)
          && this.childIndex == that.childIndex
          && this.childIndexOfTag == that.childIndexOfTag
          && this.numSiblings == that.numSiblings
          && this.numSiblingsOfTag == that.numSiblingsOfTag);
    }
    
    @Override
    public int hashCode() {
      return tag.hashCode() | childIndex << 24 | childIndexOfTag << 16 | numSiblings << 8 | numSiblingsOfTag;
    }
  }
  
  class AdvancedTreeTraverser implements TreeTraverser {
    List<PathEntryAugmented> ancestors;
    Set<ImmutableList<PathEntryAugmented>> foundRawPaths;
    ImmutableList<PathEntryAugmented> currentRawPath;
    List<PathEntry> currentTweakedPath;
    Set<ImmutableList<PathEntry>> foundTweakedPaths;
    
    public AdvancedTreeTraverser(KNode rootNode) {
      ancestors = Lists.newArrayList();
      foundRawPaths = Sets.newHashSet();
      foundTweakedPaths = Sets.newHashSet();
      traverseTree(rootNode);
      LogInfo.logs("Found %d raw paths", foundRawPaths.size());
      for (ImmutableList<PathEntryAugmented> rawPath : foundRawPaths) {
        //LogInfo.log(rawPath);
        currentRawPath = rawPath;
        createInitialTweakedPath();
        tweakPaths(1);
      }
      LogInfo.logs("Found %d tweaked paths", foundTweakedPaths.size());
    }
    
    private void traverseTree(KNode currentNode) {
      ancestors.add(new PathEntryAugmented(currentNode));
      // Process current node
      if (!isBlacklisted(currentNode))
        savePath();
      // Traverse children
      for (KNode child : currentNode.getChildren()) {
        if (child.type == KNode.Type.TAG) traverseTree(child);
      }
      ancestors.remove(ancestors.size() - 1);
    }
    
    private boolean isBlacklisted(KNode node) {
      if (node.fullText == null || node.fullText.isEmpty()) return true;
      if (BLACKLISTED_TAGS.contains(node.value)) return true;
      return false;
    }
    
    private void savePath() {
      foundRawPaths.add(ImmutableList.copyOf(ancestors));
    }
    
    int numWildCards = 0;
    int numEndCuts = 0;
    
    private void createInitialTweakedPath() {
      currentTweakedPath = Lists.newArrayList();
      for (PathEntryAugmented entry : currentRawPath) {
        if (entry.numSiblingsOfTag == 1) {
          currentTweakedPath.add(new PathEntry(entry.tag));
        } else {
          currentTweakedPath.add(new PathEntry(entry.tag, entry.childIndexOfTag));
        }
      }
      numWildCards = numEndCuts = 0;
    }
    
    
    /**
     * Tweak currentTweakedPath[n - depth]
     * (depth = 1, 2, ..., opts.maxTweakDepth)
     */
    private void tweakPaths(int depth) {
      int n = currentTweakedPath.size();
      if (depth > opts.maxTweakDepth || depth >= n) {
        foundTweakedPaths.add(ImmutableList.copyOf(currentTweakedPath));
        return;
      }
      tweakPaths(depth + 1);
      PathEntry swap = currentTweakedPath.get(n - depth);
      if (swap.index != -1) {
        currentTweakedPath.set(n - depth, swap.getNoIndexVersion());
        tweakPaths(depth + 1);
        currentTweakedPath.set(n - depth, swap);
      }
      if (depth <= opts.maxAdvancedTweakDepth && numEndCuts < opts.allowEndCuts) {
        int numSiblings = swap.tag.equals("*") ? currentRawPath.get(n - depth).numSiblings : 
          currentRawPath.get(n - depth).numSiblingsOfTag;
        if (numSiblings > 1) {
          numEndCuts++;
          currentTweakedPath.set(n - depth, new PathEntryWithRange(swap.tag, 1, 0));
          tweakPaths(depth + 1);
          currentTweakedPath.set(n - depth, new PathEntryWithRange(swap.tag, 0, 1));
          tweakPaths(depth + 1);
          currentTweakedPath.set(n - depth, swap);
          numEndCuts--;
        }
      }
      if (depth <= opts.maxAdvancedTweakDepth && numWildCards < opts.allowWildcards && !swap.tag.equals("*")) {
        numWildCards++;
        if (currentRawPath.get(n - depth).numSiblings == 1) {
          currentTweakedPath.set(n - depth, new PathEntry("*"));
        } else {
          currentTweakedPath.set(n - depth, new PathEntry("*", currentRawPath.get(n - depth).childIndex));
        }
        tweakPaths(depth);
        currentTweakedPath.set(n - depth, swap);
        numWildCards--;
      }
    }
    
    @Override
    public Collection<ImmutableList<PathEntry>> getFoundPaths() {
      //debugPrint();
      return foundTweakedPaths;
    }
    
    protected void debugPrint() {
      if (CandidateGenerator.iter == 0) {
        LogInfo.begin_track("Found paths");
        List<String> paths = Lists.newArrayList();
        for (List<PathEntry> path : foundTweakedPaths) {
          paths.add(PathUtils.getXPathString(path));
        }
        Collections.sort(paths);
        for (String path : paths) {
          LogInfo.log(path);
        }
        LogInfo.end_track();
      }
      CandidateGenerator.iter++;
    }
    
  }

  private static int iter = 0;
}
