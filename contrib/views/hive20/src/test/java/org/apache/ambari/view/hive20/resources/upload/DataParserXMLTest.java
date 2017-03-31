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

package org.apache.ambari.view.hive20.resources.upload;

import org.apache.ambari.view.hive20.client.ColumnDescription;
import org.apache.ambari.view.hive20.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.resources.uploads.parsers.DataParser;
import org.apache.ambari.view.hive20.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive20.resources.uploads.parsers.PreviewData;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class DataParserXMLTest {

  @Test
  public void testParsePreviewXML() throws Exception {
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


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());


    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions);
      ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(2, pd.getPreviewRows().size()); // header row + preview rows
      Assert.assertEquals(5, pd.getHeader().size());
      ColumnInfo[] cd = {new ColumnInfo("col1", ColumnDescriptionShort.DataTypes.STRING.toString()),
              new ColumnInfo("col2", ColumnDescriptionShort.DataTypes.STRING.toString()),
              new ColumnInfo("col3", ColumnDescriptionShort.DataTypes.STRING.toString()),
              new ColumnInfo("col4", ColumnDescriptionShort.DataTypes.INT.toString()),
              new ColumnInfo("col5", ColumnDescriptionShort.DataTypes.INT.toString())
      };

      Row row2 = new Row(new Object[]{"row1-col1-Value", "row1-col2-Value", "row1-col3-Value", "10", "11"});
      Row row3 = new Row(new Object[]{"row2-col1-Value", "row2-col2-Value", "row2-col3-Value", "20", "21"});

      Row[] rows = {row2, row3};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    }
  }


  /**
   * additional columns in rows of XML are ignored.
   * number of columns are decided by the first row of the table and here second row contains more columns so those are ignored.
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVMoreColumns() throws Exception {
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

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());


    try(    StringReader sr = new StringReader(str);
            DataParser dp = new DataParser(sr, parseOptions);
    ) {


      PreviewData pd = dp.parsePreview();

      Row row2 = new Row(new Object[]{"row2-col1-Value","row2-col2-Value","row2-col3-Value","20","21"});
      Assert.assertArrayEquals("More number of columns do not give correct result.", row2.getRow(), pd.getPreviewRows().get(1).getRow());
    }
  }

  /**
   * less columns in xml makes them null.
   * number of columns are decided by the first row of the table and here second row does not contain col99 and col100
   * columns so those are set to null.
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVLessColumns() throws Exception {
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

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions);
      ) {
      PreviewData pd = dp.parsePreview();

      Row row2 = new Row(new Object[]{"row2-col1-Value","row2-col2-Value","row2-col3-Value",null,null,"20","21"});
      Assert.assertArrayEquals("Less number of columns do not give correct result.", row2.getRow(), pd.getPreviewRows().get(1).getRow());
    }
  }

  /**
   * illegal xml format gives error. adding illegal tag gives error
   *
   * @throws IOException
   */
  @Test(expected = IllegalArgumentException.class)
  public void testWrongXMLFormat() throws Exception {
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

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());
    try(
      StringReader sr = new StringReader(str);
      DataParser  dp = new DataParser(sr, parseOptions);
      ) {
      PreviewData pd = dp.parsePreview();
    }
  }

  /**
   * One row XML will give embedde column names and 1st row in preview if HEADER.EMBEDDED is selected
   * @throws IOException
   */
  @Test
  public void testParsePreview1RowXML() throws Exception {
    String str = "<table>" +
                      "<row>" +
                      "<col name=\"col1\">row1-col1-Value</col>" +
                      "<col name=\"col2\">11</col>" +
                      "</row>" +
                 "</table>";


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.EMBEDDED.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions);
      ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(1, pd.getPreviewRows().size());
      Assert.assertEquals(2, pd.getHeader().size());
      ColumnInfo[] cd = {new ColumnInfo("col1", ColumnDescriptionShort.DataTypes.STRING.toString()),
        new ColumnInfo("col2", ColumnDescriptionShort.DataTypes.INT.toString())};

      Object cols1[] = new Object[2];
      cols1[0] = "row1-col1-Value";
      cols1[1] = "11";
      Row row1 = new Row(cols1);

      Row[] rows = {row1};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    }
  }

  /**
   * One row XML will give default column names and 1st row in preview if HEADER.PROVIDED_BY_USER is selected
   * @throws IOException
   */
  @Test
  public void testParsePreview1RowXMLHeaderProvided() throws Exception {
    String str = "<table>" +
                    "<row>" +
                    "<col name=\"col1\">row1-col1-Value</col>" +
                    "<col name=\"col2\">11</col>" +
                    "</row>" +
                 "</table>";

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.XML.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.PROVIDED_BY_USER.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions)
      ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(1, pd.getPreviewRows().size());
      Assert.assertEquals(2, pd.getHeader().size());
      ColumnInfo[] cd = {new ColumnInfo("column1", ColumnDescriptionShort.DataTypes.STRING.toString()),
        new ColumnInfo("column2", ColumnDescriptionShort.DataTypes.INT.toString())};

      Object cols1[] = new Object[2];
      cols1[0] = "row1-col1-Value";
      cols1[1] = "11";
      Row row1 = new Row(cols1);

      Row[] rows = {row1};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    }
  }
}
