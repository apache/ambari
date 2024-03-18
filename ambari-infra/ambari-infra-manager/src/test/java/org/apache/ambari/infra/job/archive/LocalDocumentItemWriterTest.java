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

import static org.easymock.EasyMock.cmp;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.LogicalOperator.EQUAL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(EasyMockRunner.class)
public class LocalDocumentItemWriterTest extends EasyMockSupport {

  private static final Document DOCUMENT = new Document(new HashMap<String, Object>() {{ put("id", "1"); }});
  private static final Document DOCUMENT2 = new Document(new HashMap<String, Object>() {{ put("id", "2"); }});
  private static final Document DOCUMENT3 = new Document(new HashMap<String, Object>() {{ put("id", "3"); }});
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private LocalDocumentItemWriter localDocumentItemWriter;
  private File outFile;
  @Mock
  private ItemWriterListener itemWriterListener;

  @Before
  public void setUp() throws Exception {
    outFile = File.createTempFile("LocalDocumentItemWriterTest", "json.tmp");
    localDocumentItemWriter = new LocalDocumentItemWriter(outFile, itemWriterListener);
  }

  @After
  public void tearDown() throws Exception {
    outFile.delete();
    verifyAll();
  }

  @Test
  public void testWrite() throws Exception {
    itemWriterListener.onCompleted(
            cmp(new WriteCompletedEvent(outFile, DOCUMENT, DOCUMENT3), writeCompletedEventEqualityComparator(), EQUAL)); expectLastCall();
    replayAll();

    localDocumentItemWriter.write(DOCUMENT);
    localDocumentItemWriter.write(DOCUMENT2);
    localDocumentItemWriter.write(DOCUMENT3);
    localDocumentItemWriter.close();

    List<Document> documentList = readBack(outFile);
    assertThat(documentList.size(), is(3));
    assertThat(documentList.get(0).getString("id"), is(DOCUMENT.getString("id")));
    assertThat(documentList.get(1).getString("id"), is(DOCUMENT2.getString("id")));
    assertThat(documentList.get(2).getString("id"), is(DOCUMENT3.getString("id")));
  }

  private Comparator<WriteCompletedEvent> writeCompletedEventEqualityComparator() {
    return (o1, o2) -> {
      if (o1.getOutFile().equals(o2.getOutFile()) &&
              o1.getFirstDocument().equals(o2.getFirstDocument()) &&
              o1.getLastDocument().equals(o2.getLastDocument()))
        return 0;
      return 1;
    };
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