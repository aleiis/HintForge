package dev.aleiis.hintforge.model;

public class ContextAwareCompletionConfig {

	public static final String DEFAULT_FEW_SHOT_PROMPT = """
			MARKED CODE:
			token a = Foo;
			[[CURSOR]]
			token b = Bar;
			INSTRUCTION: Complete the fragment so that tokens a and b work together.
			OUTPUT: token a.setNext(token b)
			MARKED CODE:
			func compute(x) {
			    [[CURSOR]]
			    y = y + 3
			    return y;
			}
			INSTRUCTION: Complete the missing piece to double the value of x and store it in y.
			OUTPUT: let y = x * 2;
			MARKED CODE:
			{{code}}
			INSTRUCTION: {{instruction}}
			OUTPUT:
			""";

	private String fewShotPrompt;
	private int maxFixAttempts;
	private int maxGenerationAttempts;

	public ContextAwareCompletionConfig() {
		this.fewShotPrompt = DEFAULT_FEW_SHOT_PROMPT;
		this.maxFixAttempts = 2;
		this.maxGenerationAttempts = 1;
	}

	public ContextAwareCompletionConfig(String fewShotPrompt, int maxFixAttempts, int maxGenerationAttempts) {
		setFewShotPrompt(fewShotPrompt);
		setMaxFixAttempts(maxFixAttempts);
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
		if (!fewShotPrompt.contains("{{instruction}}")) {
			throw new IllegalArgumentException("fewShotPrompt must contain the {{instruction}} tag");
		}
		this.fewShotPrompt = fewShotPrompt;
	}

	public int getMaxFixAttempts() {
		return maxFixAttempts;
	}

	public void setMaxFixAttempts(int maxFixAttempts) {
		if (maxFixAttempts < 0) {
			throw new IllegalArgumentException("maxFixAttempts cannot be a negative value");
		}
		this.maxFixAttempts = maxFixAttempts;
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
