package com.ursulagis.desktop.chat;

/**
 * GitHub repository used as source-of-truth for chat code context.
 */
public final class GitHubRepoConfig {

	public static final String OWNER = "kotogadekiru";
	public static final String REPO = "UrsulaGIS-Desktop-Zulu";
	public static final String BRANCH = "main";
	public static final String RAW_BASE = "https://raw.githubusercontent.com/"
			+ OWNER + "/" + REPO + "/" + BRANCH + "/";
	public static final String API_BASE = "https://api.github.com/repos/"
			+ OWNER + "/" + REPO;

	private GitHubRepoConfig() {
	}
}
