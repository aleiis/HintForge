package dev.aleiis.hintforge.model;

public class IdentifierSuggestionConfig {

	public static final String DEFAULT_FEW_SHOT_PROMPT = """
			CONTEXT:
			type [[CURSOR]] {
				id: Int!
				shape_id: String!
				geometry: LineString!
				generated: Boolean!
			}
			IDENTIFIERS:
			ShapePoint
			RouteSegment
			PathStep
			LinePath
			RoutePoint
			CONTEXT:
			type Frequency {
				id: Int!
				start_time: [[CURSOR]]
				end_time: Seconds!
				headway_secs: Int!
				exact_times: Int!
			}
			IDENTIFIERS:
			Seconds!
			Seconds
			Int!
			Int
			Timestamp!
			CONTEXT:
			{{code}}
			IDENTIFIERS:
			""";

	private String fewShotPrompt;
	private int maxGenerationAttempts;

	public IdentifierSuggestionConfig() {
		this.fewShotPrompt = DEFAULT_FEW_SHOT_PROMPT;
		this.maxGenerationAttempts = 1;
	}

	public IdentifierSuggestionConfig(String fewShotPrompt, int maxGenerationAttempts) {
		setFewShotPrompt(fewShotPrompt);
		setMaxGenerationAttempts(maxGenerationAttempts);
	}

	public String getFewShotPrompt() {
		return fewShotPrompt;
	}

	public void setFewShotPrompt(String fewShotPrompt) {
		if (fewShotPrompt == null || fewShotPrompt.isBlank()) {
			throw new IllegalArgumentException("fewShotPrompt cannot be null or blank");
		}
		if (!fewShotPrompt.contains("{{code}}")) {
			throw new IllegalArgumentException("fewShotPrompt must contain the {{code}} tag");
		}
		this.fewShotPrompt = fewShotPrompt;
	}
	
	public int getMaxGenerationAttempts() {
		return maxGenerationAttempts;
	}

	public void setMaxGenerationAttempts(int maxGenerationAttempts) {
		if (maxGenerationAttempts < 1) {
			throw new IllegalArgumentException("maxGenerationAttempts must be at least 1");
		}
		this.maxGenerationAttempts = maxGenerationAttempts;
	}
}
