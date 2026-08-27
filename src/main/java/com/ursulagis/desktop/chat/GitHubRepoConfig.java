package com.ursulagis.desktop.chat;

/**
 * Identifies the public GitHub repository used as source-of-truth when chat
 * builds code context for LLM prompts ({@link GitHubCodeContextBuilder}).
 */
public final class GitHubRepoConfig {

	/** GitHub organization or user that owns the repo. */
	public static final String OWNER = "kotogadekiru";
	/** Repository name under {@link #OWNER}. */
	public static final String REPO = "UrsulaGIS-Desktop-Zulu";
	/** Branch whose raw contents and search results are preferred. */
	public static final String BRANCH = "main";
	/** Base URL for raw file downloads ({@code .../BRANCH/}). */
	public static final String RAW_BASE = "https://raw.githubusercontent.com/"
			+ OWNER + "/" + REPO + "/" + BRANCH + "/";
	/** Base URL for the GitHub REST API on this repo. */
	public static final String API_BASE = "https://api.github.com/repos/"
			+ OWNER + "/" + REPO;

	/** Prevents instantiation. */
	private GitHubRepoConfig() {
	}
}
