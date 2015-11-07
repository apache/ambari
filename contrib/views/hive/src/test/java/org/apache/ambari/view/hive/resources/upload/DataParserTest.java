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

package org.apache.ambari.view.hive.resources.upload;

import org.apache.ambari.view.hive.client.ColumnDescription;
import org.apache.ambari.view.hive.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive.client.Row;
import org.apache.ambari.view.hive.resources.uploads.ColumnDescriptionImpl;
import org.apache.ambari.view.hive.resources.uploads.DataParser;
import org.apache.ambari.view.hive.resources.uploads.ParseOptions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

public class DataParserTest {

  @Test
  public void testDataParser() throws IOException {
    String str = "1,a\n" +
            "2,b\n" +
            "3,c\n";
    StringReader sr = new StringReader(str);

    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setOption(ParseOptions.OPTIONS_FILE_TYPE, ParseOptions.FILE_TYPE_CSV);
    parseOptions.setOption(ParseOptions.OPTIONS_HEADER, ParseOptions.HEADER_FIRST_RECORD);

    DataParser dp = new DataParser(sr, parseOptions);
    dp.parsePreview();
    Assert.assertNotNull(dp.getPreviewRows());
    Assert.assertNotNull(dp.getHeader());
    Assert.assertEquals(3, dp.getPreviewRows().size());
    Assert.assertEquals(2, dp.getHeader().size());
    ColumnDescription[] cd = {new ColumnDescriptionImpl("1", ColumnDescriptionShort.DataTypes.INT.toString(), 0),
            new ColumnDescriptionImpl("a", ColumnDescriptionShort.DataTypes.CHAR.toString(), 1)};

    Assert.assertArrayEquals("Header Not Correct.", cd, dp.getHeader().toArray());

    // TODO : include testing of each row element. Below comparison does not work properly.
    // Object[] rows = {new Row(new Object[]{'1','a'}),new Row(new Object[]{'2','b'}),new Row(new Object[]{'3','c'})};
    // Assert.assertArrayEquals("Rows Not Correct.", rows, dp.getPreviewRows().toArray());

    sr.close();
  }
}
