/**
 *  Created on 09/15/18 by @author alicehzheng 
 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR/n operator for all retrieval models.
 */
public class QryIopNear extends QryIop {
	
  /**
   *  The Distance n denoted in "#Near/n"
  */	
	
  private int distance;

  QryIopNear(){
      this.distance = 1; // default distance is 1
  }
  
  QryIopNear(int d){
	  this.distance = d;
  }
  
  /**
   *  Added on 09/16/18 by @alicehzheng
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   */
  protected void evaluate () throws IOException {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.
    
    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }

    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    while (true) {

      // Advance all doc iterators until they point to the same document
      
      int maxDocid = Qry.INVALID_DOCID;
      while(true){
          // Find the maximum doc id among all the doc ids currently pointed to by different queries in arguments
          
          for(Qry q_i: this.args){
              if(q_i.docIteratorHasMatch(null)){
                  int q_iDocid = q_i.docIteratorGetMatch();
                  if((maxDocid == Qry.INVALID_DOCID) || (q_iDocid > maxDocid))
                      maxDocid = q_iDocid;
              }
              else // if one of the argument is exhausted, return
                  return; 
          }
          if(maxDocid == Qry.INVALID_DOCID) // if no maxDocid is found, return
              return;
          
          // Advance all doc iterators to the maxDocid
          boolean point2same = true;
          for(Qry q_i: this.args){
              q_i.docIteratorAdvanceTo(maxDocid);
              if(!q_i.docIteratorHasMatch(null)) // if one of the argument is exhausted, return
                  return;
              if(q_i.docIteratorGetMatch() != maxDocid){
                  point2same = false;
                  break;
              }    
          }
          // If all doc iterators are pointing to the same document, break
          if(point2same) 
              break;   
      }
      
      
      //  Create a new posting that marks the right-most location of the NEAR phrase in a document

      List<Integer> positions = new ArrayList<Integer>();
      
      // Find all valid combination in the document
      
      boolean allHaveMatch = true;
      QryIop q_0 = (QryIop)this.args.get(0);
      
      while(q_0.locIteratorHasMatch()){
          int prev_loc = q_0.locIteratorGetMatch();
          boolean validCombination = true;
          for(int i = 1; i < this.args.size(); ++i){
              QryIop q_i = (QryIop)this.args.get(i);
              q_i.locIteratorAdvancePast(prev_loc);
              if(!q_i.locIteratorHasMatch()){
                  allHaveMatch = false;
                  break;
              }
              int cur_loc = q_i.locIteratorGetMatch();
              if(cur_loc - prev_loc > this.distance){
                  validCombination = false;
                  break;
              }
              prev_loc = cur_loc;
          }
          if(!allHaveMatch)
              break;
          // If a valid combination is found, add the right-most location to the posting, and advance all loc iterators
          if(validCombination){
              positions.add(prev_loc); 
              for(int i = 0; i < this.args.size(); ++i){
                  ((QryIop)this.args.get(i)).locIteratorAdvance();
              }   
          }
          else // Else, only advance q_0's loc iterator
              q_0.locIteratorAdvance();
      }
      
      // If posting is not empty, add it to the inverted list
      if(!positions.isEmpty())
          this.invertedList.appendPosting (maxDocid, positions);
      
      // Advance all doc iterators past maxDocid
      for(Qry q_i: args){
          q_i.docIteratorAdvancePast(maxDocid);
          if(!q_i.docIteratorHasMatch(null)) // if any query is exhausted, return
              return;
      } 
    }
  }

}


