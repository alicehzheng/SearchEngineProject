/**
 *  class created on 09/29/18 by @alicehzheng
 */

/**
 *  An object that stores parameters for the Indri and indicates to the query
 *  operators how the query should be evaluated.
 */


public class RetrievalModelIndri extends RetrievalModel {
    public String defaultQrySopName () {
        return new String ("#AND");
      }
}

