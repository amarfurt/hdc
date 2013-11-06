package utils.search;

public class SearchResult implements Comparable<SearchResult> {

	public String id;
	public float score;
	public String data;
	public String highlighted;

	@Override
	public int compareTo(SearchResult o) {
		// higher score is "less", i.e. earlier in sorted list
		return (int) -Math.signum(score - o.score);
	}

}