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

import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.client.Row;
import org.apache.ambari.view.hive2.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive2.resources.uploads.TableDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * provides general implementation for parsing JSON,CSV,XML file
 * to generate preview rows, headers and column types
 * also provides TableDataReader for converting any type to CSV.
 */
public abstract class Parser implements IParser {

  protected final static Logger LOG =
    LoggerFactory.getLogger(Parser.class);
  public static final String COLUMN_PREFIX = "column";

  protected Reader reader; // same as CSV reader in this case
  protected ParseOptions parseOptions;
  private int numberOfPreviewRows = 10;

  public Parser(Reader originalReader, ParseOptions parseOptions) {
    this.reader = originalReader;
    this.parseOptions = parseOptions;
  }

  /**
   * returns which datatype is valid for all the values
   */

  /**
   *
   * @param rows : non empty list of rows
   * @param colNum : to detect datatype for this column number.
   * @return data type for that column
   */
  private ColumnDescription.DataTypes getLikelyDataType(List<Row> rows, int colNum) {
    // order of detection BOOLEAN,INT,BIGINT,DOUBLE,DATE,CHAR,STRING
    List<Object> colValues = new ArrayList<>(rows.size());
    for( Row row : rows ){
      colValues.add(row.getRow()[colNum]);
    }

    return ParseUtils.detectHiveColumnDataType(colValues);
  }

  @Override
  public PreviewData parsePreview() {
    LOG.info("generating preview for : {}", this.parseOptions );

    ArrayList<Row> previewRows;
    List<ColumnDescription> header;

    try {
      numberOfPreviewRows = (Integer) parseOptions.getOption(ParseOptions.OPTIONS_NUMBER_OF_PREVIEW_ROWS);
    } catch (Exception e) {
      LOG.debug("Illegal number of preview columns supplied {}",parseOptions.getOption(ParseOptions.OPTIONS_NUMBER_OF_PREVIEW_ROWS) );
    }

    int numberOfRows = numberOfPreviewRows;
    previewRows = new ArrayList<>(numberOfPreviewRows);

    Row headerRow = null;
    Integer numOfCols = null;

    if (parseOptions.getOption(ParseOptions.OPTIONS_HEADER) != null &&
      ( parseOptions.getOption(ParseOptions.OPTIONS_HEADER).equals(ParseOptions.HEADER.FIRST_RECORD.toString()) ||
        parseOptions.getOption(ParseOptions.OPTIONS_HEADER).equals(ParseOptions.HEADER.EMBEDDED.toString())
      )) {
      headerRow = extractHeader();
      numOfCols = headerRow.getRow().length;
    }

    Row r;
    if (iterator().hasNext()) {
      r = iterator().next();
      if( null == numOfCols ) {
        numOfCols = r.getRow().length;
      }
    } else {
      LOG.error("No rows found in the file. returning error.");
      throw new NoSuchElementException("No rows in the file.");
    }

    while (true) {
      // create Header definition from row
      Object[] values = r.getRow();
      Object[] newValues= new Object[numOfCols]; // adds null if less columns detected and removes extra columns if any

      for (int colNum = 0; colNum < numOfCols; colNum++) {
        if(colNum < values.length) {
          newValues[colNum] = values[colNum];
        }else{
          newValues[colNum] = null;
        }
      }

      previewRows.add(new Row(newValues));

      numberOfRows--;
      if (numberOfRows <= 0 || !iterator().hasNext())
        break;

      r = iterator().next();
    }

    if (previewRows.size() <= 0) {
      LOG.error("No rows found in the file. returning error.");
      throw new NoSuchElementException("Does not contain any rows.");
    }

    // find data types.
    header = generateHeader(headerRow,previewRows,numOfCols);

    return new PreviewData(header,previewRows);
  }

  private List<ColumnDescription> generateHeader(Row headerRow,List<Row> previewRows, int numOfCols) {
    List<ColumnDescription> header = new ArrayList<>();

    for (int colNum = 0; colNum < numOfCols; colNum++) {
      ColumnDescription.DataTypes type = getLikelyDataType(previewRows,colNum);
      LOG.info("datatype detected for column {} : {}", colNum, type);

      String colName = COLUMN_PREFIX + (colNum + 1);
      if (null != headerRow)
        colName = (String) headerRow.getRow()[colNum];

      ColumnDescription cd = new ColumnDescriptionImpl(colName, type.toString(), colNum);
      header.add(cd);
    }

    LOG.debug("return headers : {} ", header);
    return header;
  }
}
