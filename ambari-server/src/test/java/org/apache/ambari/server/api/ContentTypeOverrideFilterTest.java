/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api;

import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Rule;
import org.junit.Test;

import junit.framework.Assert;


public class ContentTypeOverrideFilterTest extends EasyMockSupport {

    private class FilterChainMock implements FilterChain {
        HttpServletResponse response;
        HttpServletRequest request;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            this.request = (HttpServletRequest) request;
            this.response = (HttpServletResponse) response;
        }
    }

    @Rule
    public EasyMockRule mock = new EasyMockRule(this);

    @Mock(type = MockType.NICE)
    private HttpServletRequest request;

    @Mock(type = MockType.NICE)
    private HttpServletResponse response;

    private final ContentTypeOverrideFilter filter = new ContentTypeOverrideFilter();

    @Test
    public void testJSONContentTypeRequest() throws Exception {
        Vector<String> headers = new Vector<>(1);
        headers.add(MediaType.APPLICATION_JSON);

        expect(request.getContentType()).andReturn(MediaType.APPLICATION_JSON).atLeastOnce();
        expect(request.getHeader(HttpHeaders.CONTENT_TYPE)).andReturn(MediaType.APPLICATION_JSON).atLeastOnce();
        expect(request.getHeaders(HttpHeaders.CONTENT_TYPE)).andReturn(headers.elements()).atLeastOnce();
        replayAll();

        FilterChainMock chain = new FilterChainMock();
        filter.doFilter(request, response, chain);

        Assert.assertEquals(MediaType.TEXT_PLAIN, chain.request.getHeader(HttpHeaders.CONTENT_TYPE));
        Assert.assertEquals(MediaType.TEXT_PLAIN, chain.request.getHeaders(HttpHeaders.CONTENT_TYPE).nextElement());

        verifyAll();
    }
}
