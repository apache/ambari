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

public class DataParserXMLTest {

  @Test
  public void testParsePreviewXML() throws IOException {
    String str = "<table>" +
            "<row>" +
            "<col name=\"col1\">row1-col1-Value</col>" +
            "<col name=\"col2\">row1-col2-Value</col>" +
            "<col name=\"col3\">row1-col3-Value</col>" +
            "<col name=\"col4\">10</col>" +
            "<col name=\"col5\">11</col>" +
            "</row>" +
            "<row>" +
            "<col name=\"col1\">row2-col1-Value</col>" +
            "<col name=\"col2\">row2-col2-Value</col>" +
            "<col name=\"col3\">row2-col3-Value</col>" +
            "<col name=\"col4\">20</col>" +
            "<col name=\"col5\">21</col>" +
            "</row>" +
            "</table>";

    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(3, pd.getPreviewRows().size()); // header row + preview rows
      Assert.assertEquals(5, pd.getHeader().size());
      ColumnDescription[] cd = {new ColumnDescriptionImpl("col1", ColumnDescriptionShort.DataTypes.STRING.toString(), 0),
              new ColumnDescriptionImpl("col2", ColumnDescriptionShort.DataTypes.STRING.toString(), 1),
              new ColumnDescriptionImpl("col3", ColumnDescriptionShort.DataTypes.STRING.toString(), 2),
              new ColumnDescriptionImpl("col4", ColumnDescriptionShort.DataTypes.INT.toString(), 3),
              new ColumnDescriptionImpl("col5", ColumnDescriptionShort.DataTypes.INT.toString(), 4)
      };

      Row row1 = new Row(new Object[]{"col1", "col2", "col3", "col4", "col5"});
      Row row2 = new Row(new Object[]{"row1-col1-Value", "row1-col2-Value", "row1-col3-Value", "10", "11"});
      Row row3 = new Row(new Object[]{"row2-col1-Value", "row2-col2-Value", "row2-col3-Value", "20", "21"});

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
   * additional columns in rows of XML are ignored.
   * number of columns are decided by the first row of the table and here second row contains more columns so those are ignored.
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVMoreColumns() throws IOException {
    String str ="<table>" +
            "<row>" +
            "<col name=\"col1\">row1-col1-Value</col>" +
            "<col name=\"col2\">row1-col2-Value</col>" +
            "<col name=\"col3\">row1-col3-Value</col>" +
            "<col name=\"col4\">10</col>" +
            "<col name=\"col5\">11</col>" +
            "</row>" +
            "<row>" +
            "<col name=\"col1\">row2-col1-Value</col>" +
            "<col name=\"col2\">row2-col2-Value</col>" +
            "<col name=\"col3\">row2-col3-Value</col>" +
            "<col name=\"col99\">row2-col99-Value</col>" +  // extra colummn
            "<col name=\"col100\">row2-col100-Value</col>" +  // extra column
            "<col name=\"col4\">20</col>" +
            "<col name=\"col5\">21</col>" +
            "</row>" +
            "</table>";

    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());

    DataParser dp = null;
    try {
      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();

      Row row2 = new Row(new Object[]{"row2-col1-Value","row2-col2-Value","row2-col3-Value","20","21"});
      Assert.assertArrayEquals("More number of columns do not give correct result.", row2.getRow(), pd.getPreviewRows().get(1).getRow());
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * less columns in xml makes them null.
   * number of columns are decided by the first row of the table and here second row does not contain col99 and col100
   * columns so those are set to null.
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVLessColumns() throws IOException {
    String str = "<table>" +
            "<row>" +
            "<col name=\"col1\">row1-col1-Value</col>" +
            "<col name=\"col2\">row1-col2-Value</col>" +
            "<col name=\"col3\">row1-col3-Value</col>" +
            "<col name=\"col99\">row2-col99-Value</col>" +  // extra colummn
            "<col name=\"col100\">row2-col100-Value</col>" +  // extra column
            "<col name=\"col4\">10</col>" +
            "<col name=\"col5\">11</col>" +
            "</row>" +
            "<row>" +
            "<col name=\"col1\">row2-col1-Value</col>" +
            "<col name=\"col2\">row2-col2-Value</col>" +
            "<col name=\"col3\">row2-col3-Value</col>" +
            "<col name=\"col4\">20</col>" +
            "<col name=\"col5\">21</col>" +
            "</row>" +
            "</table>";

    StringReader sr = new StringReader(str);
    DataParser dp = null;
    try {
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
      parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

      dp = new DataParser(sr, parseOptions);

      PreviewData pd = dp.parsePreview();

      Row row2 = new Row(new Object[]{"row2-col1-Value","row2-col2-Value","row2-col3-Value",null,null,"20","21"});
      Assert.assertArrayEquals("Less number of columns do not give correct result.", row2.getRow(), pd.getPreviewRows().get(2).getRow());
    } finally {
      if (null != dp)
        dp.close();

      sr.close();
    }
  }

  /**
   * illegal xml format gives error. adding illegal tag gives error
   *
   * @throws IOException
   */
  @Test(expected = IllegalArgumentException.class)
  public void testWrongXMLFormat() throws IOException {
    String str = "<table>" +
            "<row>" +
            "<ccc></ccc>" +   // illegal tag.
            "<col name=\"col1\">row1-col1-Value</col>" +
            "<col name=\"col2\">row1-col2-Value</col>" +
            "<col name=\"col3\">row1-col3-Value</col>" +
            "<col name=\"col99\">row2-col99-Value</col>" +  // extra colummn
            "<col name=\"col100\">row2-col100-Value</col>" +  // extra column
            "<col name=\"col4\">10</col>" +
            "<col name=\"col5\">11</col>" +
            "</row>" +
            "<row>" +
            "<col name=\"col1\">row2-col1-Value</col>" +
            "<col name=\"col2\">row2-col2-Value</col>" +
            "<col name=\"col3\">row2-col3-Value</col>" +
            "<col name=\"col4\">20</col>" +
            "<col name=\"col5\">21</col>" +
            "</row>" +
            "</table>";
    DataParser dp = null;
    StringReader sr = new StringReader(str);

    try {
      ParseOptions parseOptions = new ParseOptions();
      parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
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
