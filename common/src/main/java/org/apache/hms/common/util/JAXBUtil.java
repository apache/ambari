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

package org.apache.hms.common.util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.hms.common.entity.RestSource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class JAXBUtil {

  private static ObjectMapper mapper = new ObjectMapper();
  private static AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
  
  public JAXBUtil() {
    mapper.getDeserializationConfig().setAnnotationIntrospector(introspector);
    mapper.getSerializationConfig().setAnnotationIntrospector(introspector);    
  }
  
  public static byte[] write(RestSource x) throws IOException {
    try {
      return mapper.writeValueAsBytes(x);
    } catch (Throwable e) {
      throw new IOException(e);
    }
  }
  
  public static <T> T read(byte[] buffer, java.lang.Class<T> c) throws IOException {
    return (T) mapper.readValue(buffer, 0, buffer.length, c);
  }

  public static String print(RestSource x) throws IOException {
    try {
      JsonFactory jf = new JsonFactory();
      StringWriter sw = new StringWriter();
      JsonGenerator jp = jf.createJsonGenerator(sw);
      jp.useDefaultPrettyPrinter();
      mapper.writeValue(jp, x);
      jp.close();
      String buffer = sw.toString(); 
      return buffer;
    } catch (Throwable e) {
      throw new IOException(e);
    }    
  }
}
