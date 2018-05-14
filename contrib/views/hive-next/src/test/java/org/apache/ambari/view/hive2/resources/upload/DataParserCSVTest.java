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

package org.apache.ambari.view.hive2.resources.upload;

import org.apache.ambari.view.hive2.client.ColumnDescription;
import org.apache.ambari.view.hive2.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive2.client.Row;
import org.apache.ambari.view.hive2.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive2.resources.uploads.parsers.DataParser;
import org.apache.ambari.view.hive2.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive2.resources.uploads.parsers.PreviewData;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class DataParserCSVTest {
  @Test
  public void testParsePreviewCSV() throws Exception {
    String str = "1,a\n" +
            "2,b\n" +
            "3,c\n";


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());


    try (
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions);
    ){

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getPreviewRows());
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(2, pd.getPreviewRows().size()); // now it will not return the first row which is header
      Assert.assertEquals(2, pd.getHeader().size());
      ColumnDescription[] cd = {new ColumnDescriptionImpl("1", ColumnDescriptionShort.DataTypes.INT.toString(), 0),
              new ColumnDescriptionImpl("a", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1)};

      Object cols2[] = new Object[2];
      cols2[0] = "2";
      cols2[1] = "b";
      Row row2 = new Row(cols2);

      Object cols3[] = new Object[2];
      cols3[0] = "3";
      cols3[1] = "c";
      Row row3 = new Row(cols3);

      Row[] rows = { row2, row3};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    }
  }

  /**
   * even if in one of the preview rows, datatype is not correct, then it should be assigned that datatype.
   * but if first row is header then first row should not be acconted for detecting datatype
   * @throws IOException
   */
  @Test
  public void testParsePreviewDataTypeDetectionCSV() throws Exception {
    String str = "1,a,10,k\n" +
      "2,b,6,8\n" +
      "2.2,b,7,9\n" +
      "2,b,abc,1\n" +
      "2,b,9,3\n" +
      "2,b,8,5\n" +
      "2,b,7,3\n" +
      "2,b,6,3\n" +
      "3,c,c,3\n";


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    try(StringReader sr = new StringReader(str);
      DataParser dp= new DataParser(sr, parseOptions)) {

      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(4, pd.getHeader().size());
      ColumnDescription[] cd = {
        // as row 3 contains 2.2
        new ColumnDescriptionImpl("1", ColumnDescriptionShort.DataTypes.DOUBLE.toString(), 0),
        // as all are chars
        new ColumnDescriptionImpl("a", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1),
        // as row 4 contains abc
        new ColumnDescriptionImpl("10", ColumnDescriptionShort.DataTypes.STRING.toString(), 2),
        // although row 1 contains k but it is in header and not counted in detecting datatype
        new ColumnDescriptionImpl("k", ColumnDescriptionShort.DataTypes.INT.toString(), 3)};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
    }
  }

  /**
   * even if in one of the preview rows, datatype is not correct, then it should be assigned that datatype.
   * but if first row is header then first row should not be acconted for detecting datatype
   * @throws IOException
   */
  @Test
  public void testParsePreviewDataTypeDetection2CSV() throws Exception {
    String str = "1,a,10,k\n" +
      "2,b,6,p\n" +
      "2.2,b,7,9\n" +
      "2,b,2.2,1\n" +
      "2,b,9,3\n" +
      "2,b,8,5\n" +
      "2,b,7,3\n" +
      "2,b,6,3\n" +
      "3,c,c,3\n";


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());


    try(StringReader sr = new StringReader(str);
        DataParser dp = new DataParser(sr, parseOptions)) {


      PreviewData pd = dp.parsePreview();
      Assert.assertNotNull(pd.getHeader());
      Assert.assertEquals(4, pd.getHeader().size());
      ColumnDescription[] cd = {
        // as row 3 contains 2.2
        new ColumnDescriptionImpl("1", ColumnDescriptionShort.DataTypes.DOUBLE.toString(), 0),
        // as all are chars
        new ColumnDescriptionImpl("a", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1),
        // some are int, char and some double .. nothing other than 'string' satisfies all the rows
        new ColumnDescriptionImpl("10", ColumnDescriptionShort.DataTypes.STRING.toString(), 2),
        // although row 1 contains k but it is in header and not counted in detecting datatype
        // but row 2 also has a char p which will be acconted for datatype detection
        new ColumnDescriptionImpl("k", ColumnDescriptionShort.DataTypes.CHAR.toString(), 3)};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
    }
  }

  /**
   * One row csv will give default column names and 1st row in preview if HEADER.PROVIDED_BY_USER is selected
   * @throws IOException
   */
  @Test
  public void testParsePreview1RowCSV() throws Exception {
    String str = "1,a\n" ;

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
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
      ColumnDescription[] cd = {new ColumnDescriptionImpl("column1", ColumnDescriptionShort.DataTypes.INT.toString(), 0),
        new ColumnDescriptionImpl("column2", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1)};

      Object cols1[] = new Object[2];
      cols1[0] = "1";
      cols1[1] = "a";
      Row row1 = new Row(cols1);

      Row[] rows = {row1};

      Assert.assertArrayEquals("Header Not Correct.", cd, pd.getHeader().toArray());
      Assert.assertArrayEquals("Rows Not Correct.", rows, pd.getPreviewRows().toArray());
    }
  }

  /**
   * One row csv will throw exception in preview if HEADER.FIRST_RECORD is selected.
   * @throws IOException
   */
  @Test(expected = java.util.NoSuchElementException.class)
  public void testParsePreview1RowCSVFirstRowHeader() throws Exception {
    String str = "col1,col2\n" ;


    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());


    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions)
    ) {


      PreviewData pd = dp.parsePreview();
    }
  }

  /**
   * more number of columns in a row => igore the extra columns. Number of columns is decided by the first row.
   * If other row contains more columns then those columns will be ignored
   * Here first row has 2 columns and second row has 3 columns so the value 'x' is ignored
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVMoreColumns() throws Exception {
    String str = "1,a\n" +
            "2,b,x\n" +  // contains 3 cols, more number of columns
            "3,c\n";

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());


    try(
        StringReader sr = new StringReader(str);
        DataParser dp = new DataParser(sr, parseOptions)
    ) {

      PreviewData pd = dp.parsePreview();
      Row row = new Row(new Object[]{"2","b"});

      Assert.assertArrayEquals("Additional columns not properly handled.", row.getRow(),pd.getPreviewRows().get(0).getRow());
    }
  }

  /**
   * less number of columns => treat missing values as null. Number of columns is decided by the first row of the table
   * if other rows has less number of columns then it treats other columns as null
   * @throws IOException
   */
  @Test
  public void testParsePreviewCSVLessColumns() throws Exception {
    String str = "1,a\n" +
            "2\n" +  // contains 1 col, less number of columns
            "3,c\n";

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp =  new DataParser(sr, parseOptions)
      ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Missing value not detected as null.",pd.getPreviewRows().get(1).getRow()[1],null);
    }
  }

  /**
   * empty values are treated as empty string
   * @throws IOException
   */
  @Test
  public void testEmptyColumn() throws Exception {
    String str = "1,a,x\n" +
            "2,,y\n" +  // contains 1 col, less number of columns
            "3,c,z\n";
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions)
    ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(0).getRow()[1],"");
    }
  }

  /**
   * empty values are treated as empty string
   * @throws IOException
   */
  @Test
  public void testLastEmptyColumn() throws Exception {
    String str = "1,a,x\n" +
            "2,,\n" +  // contains 1 col, less number of columns
            "3,c,z\n";

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.InputFileType.CSV.toString());
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER.FIRST_RECORD.toString());

    try(
      StringReader sr = new StringReader(str);
      DataParser dp = new DataParser(sr, parseOptions)
    ) {

      PreviewData pd = dp.parsePreview();
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(0).getRow()[1],"");
      Assert.assertEquals("Empty column not detected properly.",pd.getPreviewRows().get(0).getRow()[2],"");
    }
  }
}
