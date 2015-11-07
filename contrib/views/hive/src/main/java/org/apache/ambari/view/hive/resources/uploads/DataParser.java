/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.uploads;

import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.Row;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

public class DataParser implements IParser {

  private IParser parser;

  public DataParser(Reader reader, ParseOptions parseOptions) throws IOException {
    if (parseOptions.getOption(ParseOptions.OPTIONS_FILE_TYPE).equals(ParseOptions.FILE_TYPE_CSV)) {
      parser = new CSVParser(reader, parseOptions);
    }
  }

  @Override
  public Reader getCSVReader() {
    return parser.getCSVReader();
  }

  @Override
  public List<ColumnDescription> getHeader() {
    return parser.getHeader();
  }

  @Override
  public List<Row> getPreviewRows() {
    return parser.getPreviewRows();
  }

  @Override
  public void parsePreview() {
    parser.parsePreview();
  }

  @Override
  public Iterator<Row> iterator() {
    return parser.iterator();
  }
}
