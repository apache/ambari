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

package org.apache.hms.controller.rest.config;

import java.util.List;

import com.sun.jersey.api.wadl.config.WadlGeneratorConfig;
import com.sun.jersey.api.wadl.config.WadlGeneratorDescription;
import com.sun.jersey.server.wadl.WadlGenerator;
import com.sun.jersey.server.wadl.generators.WadlGeneratorApplicationDoc;
import com.sun.jersey.server.wadl.generators.WadlGeneratorGrammarsSupport;
import com.sun.jersey.server.wadl.generators.resourcedoc.WadlGeneratorResourceDocSupport;

/**
 * This subclass of {@link WadlGeneratorConfig} defines/configures 
 * {@link WadlGenerator}s to be used for generating WADL.
 */
public class ExtendedWadlGeneratorConfig extends WadlGeneratorConfig {

  @Override
  public List<WadlGeneratorDescription> configure() {
    return generator( WadlGeneratorApplicationDoc.class )
      .prop( "applicationDocsStream", "application-doc.xml" )
      .generator(WadlGeneratorGrammarsSupport.class)
      .prop("grammarsStream", "application-grammars.xml")
      .generator(WadlGeneratorResourceDocSupport.class)
      .prop("resourceDocStream", "resourcedoc.xml")
      .descriptions();
    }
}
