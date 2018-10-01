/**
 *  class created on 09/30/18 by @alicehzheng
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The WSUM operator for Indri
 */
public class QrySopWsum extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if(r instanceof RetrievalModelIndri)
            return this.docIteratorHasMatchMin(r); // wsum operator for indri calculate scores for docs that have at least one query term
        else
            return this.docIteratorHasMatchAll (r);
    }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
    public double getScore (RetrievalModel r) throws IOException {

        if(r instanceof RetrievalModelIndri){
            return this.getScoreIndri(r);
        }
        else{
          throw new IllegalArgumentException
            (r.getClass().getName() + " doesn't support the WAND operator.");
        }
      }
  
    /**
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
   *  getScore for Indri: addes up the score of arguments
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
            int w_len = this.arg_weights.size();
            if(w_len < q_len)
                throw new IllegalArgumentException("doesn't have enought weights");
            
            Qry q_i = this.args.get(0);
            double w_sum = 0.0;
            for(int i = 0; i < w_len; i++)
                w_sum += this.arg_weights.get(i);
            double w_i = this.arg_weights.get(0);
            double score = 0.0;
            
           
            if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched)
                score += (w_i / w_sum) * (double)((QrySop) q_i).getScore(r);
            else 
                score += (w_i / w_sum) * (double)((QrySop) q_i).getDefaultScore(r,docidMatched);
            for(int i = 1; i < q_len; i++){
                q_i = this.args.get(i);
                w_i = this.arg_weights.get(i);
                if(q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docidMatched)
                    score = score + (w_i / w_sum) * (double)((QrySop) q_i).getScore(r);
                else 
                    score = score + (w_i / w_sum) * (double)((QrySop) q_i).getDefaultScore(r,docidMatched);
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
            int w_len = this.arg_weights.size();
            if(w_len < q_len)
                throw new IllegalArgumentException("doesn't have enought weights");
            
            Qry q_i = this.args.get(0);
            double w_sum = 0.0;
            for(int i = 0; i < w_len; i++)
                w_sum += this.arg_weights.get(i);
            double w_i = this.arg_weights.get(0);
            double score = 0.0;
            score += (w_i / w_sum) * (double)((QrySop) q_i).getDefaultScore(r,docid);
            for(int i = 1; i < q_len; i++){
                q_i = this.args.get(i);
                score = score + (w_i / w_sum) * (double)((QrySop) q_i).getDefaultScore(r,docid);
            }
          return score;
        }
      }



}