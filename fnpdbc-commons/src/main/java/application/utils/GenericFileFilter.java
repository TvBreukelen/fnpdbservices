package application.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

public class GenericFileFilter extends FileFilter implements FilenameFilter {
	/**
	 * Title: GenericFileFilter Description: File selection filter Copyright (c)
	 * 2003-2020
	 *
	 * @author Tom van Breukelen
	 * @version 8+
	 */
	private List<String> myExt = new ArrayList<>();
	private String description;

	public GenericFileFilter(String[] fileExt, String descr) {
		description = descr;
		for (String ext : fileExt) {
			myExt.add("." + ext.toLowerCase());
		}
	}

	@Override
	public boolean accept(File file, String name) {
		return myExt.stream().anyMatch(ext -> ext.equals(".*") || name.endsWith(ext));
	}

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return accept(file, file.getName().toLowerCase());
	}

	@Override
	public String getDescription() {
		return description;
	}
}