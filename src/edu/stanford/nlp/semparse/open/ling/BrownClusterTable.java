package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fig.basic.LogInfo;
import fig.basic.Option;

public class BrownClusterTable {
  public static class Options {
    @Option public String brownClusterFilename = null;
  }
  public static Options opts = new Options();

  public static Map<String, String> wordClusterMap;
  public static Map<String, Integer> wordFrequencyMap;
  
  public static void initModels() {
    if (wordClusterMap != null || opts.brownClusterFilename == null || opts.brownClusterFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.brownClusterFilename);
    LogInfo.logs("Reading Brown clusters from %s", dataPath);
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charsets.UTF_8)) {
      wordClusterMap = Maps.newHashMap();
      wordFrequencyMap = Maps.newHashMap();
      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\t");
        wordClusterMap.put(tokens[1], tokens[0].intern());
        wordFrequencyMap.put(tokens[1], Integer.parseInt(tokens[2]));
      }
    } catch (IOException e) {
      LogInfo.fails("Cannot load Brown cluster from %s", dataPath);
    }
  }
  
  public static String getCluster(String word) {
    initModels();
    return wordClusterMap.get(word);
  }
  
  public static String getClusterPrefix(String word, int length) {
    initModels();
    String answer = wordClusterMap.get(word);
    if (answer == null) return null;
    return answer.substring(0, Math.min(length, answer.length()));
  }
  
  public static final int[] DEFAULT_PREFIXES = {4, 6, 10, 20};
  
  public static List<String> getDefaultClusterPrefixes(String cluster) {
    List<String> answer = Lists.newArrayList();
    if (cluster != null)
      for (int length : DEFAULT_PREFIXES)
        answer.add("[" + length + "]" + cluster.substring(0, Math.min(length, cluster.length())));
    return answer;
  }
  
  public static List<String> getDefaultClusterPrefixesFromWord(String word) {
    return getDefaultClusterPrefixes(getCluster(word));
  }
  
  public static List<String> getDefaultClusterPrefixes(String cluster1, String cluster2) {
    List<String> answer = Lists.newArrayList();
    for (int length : DEFAULT_PREFIXES) {
      answer.add(cluster1.substring(0, Math.min(length, cluster1.length()))
          + "|" + cluster2.substring(0, Math.min(length, cluster2.length())));
    }
    return answer;
  }
  
  public static int getSmoothedFrequency(String word) {
    initModels();
    Integer frequency = wordFrequencyMap.get(word);
    if (frequency == null) return 1;
    return frequency + 1;
  }
}
