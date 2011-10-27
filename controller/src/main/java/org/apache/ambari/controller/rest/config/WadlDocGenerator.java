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

package org.apache.ambari.controller.rest.config;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.client.ClusterStack;
import org.apache.ambari.controller.rest.resources.StackResource;
import org.apache.ambari.controller.rest.resources.StacksResource;
import org.apache.ambari.controller.rest.resources.ClusterResource;
import org.apache.ambari.controller.rest.resources.ClustersResource;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResource;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.server.wadl.WadlGenerator;
import com.sun.jersey.server.wadl.generators.resourcedoc.ResourceDocAccessor;
import com.sun.jersey.server.wadl.generators.resourcedoc.model.MethodDocType;
import com.sun.jersey.server.wadl.generators.resourcedoc.model.ResourceDocType;
import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Method;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.RepresentationType;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * This {@link WadlDocGenerator} shows how the custom information added by the
 * {@link ExampleDocProcessor} to the resourcedoc.xml can be processed by this
 * {@link WadlDocGenerator} and is used to extend the generated application.wadl<br>
 * 
 * @version $Id$
 */
public class WadlDocGenerator implements WadlGenerator {

    private static final Logger LOG = Logger.getLogger( WadlDocGenerator.class.getName() );

    private WadlGenerator _delegate;
    private File _resourceDocFile;
    private ResourceDocAccessor _resourceDoc;

    /* (non-Javadoc)
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#setWadlGeneratorDelegate(com.sun.jersey.server.impl.wadl.WadlGenerator)
     */
    @Override
    public void setWadlGeneratorDelegate( WadlGenerator delegate ) {
        _delegate = delegate;
    }

    /* (non-Javadoc)
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#getRequiredJaxbContextPath()
     */
    public String getRequiredJaxbContextPath() {
        return _delegate.getRequiredJaxbContextPath();
    }

    public void setResourceDocFile( File resourceDocFile ) {
        _resourceDocFile = resourceDocFile;
    }

    public void init() throws Exception {
        _delegate.init();
        final ResourceDocType resourceDoc = loadFile( _resourceDocFile, ResourceDocType.class, ResourceDocType.class,
        		Examples.class);
        _resourceDoc = new ResourceDocAccessor( resourceDoc );
    }

    private <T> T loadFile( File fileToLoad, Class<T> targetClass, Class<?> ... classesToBeBound ) {
        if ( fileToLoad == null ) {
            throw new IllegalArgumentException( "The resource-doc file to load is not set!" );
        }
        try {
            final JAXBContext c = JAXBContext.newInstance( classesToBeBound );
            final Unmarshaller m = c.createUnmarshaller();
            return targetClass.cast( m.unmarshal( fileToLoad ) );
        } catch( Exception e ) {
            LOG.log( Level.SEVERE, "Could not unmarshal file " + fileToLoad, e );
            throw new RuntimeException( "Could not unmarshal file " + fileToLoad, e );
        }
    }

    /**
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createApplication()
     */
    public Application createApplication() {
        return _delegate.createApplication();
    }

    /**
     * @param resource
     * @param resourceMethod
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createMethod(com.sun.jersey.api.model.AbstractResource, com.sun.jersey.api.model.AbstractResourceMethod)
     */
    public Method createMethod( AbstractResource r,
            AbstractResourceMethod m ) {
        final Method result = _delegate.createMethod( r, m );
        final MethodDocType methodDoc = _resourceDoc.getMethodDoc( r.getResourceClass(), m.getMethod() );
        return result;
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createRequest(com.sun.jersey.api.model.AbstractResource, com.sun.jersey.api.model.AbstractResourceMethod)
     */
    public Request createRequest( AbstractResource arg0,
            AbstractResourceMethod arg1 ) {
        return _delegate.createRequest( arg0, arg1 );
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createParam(com.sun.jersey.api.model.AbstractResource, com.sun.jersey.api.model.AbstractMethod, com.sun.jersey.api.model.Parameter)
     */
    public Param createParam( AbstractResource arg0,
            AbstractMethod arg1, Parameter arg2 ) {
        return _delegate.createParam( arg0, arg1, arg2 );
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createRequestRepresentation(com.sun.jersey.api.model.AbstractResource, com.sun.jersey.api.model.AbstractResourceMethod, javax.ws.rs.core.MediaType)
     */
    public RepresentationType createRequestRepresentation(
            AbstractResource arg0, AbstractResourceMethod arg1, MediaType arg2 ) {
        return _delegate.createRequestRepresentation( arg0, arg1, arg2 );
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createResource(com.sun.jersey.api.model.AbstractResource, java.lang.String)
     */
    public Resource createResource( AbstractResource arg0, String arg1 ) {
        return _delegate.createResource( arg0, arg1 );
    }

    /**
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createResources()
     */
    public Resources createResources() {
        return _delegate.createResources();
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     * @see com.sun.jersey.server.impl.wadl.WadlGenerator#createResponse(com.sun.jersey.api.model.AbstractResource, com.sun.jersey.api.model.AbstractResourceMethod)
     */
    public Response createResponse( AbstractResource arg0,
            AbstractResourceMethod arg1 ) {
        return _delegate.createResponse( arg0, arg1 );
    }

}