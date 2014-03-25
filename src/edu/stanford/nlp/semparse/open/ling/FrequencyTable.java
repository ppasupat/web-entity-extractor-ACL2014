package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fig.basic.LogInfo;
import fig.basic.Option;

public class FrequencyTable {
  public static class Options {
    @Option public String frequencyFilename = null;
    @Option public List<Integer> frequencyAmounts = Lists.newArrayList(30, 300, 3000);
  }
  public static Options opts = new Options();

  public static Map<Integer, Set<String>> topWordsLists;
  
  public static void initModels() {
    if (topWordsLists != null || opts.frequencyFilename == null || opts.frequencyFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.frequencyFilename);
    LogInfo.logs("Reading word frequency from %s", dataPath);
    List<String> words = Lists.newArrayList();
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charsets.UTF_8)) {
      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\t");
        words.add(tokens[0]);
      }
    } catch (IOException e) {
      LogInfo.fails("Cannot load word frequency from %s", dataPath);
    }
    topWordsLists = Maps.newHashMap();
    for (int amount : opts.frequencyAmounts) {
      topWordsLists.put(amount, ImmutableSet.copyOf(words.subList(0, amount)));
    }
  }
}
