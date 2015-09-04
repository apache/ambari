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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SliderAppsConfiguration {

	public static final SliderAppsConfiguration INSTANCE = new SliderAppsConfiguration();
	private static final Logger logger = LoggerFactory
	    .getLogger(SliderAppsConfiguration.class);
	private static final String SLIDER_APPS_PROPERTIES_FILE = "/slider.properties";
	private PropertiesConfiguration propertiesConfig = null;

	private PropertiesConfiguration getConfiguration()
	    throws ConfigurationException {
		if (propertiesConfig == null) {
			propertiesConfig = new PropertiesConfiguration();
			propertiesConfig.load(getClass().getResourceAsStream(
			    SLIDER_APPS_PROPERTIES_FILE));
		}
		return propertiesConfig;
	}

	public String getVersion() {
		try {
			return getConfiguration().getString("slider.view.version");
		} catch (ConfigurationException e) {
			logger.warn("Unable to get version configuration", e);
		}
		return null;
	}
}
