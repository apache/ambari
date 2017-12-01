/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.infra.job.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class LocalDocumentItemWriterTest extends EasyMockSupport {

  private static final Document DOCUMENT = new Document(new HashMap<String, String>() {{ put("id", "1"); }});
  private static final Document DOCUMENT2 = new Document(new HashMap<String, String>() {{ put("id", "2"); }});
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private LocalDocumentItemWriter localDocumentItemWriter;
  private File outFile;
  @Mock
  private FileAction fileAction;

  @Before
  public void setUp() throws Exception {
    outFile = File.createTempFile("LocalDocumentItemWriterTest", "json.tmp");
    localDocumentItemWriter = new LocalDocumentItemWriter(outFile, fileAction);
  }

  @After
  public void tearDown() throws Exception {
    outFile.delete();
    verifyAll();
  }

  @Test
  public void testWrite() throws Exception {
    expect(fileAction.perform(outFile)).andReturn(outFile);
    replayAll();

    localDocumentItemWriter.write(DOCUMENT);
    localDocumentItemWriter.write(DOCUMENT2);
    localDocumentItemWriter.close();

    List<Document> documentList = readBack(outFile);
    assertThat(documentList.size(), is(2));
    assertThat(documentList.get(0).get("id"), is(DOCUMENT.get("id")));
    assertThat(documentList.get(1).get("id"), is(DOCUMENT2.get("id")));
  }

  private List<Document> readBack(File file) throws IOException {
    List<Document> documentList = new ArrayList<>();
    for (String line : FileUtils.readLines(file)) {
      documentList.add(OBJECT_MAPPER.readValue(line, Document.class));
    }
    return documentList;
  }

  @Test
  public void testRevert() throws Exception {
    replayAll();

    localDocumentItemWriter.write(DOCUMENT);
    localDocumentItemWriter.revert();

    assertThat(outFile.exists(), is(false));
  }
}