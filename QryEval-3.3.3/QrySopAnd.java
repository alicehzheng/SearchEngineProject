/**
 *  Created on 09/15/18 by @alicehzheng
 *  Modified on 09/30/18 by @alicehzheng
 */

import java.io.*;
import java.lang.Math;

/**
 *  The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

  /**
   *  Modified on 09/30/18 by @alicehzheng : adding Indri model processing
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
      if(r instanceof RetrievalModelIndri)
          return this.docIteratorHasMatchMin(r); // and operator for indri calculate scores for docs that have at least one query term
      else
          return this.docIteratorHasMatchAll (r);
  }

  /**
   *  Modified on 09/30/18 by @alicehzheng : adding Indri model processing
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
    else if(r instanceof RetrievalModelIndri){
        return this.getScoreIndri(r);
    }
    else{
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }
  
  /**
   *  Added on 09/30/18 by @alicehzheng
   *  Get a default score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @param docid The document whose default score is going to be calculated
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if(r instanceof RetrievalModelIndri){
        return this.getDefaultScoreIndri(r,docid);
    }
    else{
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the getDefaultScore");
    }
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
   *  getScore for the RankedBoolean retrieval model (MIN)
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      int docidMatched = this.docIteratorGetMatch();
      double minScore = Double.MAX_VALUE;
      for (int i=0; i<this.args.size(); i++) {
          Qry q_i = this.args.get(i);
          // QrySopAnd can only have QrySop operators as arguments
          // Note: may need to be modified to throw exception
          if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched){
              double score_i = ((QrySop) q_i).getScore(r);
              if (score_i < minScore)
                  minScore = score_i;
          }
      }
      return minScore;
      
    }
  }
  
  /**
   *  Created on 09/30/18 by @alicehzheng
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreIndri (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
        int docidMatched = this.docIteratorGetMatch();
        int q_len = this.args.size();
        double exp = 1 / (double)q_len;
        Qry q_i = this.args.get(0);
        double score = 0.0;
        if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched)
            score = Math.pow((double)((QrySop) q_i).getScore(r), exp);
        else 
            score = Math.pow((double)((QrySop) q_i).getDefaultScore(r,docidMatched), exp);
        for(int i = 1; i < q_len; i++){
            q_i = this.args.get(i);
            if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched)
                score = score * Math.pow((double)((QrySop) q_i).getScore(r), exp);
            else 
                score = score * Math.pow((double)((QrySop) q_i).getDefaultScore(r,docidMatched), exp);
        }
      return score;
    }
  }
  
  /**
   *  Created on 09/30/18 by @alicehzheng
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getDefaultScoreIndri (RetrievalModel r, long docid) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
        int q_len = this.args.size();
        double exp = 1 / (double)q_len;
        Qry q_i = this.args.get(0);
        double score = Math.pow((double)((QrySop) q_i).getDefaultScore(r,docid), exp);
        for(int i = 1; i < q_len; i++){
            q_i = this.args.get(i);
            score *= Math.pow((double)((QrySop) q_i).getDefaultScore(r,docid), exp);
        }
      return score;
    }
  }


}
