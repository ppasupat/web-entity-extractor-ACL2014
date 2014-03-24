package edu.stanford.nlp.semparse.open.dataset.library;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The format of the JSON file is
 * 
 * <pre>
 *   {
 *     "options": {
 *       "cacheDirectory": "(location of cache directory -- default = web.cache)",
 *       "useHashcode": (true if the page should be loaded from the frozen cache by hashcode
 *                       and not from the Internet -- default = false),
 *       "detailed": (true if detailed data is available -- default = false)
 *     },
 *     "data": [ ... ]
 *   }
 * </pre>
 *   
 * where each element in the data array is
 * 
 * <pre>
 *   {
 *     "hashcode": "(OPTIONAL - hashcode for frozen cache)",
 *     "query": "(MANDATORY - query string)",
 *     "url": "(OPTIONAL - url)",
 *     "entities": [ ...(MANDATORY - target entity strings)... ]
 *     "criteria": { ...(OPTIONAL - mapping from "first", "second", and "last" to entity string)... }
 *   }
 * </pre>
 *   
 */
public class JSONDataset {
  
  @JsonIgnoreProperties(ignoreUnknown=true)
  public static class JSONDatasetOption {
    public String cacheDirectory = null;
    public boolean useHashcode = false;
    public boolean detailed = false;
    
    @Override
    public String toString() {
      return new StringBuilder()
        .append("useHashcode: ").append(useHashcode).append("\n")
        .append("cacheDirectory: ").append(cacheDirectory).append("\n")
        .append("detailed: ").append(detailed).append("\n")
        .toString();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown=true)
  public static class JSONDatasetDatum {
    public String hashcode;
    public String query;
    public String url;
    public List<String> entities;
    public List<JSONDatasetRawAnswers> rawanswers;
    public JSONDatasetCriteria criteria;
    
    @Override
    public String toString() {
      return new StringBuilder()
        .append("[").append(query)
        .append(hashcode == null ? "" : " " + hashcode).append("]")
        .toString();
    }
  }
  
  public enum JSONDatasetRawAnswerType { Z, L, H };
  
  public static class JSONDatasetRawAnswers {
    public JSONDatasetRawAnswerType type;
    public List<String> answers;
  }
  
  public static class JSONDatasetCriteria {
    public String first, second, last;
  }
  
  public JSONDatasetOption options;
  public List<JSONDatasetDatum> data;
}
