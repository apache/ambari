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

package org.apache.ambari.view.hive.resources.upload;

import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive.client.Row;
import org.apache.ambari.view.hive.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive.resources.uploads.parsers.DataParser;
import org.apache.ambari.view.hive.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive.resources.uploads.parsers.PreviewData;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class DataParserJSONTest {

  @Test
  public void testParsePreviewJSON() throws IOException {
    String str = "[ {\"col1\" : \"a\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"4.4\" },"
            + "{\"col1\": \"b\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"5.4\" },"
            + "{\"col1\": \"c\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"6.4\" },"
            + "{\"col1\": \"d\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"7.4\" },"
            + "{\"col1\": \"e\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"8.4\" },"
            + "{\"col1\": \"f\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"9.4\" },"
            + "{\"col1\": \"g\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"10.4\" },"
            + "{\"col1\": \"h\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"11.4\" },"
            + "{\"col1\": \"i\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"4\" },"
            + "{\"col1\": \"j\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"5\" },"
            + "{\"col1\": \"k\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"6\" },"
            + "{\"col1\": \"l\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"7\" },"
            + "{\"col1\": \"m\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"24.4\" },"
            + "{\"col1\": \"n\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"14.4\" },"
            + "{\"col1\": \"o\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"34.4\" },"
            + "{\"col1\": \"p\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"44.4\" },"
            + "{\"col1\": \"q\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"54.4\" },"
            + "{\"col1\": \"r\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"64.4\" }"
            + "]";

    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.JSON.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_NUMBER_OF_PREVIEW_ROWS, 7);

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(8, pd.getPreviewRows().size()); // header row + preview rows
      Assert.assertEquals(14, pd.getHeader().size());
      ColumnDescription[] cd = {new ColumnDescriptionImpl("col1", ColumnDescriptionShort.DataTypes.CHAR.toString(), 0),
              new ColumnDescriptionImpl("col2", ColumnDescriptionShort.DataTypes.STRING.toString(), 1),
              new ColumnDescriptionImpl("col3", ColumnDescriptionShort.DataTypes.STRING.toString(), 2),
              new ColumnDescriptionImpl("col4", ColumnDescriptionShort.DataTypes.STRING.toString(), 3),
              new ColumnDescriptionImpl("col5", ColumnDescriptionShort.DataTypes.STRING.toString(), 4),
              new ColumnDescriptionImpl("col6", ColumnDescriptionShort.DataTypes.STRING.toString(), 5),
              new ColumnDescriptionImpl("col7", ColumnDescriptionShort.DataTypes.STRING.toString(), 6),
              new ColumnDescriptionImpl("col8", ColumnDescriptionShort.DataTypes.STRING.toString(), 7),
              new ColumnDescriptionImpl("col9", ColumnDescriptionShort.DataTypes.STRING.toString(), 8),
              new ColumnDescriptionImpl("col10", ColumnDescriptionShort.DataTypes.STRING.toString(), 9),
              new ColumnDescriptionImpl("col11", ColumnDescriptionShort.DataTypes.STRING.toString(), 10),
              new ColumnDescriptionImpl("col12", ColumnDescriptionShort.DataTypes.STRING.toString(), 11),
              new ColumnDescriptionImpl("col13", ColumnDescriptionShort.DataTypes.STRING.toString(), 12),
              new ColumnDescriptionImpl("col14", ColumnDescriptionShort.DataTypes.DOUBLE.toString(), 13)};

      Row row1 = new Row(new Object[]{"col1", "col2", "col3", "col4", "col5", "col6", "col7", "col8", "col9", "col10", "col11", "col12", "col13", "col14"});
      Row row2 = new Row(new Object[]{"a", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "4.4"});
      Row row3 = new Row(new Object[]{"b", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "5.4"});
      Row row4 = new Row(new Object[]{"c", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "6.4"});
      Row row5 = new Row(new Object[]{"d", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "7.4"});
      Row row6 = new Row(new Object[]{"e", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "8.4"});
      Row row7 = new Row(new Object[]{"f", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "9.4"});
      Row row8 = new Row(new Object[]{"g", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "10.4"});

      Row[] rows = {row1, row2, row3, row4, row5, row6, row7, row8};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * additional columns in rows of JSON are ignored.
   *
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVMoreColumns() throws IOException {
    String str = "[ {\"col1\" : \"a\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"4.4\" },"
            + "{\"col1\": \"b\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" , \"col14\" : \"43.4\" ,\"col15\" : \"asafsfa\" },"
            + "{\"col1\": \"c\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"6.4\" },"
            + "{\"col1\": \"d\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"7.4\" }"
            + "]";

    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.JSON.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();

      Row row2 = new Row(new Object[]{"b", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "abcd", "43.4"});
      Assert.assertArrayEquals("More number of columns do not give correct result.", row2.getRow(), pd.getPreviewRows().get(1).getRow());
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * less columns in json makes them null.
   *
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVLessColumns() throws IOException {
    String str = "[ " +
            "{\"col1\" : \"a\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"4.4\" },"
            + "{\"col1\": \"b\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\"  },"
            + "{\"col1\": \"c\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"6.4\" },"
            + "{\"col1\": \"d\", \n\"col2\": \"abcd\" ,\"col3\": \"abcd\" ,\"col4\": \"abcd\" ,\"col5\": \"abcd\" ,\"col6\": \"abcd\" ,\"col7\": \"abcd\" ,\"col8\": \"abcd\" ,\"col9\": \"abcd\" ,\"col10\": \"abcd\" ,\"col11\": \"abcd\" ,\"col12\" : \"abcd\" ,\"col13\" : \"abcd\" ,\"col14\" : \"7.4\" }"
            + "]";

    StringReader sr = new StringReader(str);
    DataParser dp = null;
    try {
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.JSON.toString());
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();

      Assert.assertNull(pd.getPreviewRows().get(2).getRow()[13]);
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * illegal json format gives error
   *
   * @throws IOException
   */
  @Test(expected = IllegalArgumentException.class)
  public void testWrongJsonFormat() throws IOException {
    String str = "[ " +
            "{\"col1\" : \"a\", \n\"col2\": \"abcd\" },"
            + "{\"col1\": \"b\", \n\"col2\": \"abcd\" },"
            + "{\"col1\": \"c\", \n\"col2\": \"abcd\"  },"
            + "{\"col1\": \"d\",, \n\"col2\": \"abcd\"  }"       // extra comma in this line
            + "]";
    DataParser dp = null;
    StringReader sr = new StringReader(str);

    try {
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.JSON.toString());
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }
}
