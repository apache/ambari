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
package org.apache.ambari.logsearch.query.converter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class AbstractConverterAware<SOURCE, RESULT> implements Converter<SOURCE, RESULT> {

  @Inject
  @Qualifier("conversionService")
  private ConversionService conversionService;

  public ConversionService getConversionService() {
    return conversionService;
  }

  @PostConstruct
  private void register() {
    if (conversionService instanceof ConverterRegistry) {
      ((ConverterRegistry) conversionService).addConverter(this);
    } else {
      throw new IllegalStateException("Can't register Converter to ConverterRegistry");
    }
  }
}
