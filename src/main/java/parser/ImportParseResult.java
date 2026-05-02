package parser;

import java.util.Map;

public record ImportParseResult(
    ParseResult parseResult,
    Map<TransitionKey, TransitionStats> transitions
) {}

