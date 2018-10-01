/**
 *  class created on 09/29/18 by @alicehzheng
 */

/**
 *  An object that stores parameters for the Indri and indicates to the query
 *  operators how the query should be evaluated.
 */


public class RetrievalModelIndri extends RetrievalModel {
    RetrievalModelIndri(){
        this.mu = 0;
        this.lambda = 0.0;
    }
    RetrievalModelIndri(int m, double l){
        this.mu = m;
        this.lambda = l;
    }
    public String defaultQrySopName () {
        return new String ("#AND");
    }
    int mu;
    double lambda;
}

