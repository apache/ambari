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

package org.apache.ambari.view.slider;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.yarn.exceptions.YarnException;

import com.google.gson.JsonObject;
import com.google.inject.ImplementedBy;

@ImplementedBy(SliderAppsViewControllerImpl.class)
public interface SliderAppsViewController {

  public ViewStatus getViewStatus();

  /**
   * Provides information about requested Slider App.
   * 
   * @param applicationId
   * @param properties
   *          Identifies specific properties to show up. Provide
   *          <code>null</code> for default properties.
   * @return
   * @throws YarnException
   * @throws IOException
   */
  public SliderApp getSliderApp(String applicationId, Set<String> properties)
      throws YarnException, IOException;

  /**
   * Provides list of Slider apps with requested properties populated.
   * 
   * @param properties
   *          Identifies specific properties to show up. Provide
   *          <code>null</code> for default properties.
   * @return
   * @throws YarnException
   * @throws IOException
   */
  public List<SliderApp> getSliderApps(Set<String> properties)
      throws YarnException, IOException;

  /**
   * Attempts to delete a Slider app. An unsuccessful attempt will result in
   * exception.
   * 
   * @param applicationId
   * @throws YarnException
   * @throws IOException
   */
  public void deleteSliderApp(String applicationId) throws YarnException,
      IOException;

  public SliderAppType getSliderAppType(String appTypeId, Set<String> properties);

  public List<SliderAppType> getSliderAppTypes(Set<String> properties);

  public String createSliderApp(JsonObject requestJson) throws IOException, YarnException, InterruptedException;
}
