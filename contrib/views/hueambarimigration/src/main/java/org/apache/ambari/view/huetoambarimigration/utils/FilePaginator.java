/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.huetoambarimigration.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.hadoop.fs.FSDataInputStream;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static java.lang.Math.ceil;

/**
 * Pagination for HDFS file implementation
 */
public class FilePaginator {
  private static int PAGE_SIZE = 1*1024*1024;  // 1MB

  private String filePath;
  private ViewContext context;

  /**
   * Constructor
   * @param filePath Path to file on HDFS
   * @param context View Context instance
   */
  public FilePaginator(String filePath, ViewContext context) {
    this.filePath = filePath;
    this.context = context;
  }

  /**
   * Set page size
   * @param PAGE_SIZE size
   */
  public static void setPageSize(int PAGE_SIZE) {
    FilePaginator.PAGE_SIZE = PAGE_SIZE;
  }

  /**
   * Get page count
   * @return page count
   * @throws IOException
   * @throws InterruptedException
   */

  /**
   * Read one page of size PAGE_SIZE
   * @param page page index
   * @return data in UTF-8
   * @throws IOException
   * @throws InterruptedException
   */

}
