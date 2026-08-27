package com.ursulagis.desktop.chat;

import com.ursulagis.desktop.gui.Messages;

/**
 * Localized voice and tone helpers for Ursula, the female GIS chat assistant.
 * Supplies greeting, help intro, unknown-reply, and the system-prompt preamble
 * injected into AI intent and guidance calls.
 */
public final class UrsulaPersonality {

	/** Prevents instantiation. */
	private UrsulaPersonality() {
	}

	/** Display name used in the chat UI chrome (falls back to “Ursula IA”). */
	public static String roleName() {
		return msg("Chat.roleUrsula", "Ursula IA");
	}

	/** First message shown when the chat panel opens. */
	public static String greeting() {
		return msg("Chat.greeting",
				"Hi! What shall we do today?");
	}

	/**
	 * Short personality instructions prepended to AI system prompts so replies
	 * stay in Ursula's warm, concise voice.
	 */
	public static String systemPromptPreamble() {
		return msg("Chat.personalitySystem",
				"You are Ursula, a friendly female GIS assistant for Ursula GIS. "
						+ "You speak warmly, clearly, and professionally. "
						+ "In the JSON \"message\" field, reply in Ursula's voice: concise, helpful, and encouraging.");
	}

	/** Intro line before the help bullet list of available chat actions. */
	public static String helpIntro() {
		return msg("Chat.helpIntro",
				"I'm Ursula, and I can help you with the map. For example:");
	}

	/** Fallback when no action or achievement match is found. */
	public static String unknownReply() {
		return msg("Chat.unknownReply",
				"I'm not sure I understood. Try \"help\" to see what I can do for you today.");
	}

	/** Localized string for {@code key}, or {@code fallback} when the bundle has no entry. */
	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
