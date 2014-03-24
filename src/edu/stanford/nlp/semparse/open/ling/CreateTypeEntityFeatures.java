package edu.stanford.nlp.semparse.open.ling;

import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.end_track;
import static fig.basic.LogInfo.logs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fig.basic.IOUtils;
import fig.basic.MapUtils;
import fig.basic.Option;
import fig.basic.TVMap;
import fig.exec.Execution;

/*
To run:
    java -Xmx30g edu.stanford.nlp.semparse.open.CreateTypeEntityFeatures
*/
public class CreateTypeEntityFeatures implements Runnable {
  Map<String, String> clusterMap = new HashMap<String, String>();
  Map<String, Double> typeEntityCluster1Counts = new HashMap<String, Double>();
  Map<String, Double> typeEntityCluster2Counts = new HashMap<String, Double>();

  // Hack
  String pluralize(String s) {
    if (s.endsWith("y")) return s.substring(0, s.length()-1) + "ies";
    if (s.endsWith("s")) return s + "es";
    return s + "s";
  }

  TVMap<String, String> nameMap = new TVMap<String, String>();

  @Option public String namesPath = "/u/nlp/data/semparse/scr/freebase/names.ttl";
  @Option public String typesPath = "/u/nlp/data/semparse/scr/freebase/types.ttl";
  @Option public int maxLines = Integer.MAX_VALUE;

  void readNames() {
    begin_track("Reading names");
    try {
      String line;
      BufferedReader in = IOUtils.openIn(namesPath);
      int numLines = 0;
      while ((line = in.readLine()) != null && numLines++ < maxLines) {
        String[] tokens = line.split("\t");

        String id = tokens[0];
        if (id.startsWith("fb:user")) continue;
        if (id.startsWith("fb:base")) continue;
        if (id.startsWith("fb:freebase")) continue;
        if (id.startsWith("fb:common")) continue;
        if (id.startsWith("fb:type")) continue;
        if (id.startsWith("fb:measurement_unit")) continue;

        // Remove "@en.
        String name = tokens[2].substring(1, tokens[2].length() - 5);
        nameMap.put(id, name);

        if (numLines % 1000000 == 0) logs("%d lines", numLines);
      }
      in.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    end_track();
  }

  public void run() {
    readNames();
    readTypes();
    outputCounts();
  }

  void readTypes() {
    begin_track("Reading entities");
    try {
      String line;
      BufferedReader in = IOUtils.openIn(typesPath);
      Set<String> hit = new HashSet<String>();
      int numLines = 0;
      // Write raw strings
      PrintWriter out = IOUtils.openOutHard("/u/nlp/data/open-semparse/scr/freebase/types-entities.tsv");
      while ((line = in.readLine()) != null && numLines++ < maxLines) {
        String[] tokens = line.split("\t");
        String entityId = tokens[0];
        String origTypeId = tokens[2].substring(0, tokens[2].length()-1);

        String entity = nameMap.get(entityId, null);
        if (entity == null) continue;
        String entityCluster = ClusterRepnUtils.getRepn(entity);

        String origType = nameMap.get(origTypeId, null);
        if (origType == null) continue;

        out.println(origTypeId + "\t" + origType + "\t" + entity);

        String[] typeTokens = origType.split(" ");
        if (typeTokens.length > 3) continue; // Only keep short phrases

        MapUtils.incr(typeEntityCluster1Counts, origType + "\t" + entityCluster, 1);

        String headType = typeTokens[typeTokens.length-1]; // Just take head
        for (String type : headType.split("/")) {
          type = pluralize(type.toLowerCase());  // Process the type
          if (!hit.contains(type)) {
            logs("new type: %s", type);
            hit.add(type);
          }
          String typeCluster = ClusterRepnUtils.getRepn(type);
          MapUtils.incr(typeEntityCluster2Counts, typeCluster + "\t" + entityCluster, 1);
        }

        if (numLines % 1000000 == 0) logs("%d lines", numLines);
      }
      in.close();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    end_track();
  }

  void outputCounts() {
    // Write out counts
    begin_track("Writing out %d counts", typeEntityCluster1Counts.size());
    PrintWriter out = IOUtils.openOutHard("/u/nlp/data/open-semparse/scr/freebase/types-entities-cluster1-counts.tsv");
    for (Map.Entry<String, Double> e : typeEntityCluster1Counts.entrySet())
      out.println(e.getKey() + "\t" + e.getValue());
    out.close();
    end_track();

    begin_track("Writing out %d counts", typeEntityCluster2Counts.size());
    out = IOUtils.openOutHard("/u/nlp/data/open-semparse/scr/freebase/types-entities-cluster2-counts.tsv");
    for (Map.Entry<String, Double> e : typeEntityCluster2Counts.entrySet())
      out.println(e.getKey() + "\t" + e.getValue());
    out.close();
    end_track();
  }

  public static void main(String[] args) {
    Execution.run(args, new CreateTypeEntityFeatures());
  }
}
