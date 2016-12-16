/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSFileUtils {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(HDFSFileUtils.class);
	private ViewContext viewContext;

	public HDFSFileUtils(ViewContext viewContext) {
		super();
		this.viewContext = viewContext;
	}

	public boolean fileExists(String path) {
		try {
			return getHdfsgetApi().exists(path);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public FSDataInputStream read(String filePath) throws IOException {
		FSDataInputStream is;
		try {
			is = getHdfsgetApi().open(filePath);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return is;
	}

	public String writeToFile(String filePath, String content, boolean overwrite)
			throws IOException {
		FSDataOutputStream fsOut;
		try {
			fsOut = getHdfsgetApi().create(filePath, overwrite);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		fsOut.write(content.getBytes());
		fsOut.close();
		return filePath;
	}

	public void deleteFile(String filePath) throws IOException {
		try {
			getHdfsgetApi().delete(filePath, false);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private HdfsApi getHdfsgetApi() {
		try {
			return HdfsUtil.connectToHDFSApi(viewContext);
		} catch (Exception ex) {
			LOGGER.error("Error in getting HDFS Api", ex);
			throw new RuntimeException(
					"HdfsApi connection failed. Check \"webhdfs.url\" property",
					ex);
		}
	}

	public FileStatus getFileStatus(String filePath) {
		try {
			return getHdfsgetApi().getFileStatus(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

}
