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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;

import org.apache.ambari.infra.job.CloseableIterator;
import org.apache.ambari.infra.job.ObjectSource;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;

@RunWith(EasyMockRunner.class)
public class DocumentItemReaderTest extends EasyMockSupport {
  private static final Document DOCUMENT = new Document(new HashMap<String, Object>() {{ put("id", "1"); }});
  private static final Document DOCUMENT_2 = new Document(new HashMap<String, Object>() {{ put("id", "2"); }});
  private static final Document DOCUMENT_3 = new Document(new HashMap<String, Object>() {{ put("id", "3"); }});
  private static final int READ_BLOCK_SIZE = 2;

  private DocumentItemReader documentItemReader;
  @Mock
  private ObjectSource<Document> documentSource;
  @Mock
  private CloseableIterator<Document> documentIterator;
  @Mock
  private CloseableIterator<Document> documentIterator2;

  @Before
  public void setUp() throws Exception {
    documentItemReader = new DocumentItemReader(documentSource, READ_BLOCK_SIZE);
  }

  @After
  public void tearDown() throws Exception {
    verifyAll();
  }

  @Test
  public void testReadWhenCollectionIsEmpty() throws Exception {
    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentIterator.next()).andReturn(null);
    documentIterator.close(); expectLastCall();
    replayAll();

    assertThat(documentItemReader.read(), is(nullValue()));
    assertThat(documentItemReader.isComplete(null), is(true));
    assertThat(documentItemReader.isComplete(null, null), is(true));
  }

  @Test
  public void testReadWhenCollectionContainsLessElementsThanReadBlockSize() throws Exception {
    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentIterator.next()).andReturn(DOCUMENT);
    expect(documentIterator.next()).andReturn(null);
    documentIterator.close(); expectLastCall();
    replayAll();

    assertThat(documentItemReader.read(), is(DOCUMENT));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));
    assertThat(documentItemReader.read(), is(nullValue()));
    assertThat(documentItemReader.isComplete(null), is(true));
    assertThat(documentItemReader.isComplete(null, null), is(true));
  }

  @Test
  public void testReadWhenCollectionContainsExactlySameCountElementsAsReadBlockSize() throws Exception {
    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentSource.open(DOCUMENT_2, 2)).andReturn(documentIterator2);
    expect(documentIterator.next()).andReturn(DOCUMENT);
    expect(documentIterator.next()).andReturn(DOCUMENT_2);
    expect(documentIterator.next()).andReturn(null);
    documentIterator.close(); expectLastCall();

    expect(documentIterator2.next()).andReturn(null);
    documentIterator2.close(); expectLastCall();
    replayAll();

    assertThat(documentItemReader.read(), is(DOCUMENT));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));
    assertThat(documentItemReader.read(), is(DOCUMENT_2));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));
    assertThat(documentItemReader.read(), is(nullValue()));
    assertThat(documentItemReader.isComplete(null), is(true));
    assertThat(documentItemReader.isComplete(null, null), is(true));
  }

  @Test
  public void testReadWhenCollectionContainsMoreElementsThanReadBlockSize() throws Exception {
    Document document3 = new Document(new HashMap<String, Object>() {{ put("id", "2"); }});

    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentSource.open(DOCUMENT_2, 2)).andReturn(documentIterator2);
    expect(documentIterator.next()).andReturn(DOCUMENT);
    expect(documentIterator.next()).andReturn(DOCUMENT_2);
    expect(documentIterator.next()).andReturn(null);
    documentIterator.close(); expectLastCall();
    expect(documentIterator2.next()).andReturn(document3);
    expect(documentIterator2.next()).andReturn(null);
    documentIterator2.close(); expectLastCall();

    replayAll();

    assertThat(documentItemReader.read(), is(DOCUMENT));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));

    assertThat(documentItemReader.read(), is(DOCUMENT_2));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));

    assertThat(documentItemReader.read(), is(document3));
    assertThat(documentItemReader.isComplete(null), is(false));
    assertThat(documentItemReader.isComplete(null, null), is(false));

    assertThat(documentItemReader.read(), is(nullValue()));
    assertThat(documentItemReader.isComplete(null), is(true));
    assertThat(documentItemReader.isComplete(null, null), is(true));
  }

  @Test
  public void testContinueWhenOnlyFirstElementWasRead() throws Exception {
    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentIterator.next()).andReturn(DOCUMENT);
    documentIterator.close(); expectLastCall();
    expect(documentSource.open(null, 2)).andReturn(documentIterator2);
    expect(documentIterator2.next()).andReturn(DOCUMENT);
    documentIterator2.close(); expectLastCall();
    replayAll();

    ExecutionContext executionContext = new ExecutionContext();
    documentItemReader.open(executionContext);
    assertThat(documentItemReader.read(), is(DOCUMENT));
    documentItemReader.update(executionContext);
    assertThat(executionContext.containsKey(DocumentItemReader.POSITION), is(false));
    documentItemReader.close();

    documentItemReader.open(executionContext);
    assertThat(documentItemReader.read(), is(DOCUMENT));
    documentItemReader.close();
  }

  @Test
  public void testContinueWhenMoreThanOneElementWasRead() throws Exception {
    expect(documentSource.open(null, 2)).andReturn(documentIterator);
    expect(documentIterator.next()).andReturn(DOCUMENT);
    expect(documentIterator.next()).andReturn(DOCUMENT_2);
    documentIterator.close(); expectLastCall();
    expect(documentSource.open(DOCUMENT, 2)).andReturn(documentIterator2);
    expect(documentIterator2.next()).andReturn(DOCUMENT_2);
    expect(documentIterator2.next()).andReturn(DOCUMENT_3);
    documentIterator2.close(); expectLastCall();

    replayAll();

    ExecutionContext executionContext = new ExecutionContext();
    documentItemReader.open(executionContext);
    assertThat(documentItemReader.read(), is(DOCUMENT));
    assertThat(documentItemReader.read(), is(DOCUMENT_2));
    documentItemReader.update(executionContext);
    assertThat(executionContext.get(DocumentItemReader.POSITION), is(DOCUMENT));
    documentItemReader.close();

    documentItemReader.open(executionContext);
    assertThat(documentItemReader.read(), is(DOCUMENT_2));
    assertThat(documentItemReader.read(), is(DOCUMENT_3));
    documentItemReader.close();
  }
}