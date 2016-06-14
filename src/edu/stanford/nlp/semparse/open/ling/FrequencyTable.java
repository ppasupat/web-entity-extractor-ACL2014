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

public class FrequencyTable {
  public static class Options {
    @Option public String frequencyFilename = null;
    @Option public List<Integer> frequencyAmounts = Arrays.asList(30, 300, 3000);
  }
  public static Options opts = new Options();

  public static Map<Integer, Set<String>> topWordsLists;
  
  public static void initModels() {
    if (topWordsLists != null || opts.frequencyFilename == null || opts.frequencyFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.frequencyFilename);
    LogInfo.logs("Reading word frequency from %s", dataPath);
    List<String> words = new ArrayList<>();
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charset.forName("UTF-8"))) {
      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\t");
        words.add(tokens[0]);
      }
    } catch (IOException e) {
      LogInfo.fails("Cannot load word frequency from %s", dataPath);
    }
    topWordsLists = new HashMap<>();
    for (int amount : opts.frequencyAmounts) {
      topWordsLists.put(amount, new HashSet<>(words.subList(0, amount)));
    }
  }
}
