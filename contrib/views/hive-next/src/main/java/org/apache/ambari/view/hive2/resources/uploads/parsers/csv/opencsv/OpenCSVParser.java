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
package org.apache.ambari.view.hive2.resources.uploads.parsers.csv.opencsv;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.ambari.view.hive2.client.Row;
import org.apache.ambari.view.hive2.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive2.resources.uploads.parsers.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

/**
 * Parses the given Reader which contains CSV stream and extracts headers and rows
 */
public class OpenCSVParser extends Parser {
  private Row headerRow;
  private OpenCSVIterator iterator;
  private CSVReader csvReader = null;
  private final static Logger LOG =
    LoggerFactory.getLogger(OpenCSVParser.class);

  public OpenCSVParser(Reader reader, ParseOptions parseOptions) throws IOException {
    super(reader, parseOptions);
    CSVParserBuilder csvParserBuilder = new CSVParserBuilder();
    CSVReaderBuilder builder =  new CSVReaderBuilder(reader);

    Character delimiter = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_DELIMITER);
    if(delimiter != null){
      LOG.info("setting delimiter as {}", delimiter);
      csvParserBuilder = csvParserBuilder.withSeparator(delimiter);
    }

    Character quote = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_QUOTE);
    if( null != quote ){
      LOG.info("setting Quote char : {}", quote);
      csvParserBuilder = csvParserBuilder.withQuoteChar(quote);
    }

    Character escapeChar = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_ESCAPE_CHAR);
    if( null != escapeChar ){
      LOG.info("setting escapeChar : {}", escapeChar);
      csvParserBuilder = csvParserBuilder.withEscapeChar(escapeChar);
    }

    builder.withCSVParser(csvParserBuilder.build());
    this.csvReader = builder.build();
    iterator = new OpenCSVIterator(this.csvReader.iterator());

    String optHeader =  (String)parseOptions.getOption(ParseOptions.OPTIONS_HEADER);
    if(optHeader != null){
      if(optHeader.equals(ParseOptions.HEADER.FIRST_RECORD.toString())) {
        this.headerRow = iterator().hasNext() ? iterator.next() : new Row(new Object[]{});
      }
    }

  }

  @Override
  public Row extractHeader() {
    return headerRow;
  }

  @Override
  public void close() throws Exception {
    this.csvReader.close();
  }

  public Iterator<Row> iterator() {
    return iterator; // only one iterator per parser.
  }
}
