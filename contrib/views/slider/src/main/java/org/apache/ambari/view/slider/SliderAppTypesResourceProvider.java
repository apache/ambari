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

public class SliderAppTypesResourceProvider implements
    ResourceProvider<SliderAppType> {

	private static final Logger logger = LoggerFactory
	    .getLogger(SliderAppTypesResourceProvider.class);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SliderAppType getResource(String appTypeId, Set<String> properties)
	    throws SystemException, NoSuchResourceException,
	    UnsupportedPropertyException {
			SliderAppType sliderAppType = sliderController.getSliderAppType(appTypeId,
			    properties);
			if (sliderAppType == null)
				throw new NoSuchResourceException(appTypeId);
			return sliderAppType;
	}

	@Override
	public Set<SliderAppType> getResources(ReadRequest request)
	    throws SystemException, NoSuchResourceException,
	    UnsupportedPropertyException {
		Set<SliderAppType> appSet = new HashSet<SliderAppType>();
			List<SliderAppType> sliderAppTypes = sliderController.getSliderAppTypes(request
			    .getPropertyIds());
            if (sliderAppTypes != null) {
              for (SliderAppType app : sliderAppTypes)
                appSet.add(app);
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
