package com.github.muuntaja;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import edu.umd.cs.findbugs.annotations.NonNull;

public class ProcessorTask extends Task {

	private File baseDir;
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
	
	private void run(@NonNull final File input, @NonNull final File base) {
		Logger logger = Logger.getAnonymousLogger();
		logger.setUseParentHandlers(false);
		logger.addHandler(new AntHandler(getProject(), this));
		logger.setLevel(Level.ALL);
		Processor p = new OTProcessor(baseDir, temp, logger);
		p.run(input.toURI());
	}
	
	@Override
	public String getTaskName() {
		return "muuntaja";
	} 
	
	public void setBasedir(File baseDir) {
		this.baseDir = baseDir;
	}
	
	public void setTempdir(File temp) {
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

	private static class AntHandler extends Handler {
		
		private final Project project;
		private final Task task;
		
		AntHandler(final Project project, final Task task) {
			super();
			this.project = project;
			this.task = task;
		}
		
		@Override
		public void publish(LogRecord record) {
			int level;
			int l = record.getLevel().intValue();
			if (Level.SEVERE.intValue() == l) {
				level = Project.MSG_ERR;
			} else if (Level.WARNING.intValue() == l) {
				level = Project.MSG_WARN;
			} else if (Level.INFO.intValue() == l) {
				level = Project.MSG_INFO;
			} else if (Level.CONFIG.intValue() == l) {
				level = Project.MSG_VERBOSE;
			} else if (Level.FINE.intValue() == l) {
				level = Project.MSG_VERBOSE;
			} else if (Level.FINER.intValue() == l) {
				level = Project.MSG_DEBUG;
			} else if (Level.FINEST.intValue() == l) {
				level = Project.MSG_DEBUG;
			} else {
				level = Project.MSG_INFO;
			}
			project.log(task, record.getMessage(), level);			
		}

		@Override
		public void flush() {
			// NOOP
		}

		@Override
		public void close() throws SecurityException {
			// NOOP
		}
		
		
	}
	
}
