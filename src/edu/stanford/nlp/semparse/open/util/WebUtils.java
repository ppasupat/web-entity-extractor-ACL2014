package edu.stanford.nlp.semparse.open.util;

import java.io.IOException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fig.basic.Utils;

/** 
 * Handy utilities for interacting with the web.
 */ 
public class WebUtils {
  private static ObjectMapper jsonMapper = new ObjectMapper();

  /**
   * Return the contents of a webpage.
   */
  private static Document executeGetWebpageScript(String flags) {
    try {
      String contents = Utils.systemGetStringOutput("./scripts/get-webpage.py " + flags);
      return Jsoup.parse(contents);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Document getWebpage(String url) {
    url = url.replaceAll("'", "'\"'\"'");
    return executeGetWebpageScript(" '" + url + "' ");
  }
  
  public static Document getWebpageFromHashcode(String cacheDirectory, String hashcode) {
    String flags = " -H " + hashcode;
    if (cacheDirectory != null && !cacheDirectory.isEmpty())
      flags += " -d " + cacheDirectory;
    return executeGetWebpageScript(flags);
  }

  /**
   * Return the search results for a given query.
   */
  public static List<SearchResult> googleSearch(String query) {
    // Query is just a single webpage
    if (query.startsWith("http://"))
      return Collections.singletonList(new SearchResult(query, query, null));

    try {
      query = query.replaceAll("'", "'\"'\"'");
      String contents = Utils.systemGetStringOutput("./scripts/google-search.py '" + query + "'");
      JsonNode root = jsonMapper.readTree(contents.getBytes("UTF-8"));
      List<SearchResult> pages = new ArrayList<>();
      for (JsonNode item : root) {
        pages.add(new SearchResult(query, item.get(0).asText(), item.get(1).asText()));
      }
      return pages;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Equivalent to doing Google Search but actually reading from file.
   */
  public static List<SearchResult> fakeGoogleSearch(String query) {
    try {
      query = query.replaceAll("'", "'\"'\"'");
      String contents = Utils.systemGetStringOutput("./scripts/fake-google-search.py '" + query + "'");
      JsonNode root = jsonMapper.readTree(contents.getBytes("UTF-8"));
      List<SearchResult> pages = new ArrayList<>();
      for (JsonNode item : root) {
        pages.add(new SearchResult(query, item.get("link").asText(), item.get("title").asText()));
      }
      return pages;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
