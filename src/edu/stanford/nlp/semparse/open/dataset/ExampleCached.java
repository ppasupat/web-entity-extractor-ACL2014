package edu.stanford.nlp.semparse.open.dataset;

/**
 * A CachedExample is an Example that should build the knowledge tree from the
 * cached web page instead of from the Web.
 * 
 * It is useful for testing datasets annotated on cached web pages.
 */
public class ExampleCached extends Example {
  public final String hashcode, cacheDirectory, url;
  
  public ExampleCached(String phrase, String url) {
    this(phrase, null, null, url, null);
  }
  
  public ExampleCached(String phrase, String cacheDirectory, String hashcode, String url) {
    this(phrase, cacheDirectory, hashcode, url, null);
  }
  
  public ExampleCached(String phrase, String cacheDirectory, String hashcode, String url, ExpectedAnswer expectedAnswer) {
    super(phrase, expectedAnswer);
    this.url = url;
    this.hashcode = hashcode;
    this.cacheDirectory = cacheDirectory;
  }
  
  @Override public String toString() {
    StringBuilder sb = new StringBuilder("[").append(phrase).append("]")
        .append("[").append(cacheDirectory).append("/").append(hashcode).append("]");
    if (url != null)
      sb.append("[").append(url).append("]");
    return sb.toString();
  }
}
