package edu.stanford.nlp.semparse.open.util;

import java.util.*;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;

public class BipartiteMatcher {

  private final int SOURCE = 1;
  private final int SINK = -1;
  
  private final Map<Object, Integer> fromMap;
  private final Map<Object, Integer> toMap;
  private final Map<Integer, List<Integer>> edges;

  public BipartiteMatcher() {
    this.fromMap = new HashMap<>();
    this.toMap = new HashMap<>();
    this.edges = new HashMap<>();
  }

  public BipartiteMatcher(List<TargetEntity> targetEntities, List<String> predictedEntities) {
    this();
    for (int i = 0; i < targetEntities.size(); i++) {
      TargetEntity targetEntity = targetEntities.get(i);
      for (int j = 0; j < predictedEntities.size(); j++) {
        if (targetEntity.match(predictedEntities.get(j))) {
          this.addEdge(i, j);
        }
      }
    }
  }
  
  public void addEdge(Object fromObj, Object toObj) {
    Integer from = fromMap.get(fromObj), to = toMap.get(toObj);
    if (from == null) {
      from = 2 + fromMap.size();
      fromMap.put(fromObj, from);
      if (!edges.containsKey(SOURCE)) edges.put(SOURCE, new ArrayList<>());
      edges.get(SOURCE).add(from);
    }
    if (to == null) {
      to = - 2 - toMap.size();
      toMap.put(toObj, to);
      if (!edges.containsKey(to)) edges.put(to, new ArrayList<>());
      edges.get(to).add(SINK);
    }
    if (!edges.containsKey(from)) edges.put(from, new ArrayList<>());
    edges.get(from).add(to);
  }
  
  private List<Integer> foundPath;
  private Set<Integer> foundNodes;
  
  public int findMaximumMatch() {
    int count = 0;
    this.foundPath = new ArrayList<>();
    this.foundNodes = new HashSet<>();
    while (findPath(SOURCE)) {
      count++;
      for (int i = 0; i < foundPath.size() - 1; i++) {
        int from = foundPath.get(i), to = foundPath.get(i+1);
        edges.get(from).remove(Integer.valueOf(to));
        if (!edges.containsKey(to)) edges.put(to, new ArrayList<>());
        edges.get(to).add(from);
      }
      foundPath.clear();
      foundNodes.clear();
    }
    return count;
  }
  
  private boolean findPath(int node) {
    // DFS
    foundNodes.add(node);
    foundPath.add(node);
    if (node == SINK) return true;
    for (int dest : edges.get(node)) {
      if (!foundNodes.contains(dest)) {
        if (findPath(dest)) return true;
      }
    }
    foundPath.remove(foundPath.size() - 1);
    return false;
  }
  
  public static void main(String[] args) {
    // Test Method
    BipartiteMatcher bm = new BipartiteMatcher();
    bm.addEdge("A", 1); bm.addEdge("A", 2); bm.addEdge("A", 4);
    bm.addEdge("B", 1); bm.addEdge("C", 2); bm.addEdge("C", 1);
    bm.addEdge("D", 4); bm.addEdge("D", 5); bm.addEdge("E", 3);
    System.out.println(bm.findMaximumMatch());
  }
  
}
