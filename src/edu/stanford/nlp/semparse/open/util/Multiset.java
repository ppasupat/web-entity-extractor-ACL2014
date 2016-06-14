package edu.stanford.nlp.semparse.open.util;

import java.util.*;

public class Multiset<T> {
  protected final HashMap<T, Integer> map = new HashMap<>();
  protected int size = 0;

  public void add(T entry) {
    Integer count = map.get(entry);
    if (count == null)
      count = 0;
    map.put(entry, count + 1);
    size++;
  }
  
  public void add(T entry, int incr) {
    Integer count = map.get(entry);
    if (count == null)
      count = 0;
    map.put(entry, count + incr);
    size += incr;
  }

  public boolean contains(T entry) {
    return map.containsKey(entry);
  }

  public int count(T entry) {
    Integer count = map.get(entry);
    if (count == null)
      count = 0;
    return count;
  }

  public Set<T> elementSet() {
    return map.keySet();
  }

  public Set<Map.Entry<T, Integer>> entrySet() {
    return map.entrySet();
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }
  
  public Multiset<T> getPrunedByCount(int minCount) {
    Multiset<T> pruned = new Multiset<>();
    for (Map.Entry<T, Integer> entry : map.entrySet()) {
      if (entry.getValue() >= minCount)
        pruned.add(entry.getKey(), entry.getValue());
    }
    return pruned;
  }
}
