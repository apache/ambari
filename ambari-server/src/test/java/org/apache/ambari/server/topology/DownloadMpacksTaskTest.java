/*
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

package org.apache.ambari.server.topology;


import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.MpackResourceProvider;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DownloadMpacksTaskTest {

  private static final List<MpackInstance> INSTALLED_MPACKS = ImmutableList.of(
    mpack("HDPCORE", "1.0.0.0"),
    mpack("HDF", "3.1.1.0"));

  private static final List<MpackInstance> MISSING_MPACKS = ImmutableList.of(
    mpack("EDW", "1.0.0"),
    mpack("HDP", "2.6.0"));

  private AmbariMetaInfo metaInfo;
  private MpackResourceProvider resourceProvider;
  private DownloadMpacksTask downloadMpacksTask;
  private Capture<Request> downloadRequests;

  @Before
  public void setup() throws Exception {
    metaInfo = mock(AmbariMetaInfo.class);
    for (MpackInstance mpackInstance: INSTALLED_MPACKS) {
      expect(metaInfo.getStack(mpackInstance.getStackId())).andReturn(null).anyTimes();
    }
    expect(metaInfo.getStack(anyObject(StackId.class))).
      andThrow(new StackAccessException("The specified stack is not found.")).anyTimes();

    resourceProvider = mock(MpackResourceProvider.class);
    downloadRequests = newCapture(CaptureType.ALL);
    expect(resourceProvider.createResources(capture(downloadRequests))).
      andReturn(new RequestStatusImpl(null, null, null)).anyTimes();
    downloadMpacksTask = new DownloadMpacksTask(resourceProvider, metaInfo);

    replay(metaInfo, resourceProvider);
  }

  @Test
  public void testIsStackMissing() {
    INSTALLED_MPACKS.forEach(stackId -> assertFalse(downloadMpacksTask.isStackMissing(stackId)) );
    MISSING_MPACKS.forEach(stackId -> assertTrue(downloadMpacksTask.isStackMissing(stackId)) );
  }

  @Test
  public void testDownloadMissingMpacks() {
    // given
    List<MpackInstance> mpacks = concat(INSTALLED_MPACKS.stream(), MISSING_MPACKS.stream()).collect(toList());

    // when
    downloadMpacksTask.downloadMissingMpacks(mpacks);

    // then
    Set<String> missingMpackUris = MISSING_MPACKS.stream().
      map(MpackInstance::getUrl).
      collect(toSet());

    Set<String> dowloadedMpackUris = downloadRequests.getValues().stream().
      map(this::getUriFromRequest).
      collect(toSet());

    assertEquals(missingMpackUris, dowloadedMpackUris);

  }

  @Test(expected = IllegalArgumentException.class)
  public void testDownloadMissingMpacks_undefinedUri() {
    // given
    MpackInstance brokenMpack = mpack("HDP", "2.6");
    brokenMpack.setUrl(null);

    // when
    downloadMpacksTask.downloadMissingMpacks(ImmutableList.of(brokenMpack));
  }


  private String getUriFromRequest(Request request) {
    return (String)request.getProperties().iterator().next().get(MpackResourceProvider.MPACK_URI);
  }

  private static MpackInstance mpack(String stackName, String stackVersion) {
    return new MpackInstance(stackName, stackName, stackVersion, createUri(stackName, stackVersion), Configuration.createEmpty());
  }

  private static String createUri(String stackName, String stackVersion) {
    return "http://mpacks.org/" + stackName + "." + stackVersion;
  }

}