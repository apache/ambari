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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.view.NoSuchResourceException;
import org.apache.ambari.view.ReadRequest;
import org.apache.ambari.view.ResourceAlreadyExistsException;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.UnsupportedPropertyException;
import org.apache.hadoop.yarn.exceptions.YarnException;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SliderAppsResourceProvider implements ResourceProvider<SliderApp> {

	private static final Logger logger = LoggerFactory
	    .getLogger(SliderAppsResourceProvider.class);
	@Inject
	private SliderAppsViewController sliderController;

	@Override
	public void createResource(String resourceId, Map<String, Object> properties)
	    throws SystemException, ResourceAlreadyExistsException,
	    NoSuchResourceException, UnsupportedPropertyException {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean deleteResource(String resourceId) throws SystemException,
	    NoSuchResourceException, UnsupportedPropertyException {
		try {
			sliderController.deleteSliderApp(resourceId);
			return true;
		} catch (YarnException e) {
			logger.warn("Unable to delete Slider app with id " + resourceId, e);
			throw new SystemException(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("Unable to delete Slider app with id " + resourceId, e);
			throw new SystemException(e.getMessage(), e);
		} catch (InterruptedException e) {
		    logger.warn("Unable to delete Slider app with id " + resourceId, e);
            throw new SystemException(e.getMessage(), e);
        }
	}

	@Override
	public SliderApp getResource(String resourceId, Set<String> properties)
	    throws SystemException, NoSuchResourceException,
	    UnsupportedPropertyException {
		try {
			SliderApp sliderApp = sliderController.getSliderApp(resourceId,
			    properties);
			if (sliderApp == null)
				throw new NoSuchResourceException(resourceId);
			return sliderApp;
		} catch (YarnException e) {
			logger.warn("Unable to determine Slider app with id " + resourceId, e);
			throw new SystemException(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("Unable to determine Slider app with id " + resourceId, e);
			throw new SystemException(e.getMessage(), e);
		} catch (InterruptedException e) {
		    logger.warn("Unable to determine Slider app with id " + resourceId, e);
            throw new SystemException(e.getMessage(), e);
        }
	}

	@Override
	public Set<SliderApp> getResources(ReadRequest request)
	    throws SystemException, NoSuchResourceException,
	    UnsupportedPropertyException {
		Set<SliderApp> appSet = new HashSet<SliderApp>();
		try {
			List<SliderApp> sliderApps = sliderController.getSliderApps(request
			    .getPropertyIds());
			for (SliderApp app : sliderApps)
				appSet.add(app);
		} catch (YarnException e) {
			logger.warn("Unable to determine Slider apps", e);
			throw new SystemException(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("Unable to determine Slider apps", e);
			throw new SystemException(e.getMessage(), e);
		} catch (InterruptedException e) {
		    logger.warn("Unable to determine Slider apps", e);
            throw new SystemException(e.getMessage(), e);
        }
		return appSet;
	}

	@Override
	public boolean updateResource(String resourceId,
	    Map<String, Object> properties) throws SystemException,
	    NoSuchResourceException, UnsupportedPropertyException {
		// TODO Auto-generated method stub
		return false;
	}

}
