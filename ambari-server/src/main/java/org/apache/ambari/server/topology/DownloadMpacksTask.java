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

import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.MpackResourceProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Downloads (registers) missing (declared by the blueprint or the cluster template but not present) management packs
 * during cluster creation.
 */
public class DownloadMpacksTask {
  private static final Logger LOG = LoggerFactory.getLogger(DownloadMpacksTask.class);

  private MpackResourceProvider mpackResourceProvider;
  private AmbariMetaInfo ambariMetaInfo;

  public DownloadMpacksTask(MpackResourceProvider mpackResourceProvider, AmbariMetaInfo ambariMetaInfo) {
    this.mpackResourceProvider = mpackResourceProvider;
    this.ambariMetaInfo = ambariMetaInfo;
  }

  /**
   * Registers/downloads all required management packs
   * @param mpackInstances management pack instances declared in the blueprint and/or the cluster creation template
   */
  public void downloadMissingMpacks(Collection<MpackInstance> mpackInstances) {
    List<MpackInstance> missingMpacks = mpackInstances.stream().
      filter(this::isStackMissing).
      collect(toList());

    for (MpackInstance mpack: missingMpacks) {
      Preconditions.checkArgument(null != mpack.getUrl(), "Cannot register missing mpack with undefined uri: %s",
        mpack.getStackId());
      LOG.info("Registering mpack {}", mpack.getStackId());
      Request mpackRequest = createRequest(mpack.getUrl());
      try {
        RequestStatus.Status status = mpackResourceProvider.createResources(mpackRequest).getStatus();
        if (!RequestStatus.Status.Complete.equals(status)) {
          throw new RuntimeException("An error occured while registering mpack. Request status: " + status);
        }
      }
      catch (Exception ex) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        throw new RuntimeException(
          String.format("Error occured while registering mpack: %s. Caused by %s: %s", mpack.getStackId(),
            rootCause.getClass().getName(), rootCause.getMessage(),
          rootCause));
      }
    }
  }

  /**
   * @param mpackUri the uri of the mpack
   * @return the Request to send to {@link MpackResourceProvider} to to register the mpack
   */
  private Request createRequest(String mpackUri) {
    return PropertyHelper.getCreateRequest(
      ImmutableSet.of(
        ImmutableMap.of(
          MpackResourceProvider.MPACK_URI, mpackUri)),
      ImmutableMap.of()
    );
  }

  /**
   *
   * @param mpackInstance the mpack to check
   * @return {@code true} if the mpack is not registered, {@code false} if already registered.
   */
  boolean isStackMissing(MpackInstance mpackInstance) {
    try {
      ambariMetaInfo.getStack(mpackInstance.getStackId());
      return false;
    }
    catch (StackAccessException ex) {
      LOG.debug("Stack {} is not available.", mpackInstance.getStackId());
      return true;
    }
  }

}
