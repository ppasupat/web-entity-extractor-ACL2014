package edu.stanford.nlp.semparse.open.dataset.library;

import java.io.IOException;

import edu.stanford.nlp.semparse.open.dataset.Dataset;
import fig.basic.LogInfo;

public class DatasetLibrary {

  public static Dataset getDataset(String fullname) {
    if (fullname == null)
      return null;
    
    String[] parts = fullname.split("\\.");
    if (parts.length != 2)
      LogInfo.fails("Expected dataset format = family.name; got " + fullname);
    
    String family = parts[0], name = parts[1];
    if (family == null)
      LogInfo.fails("No dataset family specified.");
    
    // Special case : Unary family (the very old dataset)
    if ("unary".equals(family))
      return new UnaryDatasets().getDataset(name);
    
    // Load from the `datasets` directory
    try {
      return new JSONDatasetReader(family, name).getDataset();
    } catch (IOException e) {
      LogInfo.fail(e);
    }
    return null;
  }

}
