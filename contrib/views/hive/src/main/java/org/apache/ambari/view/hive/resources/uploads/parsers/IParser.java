/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads.parsers;

import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.Row;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * Interface defining methods for Parsers that can used for generating preview
 * and uploading table into hive.
 */
public interface IParser extends Iterable<Row> {

  /**
   * @return returns the Reader that can be read to get the table data as CSV Text Data that can be uploaded directly
   * to HDFS
   */
  Reader getTableDataReader();

  PreviewData parsePreview();

  Row extractHeader();

  void close() throws IOException;

}
