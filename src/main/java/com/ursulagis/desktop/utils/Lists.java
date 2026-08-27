package com.ursulagis.desktop.utils;

import java.util.List;
import java.util.stream.Collectors;

import java.util.logging.Logger;
public class Lists {
	private static final Logger logger = Logger.getLogger(Lists.class.getName());

	public static String toString(List<?> l) {
		String separator = ", ";
		String toPrint = l.stream().map(o -> o.toString()).collect(Collectors.joining(separator));
		return toPrint;
	}
	
	public static void println(List<?> l) {
		logger.fine(toString(l));
	}
}
