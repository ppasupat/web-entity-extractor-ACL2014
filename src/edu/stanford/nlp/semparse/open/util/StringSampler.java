package edu.stanford.nlp.semparse.open.util;

import java.util.List;

public class StringSampler {

  public static <T> String sampleEntities(List<T> entities) {
    return sampleEntities(entities, entities.size());
  }
  
  private static final int TRAILING_ENTITIES = 3;
  private static final int MAX_TEXT_LENGTH = 60;
  public static final int DEFAULT_LIMIT = 30;
  
  public static <T> String sampleEntities(List<T> entities, int limit) {
    StringBuilder sb = new StringBuilder("{");
    int n = entities.size();
    if (n <= limit) {
      for (int i = 0; i < n; i++) {
        chopString(sb, entities.get(i).toString(), i > 0);
      }
    } else {
      for (int i = 0; i < limit - TRAILING_ENTITIES; i++) {
        chopString(sb, entities.get(i).toString(), i > 0);
      }
      sb.append(", ... (").append(n - limit).append(" more) ...");
      for (int i = n - TRAILING_ENTITIES - 1; i < n; i++) {
        chopString(sb, entities.get(i).toString(), i > 0);
      }
    }
    return sb.append("} (").append(n).append(" total)").toString();
  }
  
  private static void chopString(StringBuilder sb, String x, boolean addComma) {
    if (addComma) sb.append(", ");
    sb.append('"');
    x = x.replace("\n", " ");
    if (x.length() > MAX_TEXT_LENGTH)
      sb.append(x.substring(0, MAX_TEXT_LENGTH)).append("...\"");
    else
      sb.append(x).append('"');
  }
}
