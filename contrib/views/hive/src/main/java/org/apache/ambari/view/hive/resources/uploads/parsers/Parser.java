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
import org.apache.ambari.view.hive.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive.resources.uploads.TableDataReader;

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

  protected Reader reader; // same as CSV reader in this case
  protected ParseOptions parseOptions;
  private int numberOfPreviewRows = 10;

  public Parser(Reader originalReader, ParseOptions parseOptions) {
    this.reader = originalReader;
    this.parseOptions = parseOptions;
  }

  /**
   * returns which datatype was detected for the maximum number of times in the given column
   *
   * @param typeCounts
   * @param colNum
   * @return
   */
  private int getLikelyDataType(int[][] typeCounts, int colNum) {
    int[] colArray = typeCounts[colNum];
    int maxIndex = 0;
    int i = 1;
    for (; i < colArray.length; i++) {
      if (colArray[i] > colArray[maxIndex])
        maxIndex = i;
    }

    return maxIndex;
  }

  @Override
  public Reader getTableDataReader() {
    return new TableDataReader(this.iterator());
  }

  @Override
  public PreviewData parsePreview() {
    List<Row> previewRows;
    List<ColumnDescription> header;

    try {
      numberOfPreviewRows = (Integer) parseOptions.getOption(ParseOptions.OPTIONS_NUMBER_OF_PREVIEW_ROWS);
    } catch (Exception e) {
    }

    int numberOfRows = numberOfPreviewRows;
    previewRows = new ArrayList<Row>(numberOfPreviewRows + 1); // size including the header.

    Row headerRow = null;
    Integer numOfCols = null;
    int[][] typeCounts = null;

    if (parseOptions.getOption(ParseOptions.OPTIONS_HEADER) != null && parseOptions.getOption(ParseOptions.OPTIONS_HEADER).equals(ParseOptions.HEADER.FIRST_RECORD.toString())) {
      if (!this.iterator().hasNext()) {
        throw new NoSuchElementException("Cannot parse Header");
      }
      headerRow = extractHeader();
      numOfCols = headerRow.getRow().length;
      typeCounts = new int[numOfCols][ColumnDescription.DataTypes.values().length];
      previewRows.add(headerRow);
    }

    // find data types.

    Row r;
    if (iterator().hasNext()) {
      r = iterator().next();
      if( null == numOfCols ) {
        numOfCols = r.getRow().length;
        typeCounts = new int[numOfCols][ColumnDescription.DataTypes.values().length];
      }
    } else {
        throw new NoSuchElementException("No rows in the file.");
    }

    while (true) {
      // create Header definition from row
      Object[] values = r.getRow();

      Object[] newValues= new Object[numOfCols]; // adds null if less columns detected and removes extra columns if any

      for (int colNum = 0; colNum < numOfCols; colNum++) {
        if(colNum < values.length) {
          // detect type
          ColumnDescription.DataTypes type = ParseUtils.detectHiveDataType(values[colNum]);
          typeCounts[colNum][type.ordinal()]++;
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

    if (previewRows.size() <= 0)
      throw new NoSuchElementException("Does not contain any rows.");

    header = new ArrayList<>(numOfCols);
    for (int colNum = 0; colNum < numOfCols; colNum++) {
      int dataTypeId = getLikelyDataType(typeCounts, colNum);
      ColumnDescription.DataTypes type = ColumnDescription.DataTypes.values()[dataTypeId];
      String colName = "Column" + colNum;
      if (null != headerRow)
        colName = (String) headerRow.getRow()[colNum];

      ColumnDescription cd = new ColumnDescriptionImpl(colName, type.toString(), colNum);
      header.add(cd);
    }

    return new PreviewData(header,previewRows);
  }
}
