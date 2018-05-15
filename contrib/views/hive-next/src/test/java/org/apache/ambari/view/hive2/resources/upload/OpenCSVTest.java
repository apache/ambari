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

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class OpenCSVTest {

  /**
   * no exception in creating csvParser with emtpy stream
   *
   * @throws IOException
   */
  @Test
  public void testEmptyStream() throws Exception {
    String csv = "";

    CSVParser jp = new CSVParser();
    String[] columns = jp.parseLine(csv);
    Assert.assertEquals("Should detect one column.", 1, columns.length);
    Assert.assertEquals("Should detect one column with empty value.", new String[]{""}, columns);
  }

  /**
   * in case of csv an empty line is still considered as row
   *
   * @throws IOException
   */
  @Test
  public void testEmptyRow() throws Exception {
    String csv = "       ";
    CSVParser jp = new CSVParser();

    String[] columns = jp.parseLine(csv);
    Assert.assertEquals("One column not detected.", 1, columns.length);
    Assert.assertArrayEquals("Row should not be empty", new String[]{"       "}, columns);
  }

  @Test
  public void testParse1Row() throws Exception {
    String csv = "value1,c,10,10.1";

    String[] cols = csv.split(",");
    CSVParser jp = new CSVParser();
    String[] columns = jp.parseLine(csv);
    Assert.assertEquals("4 columns not detect", 4, columns.length);
    Assert.assertArrayEquals("Row not equal!", cols, columns);
  }

  @Test
  public void testParseMultipleRow() throws Exception {

    String csv = "value1,c,10,10.1\n" +
      "value2,c2,102,true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,',','"','\\');
    ) {
      String[] row1 = csvReader.readNext();
      String[] row2 = csvReader.readNext();

      Assert.assertArrayEquals("Failed to match 1st row!",new String[]{"value1", "c", "10", "10.1"}, row1);

      Assert.assertArrayEquals("Failed to match 2nd row!",new String[]{"value2", "c2", "102", "true"}, row2);
    }
  }

  @Test
  public void testParseCustomSeparator() throws Exception {

    String csv = "value1#c#10#10.1\n" +
      "value2#c2#102#true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,'#','"','\\');
    ) {
      String[] row1 = csvReader.readNext();
      String[] row2 = csvReader.readNext();

      Assert.assertArrayEquals("Failed to match 1st row!",new String[]{"value1", "c", "10", "10.1"}, row1);

      Assert.assertArrayEquals("Failed to match 2nd row!",new String[]{"value2", "c2", "102", "true"}, row2);
    }
  }


  @Test
  public void testParseCustomSeparatorAndQuote() throws Exception {

    String csv = "\"valu#e1\"#c#10#10.1\n" +
      "value2#c2#102#true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,'#','"','\\');
    ) {
      String[] row1 = csvReader.readNext();
      String[] row2 = csvReader.readNext();

      Assert.assertArrayEquals("Failed to match 1st row!",new String[]{"valu#e1", "c", "10", "10.1"}, row1);

      Assert.assertArrayEquals("Failed to match 2nd row!",new String[]{"value2", "c2", "102", "true"}, row2);
    }
  }

  @Test
  public void testParseCustomSeparatorAndCustomQuote() throws Exception {

    String csv = "\'valu#e1\'#c#10#10.1\n" +
      "value2#c2#102#true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,'#','\'','\\');
    ) {
      String[] row1 = csvReader.readNext();
      String[] row2 = csvReader.readNext();
      String[] row3 = csvReader.readNext();

      Assert.assertArrayEquals("Failed to match 1st row!",new String[]{"valu#e1", "c", "10", "10.1"}, row1);

      Assert.assertArrayEquals("Failed to match 2nd row!",new String[]{"value2", "c2", "102", "true"}, row2);

      Assert.assertArrayEquals("should match Null", null, row3);
    }
  }

  @Test
  public void testWriter() throws Exception {

    String csv = "\'valu#e1\'#c#10#10.1\n" +
      "value2#c2#102#true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,'#','\'','\\');
      StringWriter sw = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(sw);
    ) {
      String[] row1 = csvReader.readNext();
      csvWriter.writeNext(row1);
      String[] row2 = csvReader.readNext();
      csvWriter.writeNext(row2);

      Assert.assertEquals("CSVWriter failed.","\"valu#e1\",\"c\",\"10\",\"10.1\"\n" +
        "\"value2\",\"c2\",\"102\",\"true\"\n", sw.getBuffer().toString());
    }
  }

  @Test
  public void testWriterCustomSeparator() throws Exception {

    String csv = "\'valu#e1\'#c#10#10.1\n" +
      "value2#c2#102#true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,'#','\'','\\');
      StringWriter sw = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(sw,'$');
    ) {
      String[] row1 = csvReader.readNext();
      csvWriter.writeNext(row1);
      String[] row2 = csvReader.readNext();
      csvWriter.writeNext(row2);

      Assert.assertEquals("CSVWriter failed.","\"valu#e1\"$\"c\"$\"10\"$\"10.1\"\n" +
        "\"value2\"$\"c2\"$\"102\"$\"true\"\n", sw.getBuffer().toString());
    }
  }

  @Test
  public void testWriterCustomSeparatorAndEnline() throws Exception {

    String csv = "value1,c,10,10.1\n" +
      "value2,c2,102,true";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,',','\'','\\');
      StringWriter sw = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(sw,'\002',',',"\003");
    ) {
      String[] row1 = csvReader.readNext();
      csvWriter.writeNext(row1,false);
      String[] row2 = csvReader.readNext();
      csvWriter.writeNext(row2,false);

      Assert.assertEquals("CSVWriter failed.","value1\002c\00210\00210.1\003" +
        "value2\002c2\002102\002true\003", sw.getBuffer().toString());
    }
  }

  @Test
  public void testWriterQuote() throws Exception {

    String csv = "val#ue1,c,10,10.1\n" +
      "'val,ue2',c2,102,true\n" +
      "val\002ue3,c\0033,103,false";

    try(
      StringReader sr = new StringReader(csv);
      CSVReader csvReader = new CSVReader(sr,',','\'','\\');
      StringWriter sw = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(sw,'\002','\'',"\003");
    ) {
      String[] row1 = csvReader.readNext();
      csvWriter.writeNext(row1,false);
      String[] row2 = csvReader.readNext();
      csvWriter.writeNext(row2,false);
      String[] row3 = csvReader.readNext();
      csvWriter.writeNext(row3,false);

      Assert.assertEquals("CSVWriter failed.","val#ue1\u0002c\u000210\u000210.1\u0003" +
        "val,ue2\u0002c2\u0002102\u0002true\u0003" +
        "'val\u0002ue3'\u0002c\u00033\u0002103\u0002false\u0003", sw.getBuffer().toString());
    }
  }
}
