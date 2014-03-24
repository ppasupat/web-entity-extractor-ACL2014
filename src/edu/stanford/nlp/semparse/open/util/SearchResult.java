package edu.stanford.nlp.semparse.open.util;

// Corresponds to a webpage 
public class SearchResult {
  // query: how did we get to this web page (possibly null)?
  public SearchResult(String query, String url, String title) {
    this.query = query;
    this.url = url;
    this.title = title;
  }
  public final String query;
  public final String url;
  public final String title;

  @Override public String toString() { return url; }
}