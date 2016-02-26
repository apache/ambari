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

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
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
import java.io.PrintWriter;
import java.io.StringReader;

public class DataParserCSVTest {
  @Test
  public void testParsePreviewCSV() throws IOException {
    String str = "1,a\n" +
            "2,b\n" +
            "3,c\n";
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(3, pd.getPreviewRows().size());
      Assert.assertEquals(2, pd.getHeader().size());
      ColumnDescription[] cd = {new ColumnDescriptionImpl("1", ColumnDescriptionShort.DataTypes.INT.toString(), 0),
              new ColumnDescriptionImpl("a", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1)};

      Object cols1[] = new Object[2];
      cols1[0] = "1";
      cols1[1] = "a";
      Row row1 = new Row(cols1);

      Object cols2[] = new Object[2];
      cols2[0] = "2";
      cols2[1] = "b";
      Row row2 = new Row(cols2);

      Object cols3[] = new Object[2];
      cols3[0] = "3";
      cols3[1] = "c";
      Row row3 = new Row(cols3);

      Row[] rows = {row1, row2, row3};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * more number of columns in a row => igore the extra columns. Number of columns is decided by the first row.
   * If other row contains more columns then those columns will be ignored
   * Here first row has 2 columns and second row has 3 columns so the value 'x' is ignored
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVMoreColumns() throws IOException {
    String str = "1,a\n" +
            "2,b,x\n" +  // contains 3 cols, more number of columns
            "3,c\n";
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Row row2 = new Row(new Object[]{"2","b"});

      Assert.assertArrayEquals("Additional columns not properly handled.", row2.getRow(),pd.getPreviewRows().get(1).getRow());
    } finally {
      if (null != dp) {
        dp.close();
      }

      sr.close();
    }
  }

  /**
   * less number of columns => treat missing values as null. Number of columns is decided by the first row of the table
   * if other rows has less number of columns then it treats other columns as null
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVLessColumns() throws IOException {
    String str = "1,a\n" +
            "2\n" +  // contains 1 col, less number of columns
            "3,c\n";
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
//    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Missing value not detected as null.",pd.getPreviewRows().get(1).getRow()[1],null);
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * empty values are treated as empty string
   * @throws IOException
   */
  @Test
  public void testEmptyColumn() throws IOException {
    String str = "1,a,x\n" +
            "2,,y\n" +  // contains 1 col, less number of columns
            "3,c,z\n";
//    System.out.println("str : " + str);
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(1).getRow()[1],"");
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * empty values are treated as empty string
   * @throws IOException
   */
  @Test
  public void testLastEmptyColumn() throws IOException {
    String str = "1,a,x\n" +
            "2,,\n" +  // contains 1 col, less number of columns
            "3,c,z\n";
//    System.out.println("str : " + str);
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(1).getRow()[1],"");
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(1).getRow()[2],"");
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }
}
