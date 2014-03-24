package edu.stanford.nlp.semparse.open.util;

import java.util.Iterator;

/**
 * A slightly more memory-efficient List of Pair<String, Double>.
 * 
 * Many parts of the code are from http://developer.classpath.org/doc/java/util/ArrayList-source.html
 */
public class StringDoubleArrayList implements Iterable<StringDoublePair> {

  public static final int DEFAULT_CAPACITY = 10;
  
  private int size;
  
  // The two data arrays must have equal length.
  private String[] strings;
  private double[] doubles;
  
  public StringDoubleArrayList(int capacity) {
    if (capacity < 0)
      throw new IllegalArgumentException();
    strings = new String[capacity];
    doubles = new double[capacity];
  }
  
  public StringDoubleArrayList() {
    this(DEFAULT_CAPACITY);
  }
  
  public int size() {
    return size;
  }
  
  public void ensureCapacity(int minCapacity) {
    if (minCapacity - strings.length > 0) {  // subtract to prevent overflow
      {
        String[] newStrings = new String[Math.max(strings.length * 2, minCapacity)];
        System.arraycopy(strings, 0, newStrings, 0, size);
        strings = newStrings;
      }
      {
        double[] newDoubles = new double[Math.max(doubles.length * 2, minCapacity)];
        System.arraycopy(doubles, 0, newDoubles, 0, size);
        doubles = newDoubles;
      }
    }
  }
  
  public void add(String s, double d) {
    if (size == strings.length)
      ensureCapacity(size + 1);
    strings[size] = s;
    doubles[size] = d;
    size++;
  }
  
  private void checkBoundExclusive(int index) {
    if (index >= size)
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
  }
  
  public String getString(int index) {
    checkBoundExclusive(index);
    return strings[index];
  }
  
  public double getDouble(int index) {
    checkBoundExclusive(index);
    return doubles[index];
  }

  public class StringDoubleArrayListIterator implements Iterator<StringDoublePair>, StringDoublePair {
    int index = -1;

    @Override
    public boolean hasNext() {
      return index < size - 1;
    }

    @Override
    public StringDoublePair next() {
      index++;
      return this;
    }

    @Override
    public void remove() {
      throw new RuntimeException("Cannot remove stuff from StringDoubleArrayList");
    }

    @Override
    public String getFirst() {
      return strings[index];
    }

    @Override
    public double getSecond() {
      return doubles[index];
    }
  }
  
  @Override
  public Iterator<StringDoublePair> iterator() {
    return new StringDoubleArrayListIterator();
  }
  
}
