/**
 *  class created on 09/29/18 by @alicehzheng
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SUM operator for BM25.
 */
public class QrySopSum extends QrySop {

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
    return this.docIteratorHasMatchFirst (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25 (r);
    } 
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SUM operator.");
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
   *  added on 09/29/18 by @alichehzneg
   *  getScore for BM25: addes up the score of arguments
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25 (RetrievalModel r) throws IOException {
      if (! this.docIteratorHasMatchCache()) {
          return 0.0;
        } else {
          double sumScore = 0;
          for (int i=0; i<this.args.size(); i++) {
              Qry q_i = this.args.get(i);
              // QrySopSum can only have QrySop operators as arguments
              // Note: may need to be modified to throw exception
              double score_i = ((QrySop) q_i).getScore(r);
              sumScore += score_i;
          }
          return sumScore;
          
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