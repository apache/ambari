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

package org.apache.ambari.view.hive20.resources.uploads;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.internal.dto.ColumnInfo;
import org.apache.ambari.view.hive20.resources.uploads.parsers.PreviewData;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class UploadServiceTest {

  @Test
  public void generatePreviewWithBOM() throws Exception {
    UploadService uploadService = new UploadService();
    // convert String into InputStream
    String str = "\ufeffCol1\tCol2\nA\tB\n";
    InputStream inputStream = new ByteArrayInputStream(str.getBytes());
    PreviewData previewData = uploadService.generatePreview(true, "CSV", new CSVParams('\t', '\"', '\\'), inputStream);

    Assert.assertEquals("Incorrect number of columns detected.", 2, previewData.getHeader().size() );
    Assert.assertEquals("incorrect col objects.", Arrays.asList(new ColumnInfo("Col1", "CHAR", null, null, null),
        new ColumnInfo("Col2", "CHAR", null, null, null)), previewData.getHeader());
    Assert.assertEquals("incorrect row objects.", Arrays.asList(new Row(new Object[]{"A", "B"})), previewData.getPreviewRows());
  }

  @Test
  public void generatePreviewWithoutBOM() throws Exception {
    UploadService uploadService = new UploadService();
    // convert String into InputStream
    String str = "Col1\tCol2\nA\tB\n";
    InputStream inputStream = new ByteArrayInputStream(str.getBytes());
    PreviewData previewData = uploadService.generatePreview(true, "CSV", new CSVParams('\t', '\"', '\\'), inputStream);

    Assert.assertEquals("Incorrect number of columns detected.", 2, previewData.getHeader().size() );
    Assert.assertEquals("incorrect col objects.", Arrays.asList(new ColumnInfo("Col1", "CHAR", null, null, null),
        new ColumnInfo("Col2", "CHAR", null, null, null)), previewData.getHeader());
    Assert.assertEquals("incorrect row objects.", Arrays.asList(new Row(new Object[]{"A", "B"})), previewData.getPreviewRows());
  }
}
