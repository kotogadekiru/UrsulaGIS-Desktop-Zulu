package com.ursulagis.desktop.chat;

/**
 * Result of matching user text against an onboarding achievement intent.
 */
public record AchievementIntentMatch(
		UrsulaAction action,
		String achievementId,
		double score,
		String suggestedReply) {
}
