package utils.search;

import java.util.Set;

public class CompletionResult implements Comparable<CompletionResult> {

	public float score;
	public String id;
	public String type;
	public String value;
	public Set<String> tokens;

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			SearchResult otherResult = (SearchResult) other;
			return id.equals(otherResult.id);
		}
		return false;
	}

	@Override
	public int compareTo(CompletionResult o) {
		// higher score is "less", i.e. earlier in sorted list
		return (int) -Math.signum(score - o.score);
	}

}
