package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import fig.basic.LogInfo;
import fig.basic.Option;

public class WordNetClusterTable {
  public static class Options {
    @Option public String wordnetClusterFilename = null;
  }
  public static Options opts = new Options();

  public static Map<String, String> wordClusterMap;
  
  public static void initModels() {
    if (wordClusterMap != null || opts.wordnetClusterFilename == null || opts.wordnetClusterFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.wordnetClusterFilename);
    LogInfo.logs("Reading WordNet clusters from %s", dataPath);
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charset.forName("UTF-8"))) {
      wordClusterMap = new HashMap<>();
      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\t");
        wordClusterMap.put(tokens[0], tokens[1]);
      }
    } catch (IOException e) {
      LogInfo.fails("Cannot load WordNet clusters from %s", dataPath);
    }
  }
  
  public static String getCluster(String word) {
    initModels();
    return wordClusterMap.get(word);
  }
}
