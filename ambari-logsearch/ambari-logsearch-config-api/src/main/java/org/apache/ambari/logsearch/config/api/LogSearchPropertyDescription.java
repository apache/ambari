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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.config.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for describe static application level properties (e.g.: logsearch.properties/logfeeder.properties)
 * Can be used to generate documentation about the internal configs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface LogSearchPropertyDescription {

  /**
   * Name of the property inside the application level property file.
   */
  String name();

  /**
   * Describe what the property used for.
   */
  String description();

  /**
   * An example value for the property.
   */
  String[] examples();

  /**
   * Default value of the property, emtpy by default.
   */
  String defaultValue() default "";

  /**
   * Name of the property files where the configurations located
   */
  String[] sources();

}
