/**
 *  class created on 09/29/18 by @alicehzheng
 */

/**
 *  An object that stores parameters for the BM25 and indicates to the query
 *  operators how the query should be evaluated.
 */


public class RetrievalModelBM25 extends RetrievalModel{
    RetrievalModelBM25(){
        this.k1 = 0.0;
        this.k3 = 0.0;
        this.b = 0.0;
    }
    RetrievalModelBM25(double k1, double k3, double b){
        this.k1 = k1;
        this.k3 = k3;
        this.b = b;
    }
    public String defaultQrySopName () {
        return new String ("#sum");
      }
    // paramters for the BM25 model
    double k1, k3, b;
}
