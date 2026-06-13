package com.ursulagis.desktop.gui.chat;

import java.util.prefs.Preferences;

/**
 * Persisted preferences for the Ursula assistant chat window.
 */
public final class ChatPreferences {

	private static final String PREFS_NODE = "com/ursulagis/desktop/chat";
	private static final String SHOW_AT_START_KEY = "showAtStart";

	private static final ChatPreferences INSTANCE = new ChatPreferences();

	private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

	private ChatPreferences() {
	}

	public static ChatPreferences getInstance() {
		return INSTANCE;
	}

	public boolean isShowAtStart() {
		return prefs.getBoolean(SHOW_AT_START_KEY, true);
	}

	public void setShowAtStart(boolean show) {
		prefs.putBoolean(SHOW_AT_START_KEY, show);
		try {
			prefs.flush();
		} catch (Exception ignored) {
			// ignore
		}
	}
}
