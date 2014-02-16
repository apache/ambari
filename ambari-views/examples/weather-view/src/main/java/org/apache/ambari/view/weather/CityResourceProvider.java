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

package org.apache.ambari.view.weather;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.ambari.view.NoSuchResourceException;
import org.apache.ambari.view.ReadRequest;
import org.apache.ambari.view.ResourceAlreadyExistsException;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.UnsupportedPropertyException;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for city resource of the weather view.
 */
public class CityResourceProvider implements ResourceProvider<CityResource> {

  /**
   * The logger.
   */
  protected final static Logger LOG =
      LoggerFactory.getLogger(CityResourceProvider.class);

  /**
   * The view context.
   */
  @Inject
  ViewContext viewContext;


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public CityResource getResource(String resourceId, Set<String> propertyIds) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException {

    Map<String, String> properties = viewContext.getProperties();

    String units = properties.get("units");

    try {
      return getResource(resourceId, units, propertyIds);
    } catch (IOException e) {
      throw new SystemException("Can't get city resource " + resourceId + ".", e);
    }
  }

  @Override
  public Set<CityResource> getResources(ReadRequest request) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException {

    Set<CityResource> resources = new HashSet<CityResource>();

    Map<String, String> properties = viewContext.getProperties();

    String   units   = properties.get("units");
    String   cityStr = properties.get("cities");
    String[] cities  = cityStr.split(";");

    for (String city : cities) {
      try {
        resources.add(getResource(city, units, request.getPropertyIds()));
      } catch (IOException e) {
        throw new SystemException("Can't get city resource " + city + ".", e);
      }
    }
    return resources;
  }

  @Override
  public void createResource(String resourceId, Map<String, Object> stringObjectMap) throws
      SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Creating city resources is not currently supported");
  }

  @Override
  public boolean updateResource(String resourceId, Map<String, Object> stringObjectMap) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Updating city resources is not currently supported");
  }

  @Override
  public boolean deleteResource(String resourceId) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Deleting city resources is not currently supported");
  }


  // ----- helper methods ----------------------------------------------------

  // Get a city resource from the given id.
  private CityResource getResource(String resourceId, String units, Set<String> propertyIds) throws IOException {
    CityResource resource = new CityResource();

    resource.setId(resourceId);
    resource.setUnits(units);
    if (isWeatherRequested(propertyIds)) {
      resource.setWeather(getWeatherProperty(resourceId, units));
    }
    return resource;
  }

  // Determine whether the weather property has been requested.
  private boolean isWeatherRequested(Set<String> propertyIds) {

    for (String propertyId : propertyIds) {
      if (propertyId.startsWith("weather")) {
        return true;
      }
    }
    return false;
  }

  // Populate the weather property.
  private Map<String, Object> getWeatherProperty(String city, String units) throws IOException {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("http");
    uriBuilder.setHost("api.openweathermap.org");
    uriBuilder.setPath("/data/2.5/weather");
    uriBuilder.setParameter("q", city);
    uriBuilder.setParameter("units", units);

    String url = uriBuilder.toString();

    InputStream in = readFrom(url);
    try {
      Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
      Map<String, Object> results =  new Gson().fromJson(IOUtils.toString(in, "UTF-8"), mapType);

      ArrayList list = (ArrayList) results.get("weather");
      if (list != null) {
        Map weather = (Map) list.get(0);
        results.put("weather", weather);
        results.put("icon_src", "http://openweathermap.org/img/w/" + weather.get("icon"));
      }
      return results;
    } finally {
      in.close();
    }
  }

  // Get an input stream from the given URL spec.
  private static InputStream readFrom(String spec) throws IOException {
    URLConnection connection = new URL(spec).openConnection();

    connection.setConnectTimeout(5000);
    connection.setDoOutput(true);
    return connection.getInputStream();
  }
}
