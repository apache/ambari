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
package org.apache.ambari.infra.json;

import static org.apache.commons.lang.StringUtils.isBlank;

import javax.inject.Named;

import org.apache.hadoop.fs.permission.FsPermission;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.util.StdConverter;

@Named
@ConfigurationPropertiesBinding
public class StringToFsPermissionConverter extends StdConverter<String, FsPermission> implements Converter<String, FsPermission> {
  @Override
  public FsPermission convert(@NonNull String value) {
    return toFsPermission(value);
  }

  public static FsPermission toFsPermission(String value) {
    return isBlank(value) ? null : new FsPermission(value);
  }
}
