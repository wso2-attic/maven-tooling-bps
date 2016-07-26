/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.maven.humantask.artifact.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.logging.Log;

public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class);

	public static final String ERROR_CREATING_CORRESPONDING_ZIP_FILE = "Error creating corresponding ZIP file";

	public static void copyDirectory(File srcPath, File dstPath, List<File> filesToBeCopied) throws IOException {

		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				dstPath.mkdir();
			}
			String files[] = srcPath.list();
			if (files != null) {
				for (String file : files) {
					copyDirectory(new File(srcPath, file), new File(dstPath, file), filesToBeCopied);
				}
			}
		} else {
			if (srcPath.exists()) {
				org.apache.commons.io.FileUtils.copyFile(srcPath, dstPath);
			}
		}
	}

	public static List<File> getAllFilesPresentInFolder(File srcPath) {
		List<File> fileList = new ArrayList<File>();
		if (srcPath.isDirectory()) {
			String files[] = srcPath.list();
			if (files != null) {
				for (String file : files) {
					fileList.addAll(getAllFilesPresentInFolder(new File(srcPath, file)));
				}
			}
		} else {
			fileList.add(srcPath);
		}
		return fileList;
	}

	public static File createArchive(Log log, File location, File artifactLocation, String artifactName)
			throws Exception {
		List<File> allFilesPresentInFolder = getAllFilesPresentInFolder(artifactLocation);
		File[] fileArray = new File[allFilesPresentInFolder.size()];
		for (int i = 0; i < allFilesPresentInFolder.size(); i++) {
			fileArray[i] = allFilesPresentInFolder.get(i);
			log.info("FileArray name : " + allFilesPresentInFolder.get(i).getName());
		}
		List<File> bpelValidFileList = getFileList(fileArray);
		if (bpelValidFileList.size() == 0) {
			throw new Exception("The selected location " + location.getName() + "(" + location.toString()
					+ ") does not contain any bpel processes.");
		}

		File targetFolder;
		targetFolder = new File(location.getPath(), "target");
		File bpelDataFolder = new File(targetFolder, "ht-tmp");
		bpelDataFolder.mkdirs();
		File zipFolder = new File(bpelDataFolder, artifactLocation.getName());
		zipFolder.mkdirs();
		copyDirectory(artifactLocation, zipFolder, bpelValidFileList);
		log.info("Copied Size : " + getAllFilesPresentInFolder(zipFolder).size());
		File zipFile = new File(targetFolder, artifactName);
		zipFolder(zipFolder.getAbsolutePath(), zipFile.toString());
		org.apache.commons.io.FileUtils.deleteDirectory(bpelDataFolder);
		return zipFile;

	}

	public static List<File> getFileList(File[] fileList) {
		List<File> list = new ArrayList<File>();
		Collections.addAll(list, fileList);
		return list;
	}

	static public void zipFolder(String srcFolder, String destZipFile) {
		ZipOutputStream zip;
		FileOutputStream fileWriter;
		try {
			fileWriter = new FileOutputStream(destZipFile);
			zip = new ZipOutputStream(fileWriter);
			addFolderContentsToZip(srcFolder, zip);
			zip.flush();
			zip.close();
		} catch (IOException ex) {
			logger.error(ERROR_CREATING_CORRESPONDING_ZIP_FILE, ex);
		}
	}

	static private void addToZip(String path, String srcFile, ZipOutputStream zip) {

		File folder = new File(srcFile);

		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			if (!srcFile.equals(".project")) {
				byte[] buf = new byte[1024];
				int len;
				try {
					FileInputStream in = new FileInputStream(srcFile);
					String location = folder.getName();
					if (!path.equalsIgnoreCase("")) {
						location = path + File.separator + folder.getName();
					}
					zip.putNextEntry(new ZipEntry(location));
					while ((len = in.read(buf)) > 0) {
						zip.write(buf, 0, len);
					}
					in.close();
				} catch (IOException e) {
					logger.error(ERROR_CREATING_CORRESPONDING_ZIP_FILE, e);
				}
			}
		}
	}

	static private void addFolderContentsToZip(String srcFolder, ZipOutputStream zip) {
		File folder = new File(srcFolder);
		String fileListe[] = folder.list();
		int i = 0;
		if (fileListe != null) {
			while (i < fileListe.length) {
				addToZip("", srcFolder + File.separator + fileListe[i], zip);
				i++;
			}
		}
	}

	static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) {
		File folder = new File(srcFolder);
		String fileListe[] = folder.list();
		int i = 0;
		while (i < fileListe.length) {
			String newPath = folder.getName();
			if (!path.equalsIgnoreCase("")) {
				newPath = path + File.separator + newPath;
			}
			addToZip(newPath, srcFolder + File.separator + fileListe[i], zip);
			i++;
		}

	}
}