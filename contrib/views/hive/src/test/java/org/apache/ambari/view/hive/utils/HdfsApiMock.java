/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.utils;

import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.easymock.EasyMock.*;

/**
* Created by roma on 2/6/15.
*/
public class HdfsApiMock {
  private ByteArrayOutputStream fsQueryOutputStream;
  private ByteArrayOutputStream fsLogsOutputStream;
  private HdfsApi hdfsApi;

  public HdfsApiMock(String inputFileContent) throws IOException, InterruptedException, HdfsApiException {
    setupHdfsApi(inputFileContent);
  }

  protected void setupHdfsApi(String inputFileContent) throws IOException, InterruptedException, HdfsApiException {
    hdfsApi = createNiceMock(HdfsApi.class);

    hdfsApi.copy(anyString(), anyString());

    fsQueryOutputStream = setupQueryOutputStream(hdfsApi);
    fsLogsOutputStream = setupLogsOutputStream(hdfsApi);
    setupQueryInputStream(hdfsApi, inputFileContent);

    expect(hdfsApi.mkdir(anyString())).andReturn(true).anyTimes();
  }

  protected SeekableByteArrayInputStream setupQueryInputStream(HdfsApi hdfsApi, String query) throws IOException, InterruptedException {
    SeekableByteArrayInputStream inputStreamHQL = new SeekableByteArrayInputStream(query.getBytes());
    expect(hdfsApi.open(endsWith(".hql"))).andReturn(new FSDataInputStream(inputStreamHQL)).anyTimes();
    return inputStreamHQL;
  }

  protected ByteArrayOutputStream setupQueryOutputStream(HdfsApi hdfsApi) throws IOException, InterruptedException {
    ByteArrayOutputStream queryOutputStream = new ByteArrayOutputStream();
    FSDataOutputStream fsQueryOutputStream = new FSDataOutputStream(queryOutputStream);
    expect(hdfsApi.create(endsWith(".hql"), anyBoolean())).andReturn(fsQueryOutputStream);
    return queryOutputStream;
  }

  protected ByteArrayOutputStream setupLogsOutputStream(HdfsApi hdfsApi) throws IOException, InterruptedException {
    ByteArrayOutputStream logsOutputStream = new ByteArrayOutputStream();
    FSDataOutputStream fsLogsOutputStream = new FSDataOutputStream(logsOutputStream);
    expect(hdfsApi.create(endsWith("logs"), anyBoolean())).andReturn(fsLogsOutputStream).anyTimes();
    return logsOutputStream;
  }

  public HdfsApi getHdfsApi() {
    return hdfsApi;
  }

  public ByteArrayOutputStream getQueryOutputStream() {
    return fsQueryOutputStream;
  }

  public ByteArrayOutputStream getLogsOutputStream() {
    return fsLogsOutputStream;
  }
}
