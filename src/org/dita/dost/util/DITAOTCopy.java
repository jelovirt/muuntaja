/*
 * This file is part of the DITA Open Toolkit project hosted on
 * Sourceforge.net. See the accompanying license.txt file for 
 * applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2005 All Rights Reserved.
 */
package org.dita.dost.util;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import org.dita.dost.log.DITAOTJavaLogger;

/**
 * Class description goes here. 
 *
 * @author Wu, Zhi Qiang
 */
public class DITAOTCopy extends Task {
	private String includes = null;
	private String relativePaths = null;
	private String destDir = null;  // the destination directory
	private DITAOTJavaLogger logger = new DITAOTJavaLogger();
	
	/**
	 * Default Constructor.
	 * 
	 */
	public DITAOTCopy(){
	}
	
	/**
	 * Set the copy files.
	 * @param incld The includes to set.
	 */
	public void setIncludes(String incld) {
		this.includes = incld;
	}
	
    /**
     * Set the destination directory.
     * @param destdir the destination directory.
     */
    public void setTodir(String destdir) {
        this.destDir = destdir;
    }
    
	/**
	 * Set the relative path from output directory.
	 * @param relPaths the relative path .
	 */
	public void setRelativePaths(String relPaths) {
		this.relativePaths = relPaths;
	}

	/**
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		FileUtils fileUitls = FileUtils.newFileUtils();
		StringTokenizer tokenizer;
		StringTokenizer pathTokenizer;
		if (includes == null) {
			return;
		}
		tokenizer = new StringTokenizer(includes, Constants.COMMA);
		if (relativePaths == null) {
			try {
				while (tokenizer.hasMoreTokens()) {
					File srcFile = new File(tokenizer.nextToken());
					if (srcFile.exists()) {
						File destFile = new File(destDir, srcFile.getName());
						fileUitls.copyFile(srcFile, destFile);
					}
				}
			} catch (IOException e) {
				logger.logException(e);
			}
		}else{
			pathTokenizer = new StringTokenizer(relativePaths, Constants.COMMA);
			StringBuffer realDest=null;
			try {
				while (tokenizer.hasMoreTokens()) {
					realDest=new StringBuffer();
					//destDir is the ouput dir
					//pathTokenizer is the relative path with the filename
					if(destDir!=null && destDir.trim().length()>0){
						realDest.append(destDir).append(File.separator).append(pathTokenizer.nextToken());
					}
					File srcFile = new File(tokenizer.nextToken());
					if (srcFile.exists()) {
						File destFile = new File(realDest.toString());
						fileUitls.copyFile(srcFile, destFile);
					}
				}
			} catch (IOException e) {
				logger.logException(e);
			}
		}
	}

}