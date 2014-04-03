package edu.stanford.nlp.semparse.open.ling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import fig.basic.LogInfo;
import fig.basic.Option;

public class QueryTypeTable {
  public static class Options {
    @Option public String queryTypeFilename = null;
  }
  public static Options opts = new Options();

  public static Map<String, String> queryHeadwordMap, queryTypeMap;
  
  public static void initModels() {
    if (queryTypeMap != null || opts.queryTypeFilename == null || opts.queryTypeFilename.isEmpty()) return;
    Path dataPath = Paths.get(opts.queryTypeFilename);
    LogInfo.logs("Reading query types from %s", dataPath);
    try (BufferedReader in = Files.newBufferedReader(dataPath, Charsets.UTF_8)) {
      queryHeadwordMap = Maps.newHashMap();
      queryTypeMap = Maps.newHashMap();
      String line = null;
      while ((line = in.readLine()) != null) {
        String[] tokens = line.split("\t");
        queryHeadwordMap.put(tokens[0], tokens[1]);
        queryTypeMap.put(tokens[0], tokens[2]);
      }
    } catch (IOException e) {
      LogInfo.fails("Cannot load query types from %s", dataPath);
    }
  }
  
  public static String getQueryHeadword(String query) {
    initModels();
    return queryHeadwordMap.get(query);
  }
  
  public static String getQueryType(String query) {
    initModels();
    return queryTypeMap.get(query);
  }
}
