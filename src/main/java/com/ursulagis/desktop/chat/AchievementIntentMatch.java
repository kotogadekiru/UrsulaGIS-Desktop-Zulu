package com.ursulagis.desktop.chat;

/**
 * Best-effort match of the user's chat text to an onboarding achievement and its chat action.
 * Used by the intent catalog to decide whether to launch a UI action or only suggest guidance.
 *
 * @param action         chat action to run, or {@link UrsulaAction#UNKNOWN} when only a hint matched
 * @param achievementId  onboarding logro id that drove the match
 * @param score          relative match strength (higher is better)
 * @param suggestedReply short reply in Ursula's voice acknowledging the match
 */
public record AchievementIntentMatch(
		UrsulaAction action,
		String achievementId,
		double score,
		String suggestedReply) {
}
