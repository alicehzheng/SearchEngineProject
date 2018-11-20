/**
 *  Created on 11/19/18 by @alicehzheng
 */
import java.io.*;
import java.util.*;

/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 */
public class LiteScoreList {

  //  A utility class to create a <internalDocid, externalDocid, score>
  //  object.

  private class ScoreListEntry {

    private String externalId;
    private double score;

    private ScoreListEntry(String externalDocid, double score) {
      this.externalId = externalDocid;
      this.score = score;
    }
  }

  /**
   *  A list of document ids and scores. 
   */
  private List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param externalid An external document id.
   *  @param score The document's score.
   */
  public void add(String externalid, double score) {
    scores.add(new ScoreListEntry(externalid, score));
  }


  /**
   *  Get the score of the n'th entry.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

  /**
   *  Set the score of the n'th entry.
   *  @param n The index of the score to change.
   *  @param score The new score.
   */
  public void setDocidScore(int n, double score) {
    this.scores.get(n).score = score;
  }

  /**
   *  Get the size of the score list.
   *  @return The size of the posting list.
   */
  public int size() {
    return this.scores.size();
  }



  /*
   *  Added on 09/16/18 by @alicehzheng
   *  Compare two ScoreListEntry objects.  Sort by score, then
   *  external docid.
   */
  public class ScoreListExternalComparator implements Comparator<ScoreListEntry> {

    @Override
    public int compare(ScoreListEntry s1, ScoreListEntry s2) {
      if (s1.score > s2.score)
          return -1;
      else if (s1.score < s2.score)
          return 1;
      else if ((s1.externalId).compareTo(s2.externalId) > 0) // if s1's externalId is larger than s2's externalId, s2 goes first
        return 1;
      else
        return -1;
    }
  }

  
  /**
   *  added on 09/16/18 by @alichehzheng
   *  Sort the list by score and external document id.
   */
  public void sortExternal () {
    Collections.sort(this.scores, new ScoreListExternalComparator());
  }
  
  
  /**
   * Reduce the score list to the first num results to save on RAM.
   * 
   * @param num Number of results to keep.
   */
  public void truncate(int num) {
    List<ScoreListEntry> truncated = new ArrayList<ScoreListEntry>(this.scores.subList(0,
        Math.min(num, scores.size())));
    this.scores.clear();
    this.scores = truncated;
  }
}
