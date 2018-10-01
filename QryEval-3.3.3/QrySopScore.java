/**
 *  Copyright (c) 2018 Carnegie Mellon University.  All Rights Reserved.
 *  Modified on 09/16/18 by @alichehzheng
 *  Modified on 09/30/18 by @alichehzheng
 */

import java.io.*;
import java.lang.IllegalArgumentException;
import java.lang.Math;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
    /**
     * Modified on 09/30/18 by @alicehzheng
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        /**
        if(r instanceof RetrievalModelIndri)
            return this.docIteratorHasMatchMin(r); // score operator for indri calculate scores for docs that have at least one query term
        else
        **/
        return this.docIteratorHasMatchAll (r);
    }

  /**
   *  Modified on 09/16/18 by @alicehzheng : added Ranked Boolean
   *  Modified on 09/29/18 by @alicehzheng: added BM25
   *  Modified on 09/30/18 by @alicehzheng: added Indri
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
    else if(r instanceof RetrievalModelBM25){
        return this.getScoreBM25(r);
    }
    else if(r instanceof RetrievalModelIndri){
        return this.getScoreIndri(r);
    }
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
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

    if (r instanceof RetrievalModelIndri) {
      return this.getDefaultScoreIndri (r, docid);
    } 
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the getDefaultScore");
    }
  }
  /**
   *  added on 09/30/18 by @alichehzneg
   *  getDefaultScore for the Indri model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScoreIndri (RetrievalModel r, long docid) throws IOException {
      RetrievalModelIndri model = (RetrievalModelIndri) r;
      int mu = model.mu;
      double lambda = model.lambda;
      QryIop arg = (QryIop)this.getArg(0);
      double defaultScore = 0.0;
      double ctf = (double)arg.getCtf(); // collection term frequency associated with this argument
      long c_len = Idx.getSumOfFieldLengths(arg.getField()); // total length of collection in a specified field
      int doc_len = Idx.getFieldLength(arg.getField(), (int)docid); // document length of doc docid in specified field
      if(ctf == 0)
          ctf = 0.5;
      double MLE = ctf / (double) c_len;
      defaultScore = (1 - lambda) * (mu * MLE / (double)(doc_len + mu)) + lambda * MLE;
      return defaultScore; 
  }
  /**
   *  added on 09/30/18 by @alichehzneg
   *  getScore for the Indri model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreIndri (RetrievalModel r) throws IOException {
      RetrievalModelIndri model = (RetrievalModelIndri) r;
      int mu = model.mu;
      double lambda = model.lambda;
      QryIop arg = (QryIop)this.getArg(0);
      int docid = this.docIteratorGetMatch();
      double score = 0.0;
      double ctf = (double)arg.getCtf(); // collection term frequency associated with this argument
      long c_len = Idx.getSumOfFieldLengths(arg.getField()); // total length of collection in a specified field
      int doc_len = Idx.getFieldLength(arg.getField(), docid); // document length of doc docid in specified field
      int tf = arg.getTfinDoc(); // term frequency
      double MLE = ctf / (double) c_len;
      score = (1 - lambda) * ((tf + mu * MLE) / (double)(doc_len + mu)) + lambda * MLE;
      return score;
    
  }
  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }
  
  /**
   *  added on 09/16/18 by @alichehzneg
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
        return 0.0;
    } 
    else {
        return this.getArg(0).getTfinDoc(); // calculate score as the term frequency associated with the matched document
    }
  }
  
  /**
   *  added on 09/29/18 by @alichehzneg
   *  getScore for the BM25 model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25 (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
        return 0.0;
    } 
    else {
        RetrievalModelBM25 model = (RetrievalModelBM25) r;
        QryIop arg = (QryIop)this.getArg(0);
        int docidMatched = this.docIteratorGetMatch();
        double score = 0.0;
        double idfWeight = 0.0;
        double tfWeight = 0.0;
        long N = Idx.getNumDocs(); // total number of documents
        int df = arg.getDf(); // document frequency of this term
        double avg_len = Idx.getSumOfFieldLengths(arg.getField()) / (float) Idx.getDocCount (arg.getField()); // average document length of specified field
        int doc_len = Idx.getFieldLength(arg.getField(), docidMatched); // document length of matched document in specified field
        int tf = arg.getTfinDoc(); // term frequency
        idfWeight = Math.max(0.0, Math.log((N - df + 0.5) / (double) (df + 0.5)));
        double k1 = model.k1;
        double b = model.b;
        tfWeight = ((double)tf) / (tf + k1 * ((1-b) + b * ((double)doc_len) / avg_len));
        score = idfWeight * tfWeight;
        return score;
    }
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
