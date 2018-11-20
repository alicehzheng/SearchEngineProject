import java.util.ArrayList;
import java.util.Map;

/**
 * Created on 11/19/18
 * @author alicehzheng
 *
 */
public class DiversityModelXquad extends DiversityModel {
	DiversityModelXquad (String initial_ranking_file, int max_input_rankings_length, int max_result_ranknig_length,
			String intents_file, double lambda){
		super(initial_ranking_file, max_input_rankings_length, max_result_ranknig_length,
			intents_file, lambda);
	}
	public LiteScoreList rerank(Map<String,ArrayList> doc2scores, int intentCnt){
		LiteScoreList r = new LiteScoreList();
		int docCnt = 0;
		ArrayList<Double> penaltyList = new ArrayList<Double>();
		for(int i = 0; i <= intentCnt; ++i) // index starts at 1 !!!
			penaltyList.add(1.0);
		while(docCnt < maxResultRankingLength && !doc2scores.isEmpty()){
			String nextDoc = null;
			Double maxScore = 0.0;
			for(String doc: doc2scores.keySet()){
				ArrayList<Double> scores = doc2scores.get(doc);
				Double curScore = (1 - lambda) * scores.get(0);
				for(int i = 1; i <= intentCnt; ++i){
					curScore += lambda * 1 / (double)intentCnt * scores.get(i) * penaltyList.get(i) ;
				}
				if(nextDoc == null || curScore > maxScore){
					nextDoc = doc;
					maxScore = curScore;
				}
			}
			r.add(nextDoc, maxScore);
			ArrayList<Double> scores = doc2scores.get(nextDoc);
			doc2scores.remove(nextDoc);
			for(int i = 1; i <= intentCnt; ++i){
				penaltyList.set(i, penaltyList.get(i) * (1 - scores.get(i)));
			}
			docCnt++;
		}
		
		return r;
	}
}
