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

import java.util.List;

/**
 * Encapsulating preview data from parser.
 */
public class PreviewData {
  private List<ColumnDescription> header;
  private List<Row> previewRows;

  public PreviewData() {
  }

  public PreviewData(List<ColumnDescription> header, List<Row> previewRows) {
    this.header = header;
    this.previewRows = previewRows;
  }

  public List<ColumnDescription> getHeader() {
    return header;
  }

  public void setHeader(List<ColumnDescription> header) {
    this.header = header;
  }

  public List<Row> getPreviewRows() {
    return previewRows;
  }

  public void setPreviewRows(List<Row> previewRows) {
    this.previewRows = previewRows;
  }
}
