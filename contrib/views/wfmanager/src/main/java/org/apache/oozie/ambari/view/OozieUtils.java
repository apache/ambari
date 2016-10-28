/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OozieUtils {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(OozieUtils.class);
	private Utils utils = new Utils();

	public String generateConfigXml(Map<String, String> map) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			Element configElement = doc.createElement("configuration");
			doc.appendChild(configElement);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				Element propElement = doc.createElement("property");
				configElement.appendChild(propElement);
				Element nameElem = doc.createElement("name");
				nameElem.setTextContent(entry.getKey());
				Element valueElem = doc.createElement("value");
				valueElem.setTextContent(entry.getValue());
				propElement.appendChild(nameElem);
				propElement.appendChild(valueElem);
			}
			return utils.generateXml(doc);
		} catch (ParserConfigurationException e) {
			LOGGER.error("error in generating config xml", e);
			throw new RuntimeException(e);
		}
	}
	public String getJobPathPropertyKey(JobType jobType) {
		switch (jobType) {
		case WORKFLOW:
			return "oozie.wf.application.path";
		case COORDINATOR:
			return "oozie.coord.application.path";
		case BUNDLE:
			return "oozie.bundle.application.path";
		}
		throw new RuntimeException("Unknown Job Type");
	}
}
