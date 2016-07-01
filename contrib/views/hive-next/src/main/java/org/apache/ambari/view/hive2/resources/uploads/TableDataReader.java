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

package org.apache.ambari.view.hive2.resources.uploads;

import com.opencsv.CSVWriter;
import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.client.Row;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Takes row iterator as input.
 * iterate over rows and creates a CSV formated stream separating rows by endline "\n"
 * Note : column values should not contain "\n".
 */
public class TableDataReader extends Reader {

  private static final int CAPACITY = 1024;
  private final List<ColumnDescriptionImpl> header;
  private StringReader stringReader = new StringReader("");

  private Iterator<Row> iterator;
  private boolean encode = false;
  public static final char CSV_DELIMITER = '\001';

  public TableDataReader(Iterator<Row> rowIterator, List<ColumnDescriptionImpl> header, boolean encode) {
    this.iterator = rowIterator;
    this.encode = encode;
    this.header = header;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {

    int totalLen = len;
    int count = 0;
    do {
      int n = stringReader.read(cbuf, off, len);

      if (n != -1) {
        // n  were read
        len = len - n; // len more to be read
        off = off + n; // off now shifted to n more
        count += n;
      }

      if (count == totalLen) return count; // all totalLen characters were read

      if (iterator.hasNext()) { // keep reading as long as we keep getting rows
        StringWriter stringWriter = new StringWriter(CAPACITY);
        CSVWriter csvPrinter = new CSVWriter(stringWriter,CSV_DELIMITER);
        Row row = iterator.next();
        // encode values so that \n and \r are overridden
        Object[] columnValues = row.getRow();
        String[] columns = new String[columnValues.length];

        for(int i = 0; i < columnValues.length; i++){
          String type = header.get(i).getType();
          if(this.encode &&
              (
                ColumnDescription.DataTypes.STRING.toString().equals(type)
                || ColumnDescription.DataTypes.VARCHAR.toString().equals(type)
                || ColumnDescription.DataTypes.CHAR.toString().equals(type)
              )
            ){
            columns[i] = Hex.encodeHexString(((String)columnValues[i]).getBytes()); //default charset
          }else {
            columns[i] = (String) columnValues[i];
          }
        }

        csvPrinter.writeNext(columns,false);
        stringReader.close(); // close the old string reader
        stringReader = new StringReader(stringWriter.getBuffer().toString());
        csvPrinter.close();
        stringWriter.close();
      } else {
        return count == 0 ? -1 : count;
      }
    } while (count < totalLen);

    return count;
  }

  @Override
  public void close() throws IOException {

  }
}
