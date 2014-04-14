package edu.stanford.nlp.semparse.open.model.tree;

import java.util.List;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import edu.stanford.nlp.semparse.open.dataset.ExampleCached;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.util.SearchResult;
import edu.stanford.nlp.semparse.open.util.WebUtils;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Compute the knowledge tree for the given example by doing web searches or following links.
 */
public class KnowledgeTreeBuilder {
  public static class Options {
    @Option(gloss = "Maximum number of search results to keep per query") public int maxResultsPerSearch = 1;
    @Option public int maxFullTextLength = 140;
    @Option public boolean ignoreTextNodes = false;
    @Option public boolean useWikipedia = false;
    @Option public boolean useFakeGoogle = false;
    @Option(gloss = "level of entity string normalization when creating the knowledge tree "
                  + "(0 = none / 1 = whitespace / 2 = simple / 3 = aggressive)")
    public int earlyNormalizeEntities = 1;
    @Option public boolean alsoNormalizeBR = true;
    @Option public boolean onlyNormalizeBR = false;
  }
  public static Options opts = new Options();

  public void buildKnowledgeTree(Example ex) {
    LogInfo.begin_track("KnowledgeTreeBuilder %s", ex);
    
    if (ex instanceof ExampleCached) {
      
      ExampleCached cex = (ExampleCached) ex;
      if (cex.hashcode != null && cex.cacheDirectory != null) {
        // Use the cached web page
        LogInfo.begin_track("[CACHED %s]", cex.hashcode);
        ex.tree = new KNode(null, KNode.Type.QUERY, cex.phrase);
        Document doc = WebUtils.getWebpageFromHashcode(cex.cacheDirectory, cex.hashcode);
        buildKnowledgeTreeFromDocument(doc, ex.tree.createChild(KNode.Type.URL, cex.url));
        LogInfo.end_track();
      } else {
        // Download from the Internet
        LogInfo.begin_track("[URL %s]", cex.url);
        ex.tree = new KNode(null, KNode.Type.QUERY, cex.phrase);
        Document doc = WebUtils.getWebpage(cex.url);
        buildKnowledgeTreeFromDocument(doc, ex.tree.createChild(KNode.Type.URL, cex.url));
        LogInfo.end_track();
      }
      
    } else if (opts.useWikipedia) {
      
      // Cheat on Wikipedia
      String query = ex.phrase;
      String url = "http://en.wikipedia.org/wiki/" + query.replaceAll(" ", "_");
      LogInfo.begin_track("[WIKIPEDIA %s]", url);
      ex.tree = new KNode(null, KNode.Type.QUERY, query);
      Document doc = WebUtils.getWebpage(url);
      buildKnowledgeTreeFromDocument(doc, ex.tree.createChild(KNode.Type.URL, url));
      LogInfo.end_track();
      
    } else {
      
      // Perform a Google search
      String query = "list of " + ex.phrase;
      List<SearchResult> results;
      if (opts.useFakeGoogle) {
        results = WebUtils.fakeGoogleSearch(query);
      } else {
        results = WebUtils.googleSearch(query);
      }
      if (results.size() > opts.maxResultsPerSearch)
        results = results.subList(0, opts.maxResultsPerSearch);
      LogInfo.logs("%s search results", results.size());
  
      // Fetch the web pages of all the top pages.
      ex.tree = new KNode(null, KNode.Type.QUERY, query);
      for (SearchResult result : results) {
        LogInfo.begin_track("%s", result);
        Document doc = WebUtils.getWebpage(result.url);
        buildKnowledgeTreeFromDocument(doc, ex.tree.createChild(KNode.Type.URL, result.url));
        LogInfo.end_track();
      }
      
    }

    LogInfo.end_track();
  }
  
  /**
   * Build a knowledge tree from jsoup Document object and attach the result to |root|.
   * @param doc    The jsoup Document. The first child of |doc| should be an <html> tag.
   * @param root   The parent of the created tree's root node.
   */
  public void buildKnowledgeTreeFromDocument(Document doc, KNode root) {
    if (!doc.child(0).tagName().equals("html")) {
      LogInfo.fail(doc.child(0).tagName());
    }
    HTMLFixer fixer = new HTMLFixer(doc);
    fixer.fixAllTables();
    if (!opts.onlyNormalizeBR) {
      convertElementToKTree(doc.child(0), root);
    }
    if (opts.alsoNormalizeBR) {
      fixer.fixAllBRs();
      convertElementToKTree(doc.child(0), root);
    }
    for (KNode htmlNode : root.getChildren()) {
      htmlNode.generateTimestamp();
    }
  }
  
  /**
   * Convert jsoup Element (= an HTML tag and its content) into a knowledge tree.
   * Contents inside style tag (CSS) and script tag (JavaScript) are ignored.
   * 
   * @param elt       The jsoup Element corresponding to the root of the tree
   * @param parent    The parent of the created tree's root node.
   */
  public void convertElementToKTree(Element elt, KNode parent) {
    String eltText = LingUtils.normalize(elt.text(), opts.earlyNormalizeEntities);
    KNode currentNode = parent.createChild(KNode.Type.TAG, elt.tagName(),
        eltText.length() > opts.maxFullTextLength ? null : eltText);

    // Add children
    for (Node child : elt.childNodes()) {
      if (child instanceof Element) {
        convertElementToKTree((Element) child, currentNode);
      } else if (child instanceof TextNode) {
        if (!opts.ignoreTextNodes) {
          String text = LingUtils.normalize(((TextNode) child).text(), opts.earlyNormalizeEntities);
          if (!text.isEmpty()) {
            //currentNode.createChild(KNode.Type.TEXT, text, text);
            currentNode.createChild(KNode.Type.TAG, "text",
                text.length() > opts.maxFullTextLength ? null : text);
          }
        }
      }
    }
    
    // Add attributes
    for (Attribute attr : elt.attributes()) {
      currentNode.createAttribute(attr.getKey(), attr.getValue());
    }
  }
  
  // ============================================================
  // Test Suite
  // ============================================================
  
  public static void main(String[] args) {
    KnowledgeTreeBuilder builder = new KnowledgeTreeBuilder();
    /*{
      ExampleCached ex = new ExampleCached("snooker tournaments", "frozen.cache/wiki/",
          "e88739bb552c5abef23b24fbf6e2e911cd3d2bac", null, null);
      builder.buildKnowledgeTree(ex);
      KNodeUtils.printTree(ex.tree.getChildren().get(0).getChildren().get(0));
    }*/
    {
      ExampleCached ex = new ExampleCached("Slovenian film", "frozen.cache/wiki/",
          "62f4c2c17afec7e54d6362f280a1e6ab65444e73", null, null);
      builder.buildKnowledgeTree(ex);
      KNodeUtils.printTree(ex.tree);
    }
    /*{
      ExampleCached ex = new ExampleCached("oxymorons", "frozen.cache/02/",
          "1bdfe640b5f5680f328e90a77136fd883a3b0105", null, null);
      builder.buildKnowledgeTree(ex);
      KNodeUtils.printTree(ex.tree.getChildren().get(0).getChildren().get(0));
    }*/
  }
  
}

