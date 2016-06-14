package edu.stanford.nlp.semparse.open.ling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semparse.open.Main;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Interface with Stanford CoreNLP to do basic things like POS tagging and NER.
 * 
 * @author akchou
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LingData {
  public static class Options {
    @Option(gloss = "What CoreNLP annotators to run")
    public List<String> annotators = Arrays.asList("tokenize", "ssplit", "pos", "lemma", "ner");

    @Option(gloss = "Whether to use CoreNLP annotators")
    public boolean useAnnotators = true;

    @Option(gloss = "Whether to be case sensitive")
    public boolean caseSensitive = true;
    
    @Option(gloss = "Linguistic cache filename")
    public String lingCacheFilename = null;
    
    @Option(gloss = "Frequency of saving linguistic cache periodically")
    public int saveLingCacheFrequency = 50000;
  }
  public static Options opts = new Options();
  public static StanfordCoreNLP pipeline = null;
  
  // Update this when changing LingData's structure.
  private static final String VERSION = "4";
  
  private static final Set<String> AUX_VERBS = new HashSet<>(Arrays.asList(
      "is", "are", "was", "were", "am", "be", "been", "will",
      "shall", "have", "has", "had", "would", "could", "should", 
      "do", "does", "did", "can", "may", "might", "must", "seem"));
  
  public static final Set<String> OPEN_CLASS_POS_TAGS = new HashSet<>(Arrays.asList(
      "CD", "FW", "JJ", "JJR", "JJS", "NN", "NNP", "NNPS", "NNS", "RB",
      "RBR", "RBS", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ"));
  
  /**
   * OPEN = nouns, general verbs, adjectives, adverbs, numbers
   * AUX = auxiliary verbs (which is a special case of CLOSE)
   * CLOSE = other POS
   */
  public enum POSType { OPEN, AUX, CLOSE };

  // Tokenization of input.
  @JsonProperty
  public final List<String> tokens;
  @JsonProperty
  public final List<String> lemmaTokens;  // Lemmatized version

  // Syntactic information from JavaNLP.
  @JsonProperty
  public final List<String> posTags;  // POS tags
  @JsonProperty
  public final List<POSType> posTypes;  // type of POS tag
  @JsonProperty
  public final List<String> nerTags;  // NER tags
  @JsonProperty
  public final List<String> nerValues;  // NER values (contains times, dates, etc.)
  @JsonIgnore
  public final int length;

  public LingData(String utterance) {
    // Stanford tokenizer doesn't break hyphens.
    // Replace hyphens with spaces for utterances like
    // "Spanish-speaking countries" but not for "2012-03-28".
    StringBuilder buf = new StringBuilder(utterance);
    for (int i = 0; i < buf.length(); i++) {
      if (buf.charAt(i) == '-' && (i+1 < buf.length() && Character.isLetter(buf.charAt(i+1))))
        buf.setCharAt(i, ' ');
    }
    utterance = buf.toString();
    
    tokens = new ArrayList<>();
    posTags = new ArrayList<>();
    posTypes = new ArrayList<>();
    nerTags = new ArrayList<>();
    nerValues = new ArrayList<>();
    lemmaTokens = new ArrayList<>();

    if (opts.useAnnotators) {
      initModels();
      Annotation annotation = pipeline.process(utterance);

      for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
        String word = token.get(TextAnnotation.class);
        String wordLower = word.toLowerCase();
        if (opts.caseSensitive) {
          tokens.add(word);
        } else {
          tokens.add(wordLower);
        }
        String pos = token.get(PartOfSpeechAnnotation.class).intern();
        posTags.add(pos);
        posTypes.add(getPOSType(pos, word));
        
        nerTags.add(token.get(NamedEntityTagAnnotation.class).intern());
        lemmaTokens.add(token.get(LemmaAnnotation.class));
        nerValues.add(token.get(NormalizedNamedEntityTagAnnotation.class));
      }
      
    } else {
      // Create tokens crudely
      for (String token : utterance.trim().split("\\s+")) {
        tokens.add(token);
        lemmaTokens.add(token);
        try {
          Double.parseDouble(token);
          posTags.add("CD");
          posTypes.add(POSType.OPEN);
          nerTags.add("NUMBER");
          nerValues.add(token);
        } catch (NumberFormatException e ){
          posTags.add("UNK");
          posTypes.add(POSType.OPEN);
          nerTags.add("UNK");
          nerValues.add("UNK");
        }
      }
    }
    
    this.length = tokens.size();
  }
  
  private static POSType getPOSType(String pos, String word) {
    if (AUX_VERBS.contains(word.toLowerCase()) && pos.charAt(0) == 'V')
      return POSType.AUX;
    if (OPEN_CLASS_POS_TAGS.contains(pos))
      return POSType.OPEN;
    return POSType.CLOSE;
  }

  @JsonCreator
  public LingData(@JsonProperty("tokens") List<String> tokens,
                  @JsonProperty("lemmaTokens") List<String> lemmaTokens,
                  @JsonProperty("posTags") List<String> posTags,
                  @JsonProperty("posTypes") List<POSType> posTypes,
                  @JsonProperty("nerTags") List<String> nerTags,
                  @JsonProperty("nerValues") List<String> nerValues) {
    this.tokens = tokens;
    this.lemmaTokens = lemmaTokens;
    this.posTags = posTags;
    this.posTypes = posTypes;
    this.nerTags = nerTags;
    this.nerValues = nerValues;
    this.length = tokens.size();
  }
  
  public static void initModels() {
    initCoreNLPModels();
    BrownClusterTable.initModels();
    WordVectorTable.initModels();
    FrequencyTable.initModels();
    WordNetClusterTable.initModels();
    QueryTypeTable.initModels();
  }

  public static void initCoreNLPModels() {
    if (pipeline != null) return;
    LogInfo.begin_track("Initializing Core NLP Models ...");
    Properties props = new Properties();
    props.put("annotators", String.join(",", opts.annotators));
    if (opts.caseSensitive) {
      props.put("pos.model", "edu/stanford/nlp/models/pos-tagger/english-bidirectional/english-bidirectional-distsim.tagger");
      props.put("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
    } else {
      props.put("pos.model", "edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger");
      props.put("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");
    }
    pipeline = new StanfordCoreNLP(props);
    LogInfo.end_track();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LingData that = (LingData) o;
    if (!lemmaTokens.equals(that.lemmaTokens)) return false;
    if (!nerTags.equals(that.nerTags)) return false;
    if (!posTags.equals(that.posTags)) return false;
    if (!tokens.equals(that.tokens)) return false;
    return true;
  }

  // Return a string representing the tokens between start and end.
  public String phrase(int start, int end) {
    return sliceSequence(tokens, start, end);
  }
  public String lemmaPhrase(int start, int end) {
    return sliceSequence(lemmaTokens, start, end);
  }

  private static String sliceSequence(List<String> items, int start, int end) {
    if (start >= end) throw new RuntimeException("Bad indices");
    if (end - start == 1) return items.get(start);
    StringBuilder out = new StringBuilder();
    for (int i = start; i < end; i++) {
      if (out.length() > 0) out.append(' ');
      out.append(items.get(i));
    }
    return out.toString();
  }

  // If all the tokens in [start, end) have the same nerValues, but not
  // start-1 and end+1 (in other words, [start, end) is maximal), then return
  // the normalizedTag.  Example: queryNerTag = "DATE".
  public String getNormalizedNerSpan(String queryTag, int start, int end) {
    String value = nerValues.get(start);
    if (!queryTag.equals(nerTags.get(start))) return null;
    if (start-1 >= 0 && value.equals(nerValues.get(start-1))) return null;
    if (end < nerValues.size() && value.equals(nerValues.get(end))) return null;
    for (int i = start+1; i < end; i++)
      if (!value.equals(nerValues.get(i))) return null;
    return value;
  }

  public String getCanonicalPos(int index) {
    return getCanonicalPos(posTags.get(index));
  }

  private String getCanonicalPos(String pos) {
    if (pos.startsWith("N")) return "N";
    if (pos.startsWith("V")) return "V";
    if (pos.startsWith("W")) return "W";
    return pos;
  }
  
  // ============================================================
  // Caching
  // ============================================================
  
  protected static Map<String, LingData> cache = new ConcurrentHashMap<>();
  
  public static LingData get(String string) {
    LingData data = cache.get(string);
    if (data == null) {
      data = new LingData(string);
      cache.put(string, data);
      if (opts.saveLingCacheFrequency > 0 && cache.size() % opts.saveLingCacheFrequency == 0) {
        LogInfo.logs("Linguistic Cache size: %d", cache.size());
        saveCache();
      }
    }
    return data;
  }

  public static LingData getNoCache(String string) {
    LingData data = cache.get(string);
    if (data == null) {
      data = new LingData(string);
    }
    return data;
  }
  
  private static String getCachePath(String version) {
    String path = opts.lingCacheFilename;
    if (path == null) {
      path = "cache/ling-" + Main.opts.dataset + ".v" + version + ".json.gz";
    }
    new File(path).getParentFile().mkdirs();
    return path;
  }
  
  private static int lastSavedCacheSize = 0; 
  
  public synchronized static void saveCache() {
    LogInfo.begin_track("Saving linguistic data to cache ...");
    if (cache.size() == lastSavedCacheSize) {
      LogInfo.log("Cache unchanged.");    // Do nothing
    } else {
      String cachePath = getCachePath(VERSION);
      try (FileOutputStream out = new FileOutputStream(cachePath);
          GZIPOutputStream so = new GZIPOutputStream(out)) {
        out.getChannel().lock();
        ObjectMapper mapper = new ObjectMapper();
        lastSavedCacheSize = cache.size();
        mapper.writeValue(so, cache);
        LogInfo.logs("Written cache to %s", cachePath);
      } catch (Exception e) {
        LogInfo.warnings("Cache cannot be saved to %s!", cachePath);
        LogInfo.warning(e);
        e.printStackTrace();
      }
    }
    LogInfo.end_track();
  }

  public static void loadCache() {
    LogInfo.begin_track("Loading linguistic data from cache ...");
    String cachePath = getCachePath(VERSION);
    try (GZIPInputStream si = new GZIPInputStream(new FileInputStream(cachePath))) {
      ObjectMapper mapper = new ObjectMapper();
      cache = mapper.readValue(si, new TypeReference<ConcurrentHashMap<String, LingData>>(){});
      lastSavedCacheSize = cache.size();
      LogInfo.logs("Cache loaded from %s", cachePath);
    } catch (FileNotFoundException e) {
      LogInfo.warnings("Cache cannot be loaded: File %s does not exist", cachePath);
    } catch (Exception e) {
      LogInfo.warnings("Cache cannot be loaded: Cache corruped!");
      LogInfo.warning(e);
    }
    LogInfo.end_track();
  }

  // ============================================================
  // Getters
  // ============================================================
  
  /**
   * @param lemmatized
   *     whether to use lemmatized tokens
   * @param onlyOpenPOS
   *     whether to return only tokens with open-class POS tags (noun, verb, adjective, adverb)
   * @return A set of tokens
   */
  public Set<String> getTokens(boolean lemmatized, boolean onlyOpenPOS) {
    Set<String> answer = new HashSet<>();
    for (int i = 0; i < length; i++) {
      if (!onlyOpenPOS || posTypes.get(i) == POSType.OPEN) {
        if (lemmatized) {
          answer.add(lemmaTokens.get(i));
        } else {
          answer.add(tokens.get(i));
        }
      }
    }
    return answer;
  }
}
