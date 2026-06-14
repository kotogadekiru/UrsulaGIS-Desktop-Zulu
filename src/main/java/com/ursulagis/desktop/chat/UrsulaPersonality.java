package com.ursulagis.desktop.chat;

import com.ursulagis.desktop.gui.Messages;

/**
 * Voice and tone for Ursula, the female GIS assistant.
 */
public final class UrsulaPersonality {

	private UrsulaPersonality() {
	}

	public static String roleName() {
		return msg("Chat.roleUrsula", "Ursula IA");
	}

	public static String greeting() {
		return msg("Chat.greeting",
				"Hi! What shall we do today?");
	}

	public static String systemPromptPreamble() {
		return msg("Chat.personalitySystem",
				"You are Ursula, a friendly female GIS assistant for Ursula GIS. "
						+ "You speak warmly, clearly, and professionally. "
						+ "In the JSON \"message\" field, reply in Ursula's voice: concise, helpful, and encouraging.");
	}

	public static String helpIntro() {
		return msg("Chat.helpIntro",
				"I'm Ursula, and I can help you with the map. For example:");
	}

	public static String unknownReply() {
		return msg("Chat.unknownReply",
				"I'm not sure I understood. Try \"help\" to see what I can do for you today.");
	}

	private static String msg(String key, String fallback) {
		String s = Messages.getString(key);
		return (s != null && !s.equals(key)) ? s : fallback;
	}
}
