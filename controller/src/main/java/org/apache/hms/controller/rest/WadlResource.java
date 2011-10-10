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

package org.apache.hms.controller.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.Marshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jersey.server.wadl.WadlApplicationContext;
import com.sun.jersey.spi.resource.Singleton;
import com.sun.research.ws.wadl.Application;

@Produces({"application/vnd.sun.wadl+xml", "application/xml"})
@Singleton
@Path("wadl")
public class WadlResource {
 
    private static final Log LOG = LogFactory.getLog(WadlResource.class);
 
    private static final String XML_HEADERS = "com.sun.xml.bind.xmlHeaders";
 
    private WadlApplicationContext wadlContext;
 
    private Application application;
 
    private byte[] wadlXmlRepresentation;
 
    public WadlResource(@Context WadlApplicationContext wadlContext) {
        this.wadlContext = wadlContext;
        this.application = wadlContext.getApplication();
    }
 
    @GET
    public synchronized Response getWadl(@Context UriInfo uriInfo) {
        if (wadlXmlRepresentation == null) {
            if (application.getResources().getBase() == null) {
                application.getResources().setBase(uriInfo.getBaseUri().toString());
            }
            try {
                final Marshaller marshaller = wadlContext.getJAXBContext().createMarshaller();
                marshaller.setProperty(XML_HEADERS, "<?xml-stylesheet type='text/xsl' href='/wadl.xsl'?>");
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                marshaller.marshal(application, os);
                wadlXmlRepresentation = os.toByteArray();
                os.close();
            } catch (Exception e) {
                LOG.warn("Could not marshal wadl Application.");
                return javax.ws.rs.core.Response.ok(application).build();
            }
        }
        return Response.ok(new ByteArrayInputStream(wadlXmlRepresentation)).build();
    }
}
