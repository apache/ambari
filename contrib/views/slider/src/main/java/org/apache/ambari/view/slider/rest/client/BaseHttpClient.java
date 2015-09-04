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

package org.apache.ambari.view.slider.rest.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.apache.ambari.view.utils.ambari.URLStreamProviderBasicAuth;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseHttpClient {
	private static final Logger logger = LoggerFactory.getLogger(BaseHttpClient.class);

	private String url;
	private boolean needsAuthentication;
	private String userId;
	private String password;
	protected ViewContext viewContext;
	protected AmbariApi ambariApi;

	public BaseHttpClient(String url, ViewContext viewContext) {
		setUrl(url);
		setNeedsAuthentication(false);
		setViewContext(viewContext);
		if (viewContext != null) {
			ambariApi = new AmbariApi(viewContext);
		}
	}

	public BaseHttpClient(String url, String userId, String password,
			ViewContext viewContext) {
		setUrl(url);
		setNeedsAuthentication(true);
		setUserId(userId);
		setPassword(password);
		setViewContext(viewContext);
		if (viewContext != null) {
			ambariApi = new AmbariApi(viewContext);
		}
	}

	public void setViewContext(ViewContext viewContext) {
		this.viewContext = viewContext;
	}

	public URLStreamProvider getUrlStreamProvider() {
		return viewContext.getURLStreamProvider();
	}

	public URLStreamProviderBasicAuth getUrlStreamProviderBasicAuth() {
		return ambariApi.getUrlStreamProviderBasicAuth();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isNeedsAuthentication() {
		return needsAuthentication;
	}

	public void setNeedsAuthentication(boolean needsAuthentication) {
		this.needsAuthentication = needsAuthentication;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public JsonElement doGetJson(String path) throws HttpException, IOException {
		return doGetJson(getUrl(), path);
	}

	public JsonElement doGetJson(String url, String path) throws HttpException,
			IOException {
		InputStream inputStream = null;
		try {
			Map<String, String> headers = new HashMap<String, String>();
			if (isNeedsAuthentication()) {
				inputStream = getUrlStreamProviderBasicAuth().readFrom(
						url + path, "GET", (String) null, headers);
			} else {
				inputStream = getUrlStreamProviderBasicAuth().readAsCurrent(
						url + path, "GET", (String) null, headers);
			}
		} catch (IOException e) {
			logger.error("Error while reading from url " + url + path, e);
			HttpException httpException = new HttpException(
					e.getLocalizedMessage());
			throw httpException;
		}
		JsonElement jsonElement = new JsonParser().parse(new JsonReader(
				new InputStreamReader(inputStream)));
		return jsonElement;
	}

	public String doGet(String path) throws HttpException, IOException {
		String response = null;
		try {
			InputStream inputStream = null;
			if (isNeedsAuthentication()) {
				inputStream = getUrlStreamProviderBasicAuth().readFrom(
						getUrl() + path, "GET", (String) null,
						new HashMap<String, String>());
			} else {
				inputStream = getUrlStreamProviderBasicAuth().readAsCurrent(
						getUrl() + path, "GET", (String) null,
						new HashMap<String, String>());
			}
			response = IOUtils.toString(inputStream);
		} catch (IOException e) {
			logger.error("Error while reading from url " + getUrl() + path, e);
			HttpException httpException = new HttpException(
					e.getLocalizedMessage());
			throw httpException;
		}
		return response;
	}
}
