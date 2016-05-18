/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder.input.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import org.apache.log4j.Logger;

public enum LogsearchReaderFactory {
  INSTANCE;
  private static Logger logger = Logger
    .getLogger(LogsearchReaderFactory.class);

  /**
   * @param fileName
   * @return
   * @throws FileNotFoundException
   */
  public Reader getReader(File file) throws FileNotFoundException {
    logger.debug("Inside reader factory for file:" + file);
    if (GZIPReader.isValidFile(file.getAbsolutePath())) {
      logger.info("Reading file " + file + " as gzip file");
      return new GZIPReader(file.getAbsolutePath());
    } else {
      return new FileReader(file);
    }
  }

}
