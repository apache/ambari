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
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class BaseHttpClient {

	private HttpClient httpClient;
	private String url;
	private boolean needsAuthentication;
	private String userId;
	private String password;

	public BaseHttpClient(String url) {
		setUrl(url);
		setNeedsAuthentication(false);
	}

	public BaseHttpClient(String url, String userId, String password) {
		setUrl(url);
		setNeedsAuthentication(true);
		setUserId(userId);
		setPassword(password);
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

	@SuppressWarnings("deprecation")
    public JsonElement doGetJson(String url, String path) throws HttpException,
	    IOException {
		GetMethod get = new GetMethod(url + path);
		if (isNeedsAuthentication()) {
			get.setDoAuthentication(true);
		}
		int executeMethod = getHttpClient().executeMethod(get);
        switch (executeMethod) {
        case HttpStatus.SC_OK:
          JsonElement jsonElement = new JsonParser().parse(new JsonReader(new InputStreamReader(get.getResponseBodyAsStream())));
          return jsonElement;
        case HttpStatus.SC_NOT_FOUND:
          return null;
        default:
          HttpException httpException = new HttpException(get.getResponseBodyAsString());
          httpException.setReason(HttpStatus.getStatusText(executeMethod));
          httpException.setReasonCode(executeMethod);
          throw httpException;
        }
	}

	public String doGet(String path) throws HttpException, IOException {
		GetMethod get = new GetMethod(url + path);
		if (isNeedsAuthentication()) {
			get.setDoAuthentication(true);
		}
		int executeMethod = getHttpClient().executeMethod(get);
		switch (executeMethod) {
		case HttpStatus.SC_OK:
			return get.getResponseBodyAsString();
		default:
			break;
		}
		return null;
	}

	private HttpClient getHttpClient() {
		if (httpClient == null) {
			httpClient = new HttpClient();
		}
		if (isNeedsAuthentication()) {
			httpClient.getState().setCredentials(
			    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
			    new UsernamePasswordCredentials(getUserId(), getPassword()));
			httpClient.getParams().setAuthenticationPreemptive(true);
		}
		return httpClient;
	}
}
