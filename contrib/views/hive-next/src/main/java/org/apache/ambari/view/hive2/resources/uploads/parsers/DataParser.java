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

package org.apache.ambari.view.hive2.resources.uploads.parsers;

import org.apache.ambari.view.hive2.client.Row;
import org.apache.ambari.view.hive2.resources.uploads.parsers.csv.opencsv.OpenCSVParser;
import org.apache.ambari.view.hive2.resources.uploads.parsers.json.JSONParser;
import org.apache.ambari.view.hive2.resources.uploads.parsers.xml.XMLParser;

import java.io.Reader;
import java.util.Iterator;

/**
 * Wrapper/Decorator over the Stream parsers.
 * Supports XML/JSON/CSV parsing.
 */
public class DataParser implements IParser {

  private IParser parser;

  public DataParser(Reader reader, ParseOptions parseOptions) throws Exception {
    if (parseOptions.getOption(ParseOptions.OPTIONS_FILE_TYPE).equals(ParseOptions.InputFileType.CSV.toString())) {
      parser = new OpenCSVParser(reader, parseOptions);
    } else if (parseOptions.getOption(ParseOptions.OPTIONS_FILE_TYPE).equals(ParseOptions.InputFileType.JSON.toString())) {
      parser = new JSONParser(reader, parseOptions);
    } else if (parseOptions.getOption(ParseOptions.OPTIONS_FILE_TYPE).equals(ParseOptions.InputFileType.XML.toString())) {
      parser = new XMLParser(reader, parseOptions);
    }
  }

  @Override
  public PreviewData parsePreview() {
    return parser.parsePreview();
  }

  @Override
  public Row extractHeader() {
    return parser.extractHeader();
  }

  @Override
  public void close() throws Exception {
    parser.close();
  }

  @Override
  public Iterator<Row> iterator() {
    return parser.iterator();
  }
}
