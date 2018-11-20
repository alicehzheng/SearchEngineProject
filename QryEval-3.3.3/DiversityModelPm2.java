import java.util.ArrayList;
import java.util.Map;

/**
 * Created on 11/19/18
 * @author alicehzheng
 *
 */
public class DiversityModelPm2 extends DiversityModel {
	DiversityModelPm2 (String initial_ranking_file, int max_input_rankings_length, int max_result_ranknig_length,
			String intents_file, double lambda){
		super(initial_ranking_file, max_input_rankings_length, max_result_ranknig_length,
			intents_file, lambda);
	}
	public LiteScoreList rerank(Map<String,ArrayList> doc2scores, int intentCnt){
		LiteScoreList r = new LiteScoreList();
		int docCnt = 0;
		Double v =  maxResultRankingLength / (double)intentCnt;
		ArrayList<Double> coverList = new ArrayList<Double>();
		ArrayList<Double> priList = new ArrayList<Double>();
		for(int i = 0; i <= intentCnt; ++i){ // index starts at 1 !!!
			coverList.add(0.0);
			priList.add(0.0);
		}
		while(docCnt < maxResultRankingLength && !doc2scores.isEmpty()){
			String nextDoc = null;
			Double maxScore = 0.0;
			int nextIntent = 1;
			Double maxPri = 0.0;
			for(int i = 1; i <= intentCnt; ++i) {
				Double curPri = v / (2 * coverList.get(i) + 1);
				priList.set(i, curPri);
				if(curPri > maxPri){
					nextIntent = i;
					maxPri = curPri;
				}	
			}
			for(String doc: doc2scores.keySet()){
				ArrayList<Double> scores = doc2scores.get(doc);
				Double curScore = lambda * maxPri * scores.get(nextIntent);
				for(int i = 1; i <= intentCnt; ++i){
					if(i == nextIntent)
						continue;
					curScore += (1-lambda) * priList.get(i) * scores.get(i);
				}
				if(nextDoc == null || curScore > maxScore){
					nextDoc = doc;
					maxScore = curScore;
				}
			}
			if(maxScore == 0.0){
				nextDoc = null;
				for(String doc: doc2scores.keySet()){
					ArrayList<Double> scores = doc2scores.get(doc);
					Double curScore = scores.get(0);
					if(nextDoc == null || curScore > maxScore){
						nextDoc = doc;
						maxScore = curScore;
					}
				}
				r.add(nextDoc, maxScore);
				doc2scores.remove(nextDoc);
			}
			else{
				r.add(nextDoc, maxScore);
				ArrayList<Double> scores = doc2scores.get(nextDoc);
				doc2scores.remove(nextDoc);
				Double denominator = 0.0;
				for(int i = 1; i <= intentCnt; ++i)
					denominator += scores.get(i);
				for(int i = 1; i <= intentCnt; ++i){
					coverList.set(i,coverList.get(i) + scores.get(i) / denominator);
				}
			}
			docCnt++;
		}
		r.sortExternal();
		return r;
	}

}
