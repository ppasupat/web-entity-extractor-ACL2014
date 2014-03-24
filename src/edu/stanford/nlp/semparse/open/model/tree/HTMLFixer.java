package edu.stanford.nlp.semparse.open.model.tree;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

import fig.basic.LogInfo;

/**
 * Fix problematic HTML structures that decrease our accuracy.
 * 
 * All fixes are done in place, so the document will be mutated.
 */
public class HTMLFixer {

  private Document document;
  
  public HTMLFixer(Document doc) {
    this.document = doc;
  }
  
  // ============================================================
  // Fix Table (colspan / rowspan)
  // ============================================================
  
  public void fixAllTables() {
    LogInfo.begin_track("Fix table ...");
    for (Element table : document.getElementsByTag("tbody")) {
      fixTable(table);
    }
    LogInfo.end_track();
  }
  
  /**
   * Normalize colspan and rowspan in the table
   * @param tbody   An Element with tag name "tbody"
   */
  private void fixTable(Element tbody) {
    // Fix colspan
    int numColumns = 0;
    for (Element tr : tbody.children()) {
      for (Element cell : Lists.newArrayList(tr.children())) {
        int colspan = parseIntHard(cell.attr("colspan")), rowspan = parseIntHard(cell.attr("rowspan"));
        if (colspan <= 1) continue;
        cell.attr("old-colspan", cell.attr("colspan"));
        cell.removeAttr("colspan");
        String tagName = cell.tagName();
        for (int i = 2; i <= colspan; i++) {
          if (rowspan <= 1)
            cell.after(String.format("<%s></%s>", tagName, tagName));
          else
            cell.after(String.format("<%s rowspan=%d></%s>", tagName, rowspan, tagName));
        }
      }
      numColumns = Math.max(numColumns, tr.children().size());
    }
    // Fix rowspan (assuming each column has 1 cell without colspan)
    int[] counts = new int[numColumns];       // For each column, track how many rows we should create new elements for
    String[] tags = new String[numColumns];   // For each column, track what type of elements to create
    for (Element tr : tbody.children()) {
      Element currentCell = null;
      List<Element> cells = Lists.newArrayList(tr.children());
      for (int i = 0, k = 0; i < numColumns; i++) {
        if (counts[i] > 0) {
          // Create a new element caused by rowspan
          String newCell = String.format("<%s></%s>", tags[i], tags[i]);
          if (currentCell == null)
            tr.prepend(newCell);
          else
            currentCell.after(newCell);
          counts[i]--;
        } else {
          if (k >= cells.size()) continue;  // Unfilled row
          currentCell = cells.get(k++);
          int rowSpan = parseIntHard(currentCell.attr("rowspan"));
          if (rowSpan <= 1) continue;
          counts[i] = rowSpan - 1;
          tags[i] = currentCell.tagName();
          currentCell.attr("old-rowspan", currentCell.attr("rowspan"));
          currentCell.removeAttr("rowspan");
        }
      }
    }
  }
  
  private int parseIntHard(String s) {
    if (s.isEmpty()) return 0;
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
  
  // ============================================================
  // Fix BR
  // ============================================================
  
  public void fixAllBRs() {
    LogInfo.begin_track("Fix BR ...");
    Elements brList;
    while (!(brList = document.getElementsByTag("br")).isEmpty()) {
      fixBR(brList.get(0).parent());
    }
    LogInfo.end_track();
  }
  
  /**
   * Fix BR tags by wrapping each part in P tag instead
   */
  private void fixBR(Element parent) {
    List<Node> childNodes = parent.childNodesCopy();
    while (parent.childNodeSize() > 0) {
      Node child = parent.childNode(0);
      child.remove();
    }
    Element currentChild = document.createElement("p"); 
    for (Node node : childNodes) {
      if (node instanceof Element && "br".equals(((Element) node).tagName())) {
        parent.appendChild(currentChild);
        currentChild = document.createElement("p"); 
      } else {
        currentChild.appendChild(node);
      }
    }
    parent.appendChild(currentChild);
  }
}
