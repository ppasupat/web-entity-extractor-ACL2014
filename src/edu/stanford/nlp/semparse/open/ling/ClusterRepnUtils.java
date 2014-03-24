package edu.stanford.nlp.semparse.open.ling;

import fig.basic.StrUtils;

/*
To run:
    java -Xmx30g edu.stanford.nlp.semparse.open.CreateTypeEntityFeatures
*/
public class ClusterRepnUtils {
  public static String getRepn(String s) {
    if (s.equals("")) return "EMPTY";
    StringBuilder buf = new StringBuilder();
    if (true) {
      s = s.replaceAll(",", " , ");
      s = s.replaceAll("!", " ! ");
      s = s.replaceAll(":", " : ");
    }
    for (String x : s.split(" ")) {
      if (x.equals("")) continue;

      if (buf.length() > 0) buf.append(' ');
      String c = BrownClusterTable.getCluster(x);
      if (c == null) c = LingUtils.computePhraseShape(x);  // Unknown: replace with word form
      buf.append(c);
    }
    return buf.toString();
  }

  public static void main(String[] args) {
    System.out.println(getRepn(StrUtils.join(args)));
  }
}
