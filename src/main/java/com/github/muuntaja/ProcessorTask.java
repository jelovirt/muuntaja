package com.github.muuntaja;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;


public class ProcessorTask extends Task {

	private File resource;
	private File temp;
	private Boolean ot = Boolean.TRUE;
	private File start;
	private FileSet fileset;
	
	@Override
	public void execute() throws BuildException {
		if (fileset != null) {
			for (final String s: fileset.getDirectoryScanner().getIncludedFiles()) {
				run(new File(fileset.getDir(), s), fileset.getDir());
			}
		} else {
			run(start, start.getParentFile());
		}
	}
	
	private void run(final File input, final File baseDir) {
		Processor p = new Processor(resource, temp, ot.booleanValue());
		p.run(input.toURI());
	}
	
	public void setResource(File resource) {
		this.resource = resource;
	}

	public void setTemp(File temp) {
		this.temp = temp;
	}

	public void setFile(File start) {
		this.start = start;
	}

	public void setCompatibility(Boolean ot) {
		this.ot = ot;
	}
	
	public void addFileset(FileSet fileset) {
		this.fileset = fileset;
	}
}
