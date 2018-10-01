/**
 *  Copyright (c) 2018 Carnegie Mellon University.  All Rights Reserved.
 *  Modified on 09/15/18 by @alicehzheng
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchMin (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } 
    else if(r instanceof RetrievalModelRankedBoolean){
        return this.getScoreRankedBoolean(r);
    }
    else {
        throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }
  
  /**
   *  Added on 09/30/18 by @alicehzheng
   *  Get a default score for a documnet denoted by docid
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {
    return 0.0;
  }
  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }
  
  /**
   *  added on 09/15/18 by alicehzheng
   *  getScore for the RankedBoolean retrieval model (MAX)
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      int docidMatched = this.docIteratorGetMatch();
      double maxScore = 0.0;
      for (int i=0; i<this.args.size(); i++) {
          Qry q_i = this.args.get(i);
          // QrySopOr can only have QrySop operators as arguments
          // Note: may need to be modified to throw exception
          if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched){
              double score_i = ((QrySop) q_i).getScore(r);
              if (score_i > maxScore)
                  maxScore = score_i;
          }
      }
      return maxScore;
        
    }
  }


}
