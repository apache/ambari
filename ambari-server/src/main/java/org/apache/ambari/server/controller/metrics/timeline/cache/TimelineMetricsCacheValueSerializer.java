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
package org.apache.ambari.server.controller.metrics.timeline.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

public class TimelineMetricsCacheValueSerializer implements Serializer<TimelineMetricsCacheValue> {
    private final ClassLoader classLoader;
    public TimelineMetricsCacheValueSerializer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    @Override
    public ByteBuffer serialize(TimelineMetricsCacheValue value) throws SerializerException {
    try {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(value);
        objectOutputStream.close();
        return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
    } catch (Exception e) {
        throw new SerializerException(e);
    }
  }

    @Override
    public TimelineMetricsCacheValue read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
    try {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(binary.array());
      ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
      return (TimelineMetricsCacheValue) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new SerializerException("Error during deserialization", e);
    }
  }

    @Override
    public boolean equals(TimelineMetricsCacheValue value, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
      try {
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(binary.array());
          ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
          TimelineMetricsCacheValue deserializedValue = (TimelineMetricsCacheValue) objectInputStream.readObject();

          // Now compare value and deserializedValue
          if (value == deserializedValue) return true;
          if (deserializedValue == null || (value.getClass() != deserializedValue.getClass())) return false;

          if (!value.getStartTime().equals(deserializedValue.getStartTime())) return false;
          if (!value.getEndTime().equals(deserializedValue.getEndTime())) return false;
          if (!value.getTimelineMetrics().equals(deserializedValue.getTimelineMetrics())) return false;
          return value.getPrecision() == deserializedValue.getPrecision();

      } catch (IOException e) {
          throw new SerializerException("Error during deserialization", e);
      }
  }
}
