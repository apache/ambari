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
import org.apache.ambari.view.hive.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive.client.Row;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

/**
 * Parses the given Reader and extracts headers and rows, and detect datatypes of columns
 */
public class CSVParser implements IParser {

  static class CSVIterator implements Iterator<Row> {

    private Iterator<CSVRecord> iterator;

    public CSVIterator(Iterator<CSVRecord> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Row next() {
      CSVRecord row = iterator.next();
      Object[] values = new Object[row.size()];
      for (int i = 0; i < values.length; i++) {
        values[i] = row.get(i);
      }
      Row r = new Row(values);
      return r;
    }

    @Override
    public void remove() {
      this.iterator.remove();
    }
  }

  private Reader originalReader; // same as CSV reader in this case
  private ParseOptions parseOptions;
  private CSVIterator iterator;
  private List<Row> previewRows;
  private List<ColumnDescription> header;
  private boolean isHeaderFirstRow = false;
  private int numberOfPreviewRows = 10;
  private org.apache.commons.csv.CSVParser parser;

  public CSVParser(Reader reader, ParseOptions parseOptions) throws IOException {
    this.originalReader = reader;
    this.parseOptions = parseOptions;
    // always create without headers
    parser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.EXCEL);
    iterator = new CSVIterator(parser.iterator());
  }

  public void parsePreview() {
    try {
      numberOfPreviewRows = (Integer) parseOptions.getOption(ParseOptions.OPTIONS_NUMBER_OF_PREVIEW_ROWS);
    } catch (Exception e) {
    }

    int numberOfRows = numberOfPreviewRows;
    previewRows = new ArrayList<Row>(numberOfPreviewRows); // size including the header.

    Row headerRow = null;
    if (parseOptions.getOption(ParseOptions.OPTIONS_HEADER).equals(ParseOptions.HEADER_FIRST_RECORD)) {
      if (!this.iterator().hasNext()) {
        throw new NoSuchElementException("Cannot parse Header");
      }
      isHeaderFirstRow = true;
      headerRow = iterator().next();
      previewRows.add(headerRow);
    }

    // find data types.
    int[][] typeCounts = null;
    Row r = null;
    int numOfCols = 0;
    if (iterator().hasNext()) {
      r = iterator().next();
      numOfCols = r.getRow().length;
      typeCounts = new int[numOfCols][ColumnDescription.DataTypes.values().length];
    } else {
      throw new NoSuchElementException("No rows in the csv.");
    }

    while (true) {
      // create Header definition from row
      Object[] values = r.getRow();
      previewRows.add(r);

      if (values.length != numOfCols)
        throw new IllegalArgumentException("Illegal number of cols for row : " + r);

      for (int colNum = 0; colNum < values.length; colNum++) {
        // detect type
        ColumnDescription.DataTypes type = ParseUtils.detectHiveDataType(values[colNum]);
        typeCounts[colNum][type.ordinal()]++;
      }
      numberOfRows--;
      if (numberOfRows <= 0 || !iterator().hasNext())
        break;

      r = iterator().next();
    }
    ;

    if (previewRows.size() <= 0)
      throw new NoSuchElementException("Does not contain any rows.");

    header = new ArrayList<ColumnDescription>(numOfCols);
    for (int colNum = 0; colNum < numOfCols; colNum++) {
      int dataTypeId = getLikelyDataType(typeCounts, colNum);
      ColumnDescription.DataTypes type = ColumnDescription.DataTypes.values()[dataTypeId];
      String colName = "Column" + colNum;
      if (null != headerRow)
        colName = (String) headerRow.getRow()[colNum];

      ColumnDescription cd = new ColumnDescriptionImpl(colName, type.toString(), colNum);
      header.add(cd);
    }
  }

  /**
   * returns which datatype was detected for the maximum number of times in the given column
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
  public Reader getCSVReader() {
    return originalReader;
  }

  @Override
  public List<ColumnDescription> getHeader() {
    return header;
  }

  @Override
  public List<Row> getPreviewRows() {
    return this.previewRows;
  }

  public Iterator<Row> iterator() {
    return iterator; // only one iterator per parser.
  }
}
