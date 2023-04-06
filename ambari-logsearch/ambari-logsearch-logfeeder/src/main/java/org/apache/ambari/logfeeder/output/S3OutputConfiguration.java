/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output;

import org.apache.ambari.logfeeder.plugin.common.ConfigItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all configuration relevant for S3 upload.
 */
public class S3OutputConfiguration {

  public static final String SPOOL_DIR_KEY = "spool_dir";
  public static final String ROLLOVER_SIZE_THRESHOLD_BYTES_KEY = "rollover_size_threshold_bytes";
  public static final Long DEFAULT_ROLLOVER_SIZE_THRESHOLD_BYTES = 10 * 1024 * 1024L;
  public static final String ROLLOVER_TIME_THRESHOLD_SECS_KEY = "rollover_time_threshold_secs";
  public static final Long DEFAULT_ROLLOVER_TIME_THRESHOLD_SECS = 3600L;
  public static final String S3_BUCKET_NAME_KEY = "s3_bucket";
  public static final String S3_LOG_DIR_KEY = "s3_log_dir";
  public static final String S3_ACCESS_KEY = "s3_access_key";
  public static final String S3_SECRET_KEY = "s3_secret_key";
  public static final String COMPRESSION_ALGO_KEY = "compression_algo";
  public static final String ADDITIONAL_FIELDS_KEY = "add_fields";
  public static final String CLUSTER_KEY = "cluster";

  private Map<String, Object> configs;

  S3OutputConfiguration(Map<String, Object> configs) {
    this.configs = configs;
  }

  public String getS3BucketName() {
    return (String) configs.get(S3_BUCKET_NAME_KEY);
  }

  public String getS3Path() {
    return (String) configs.get(S3_LOG_DIR_KEY);
  }

  public String getS3AccessKey() {
    return (String) configs.get(S3_ACCESS_KEY);
  }

  public String getS3SecretKey() {
    return (String) configs.get(S3_SECRET_KEY);
  }

  public String getCompressionAlgo() {
    return (String) configs.get(COMPRESSION_ALGO_KEY);
  }

  public Long getRolloverSizeThresholdBytes() {
    return (Long) configs.get(ROLLOVER_SIZE_THRESHOLD_BYTES_KEY);
  }

  public Long getRolloverTimeThresholdSecs() {
    return (Long) configs.get(ROLLOVER_TIME_THRESHOLD_SECS_KEY);
  }

  @SuppressWarnings("unchecked")
  public String getCluster() {
    return ((Map<String, String>) configs.get(ADDITIONAL_FIELDS_KEY)).get(CLUSTER_KEY);
  }

  public static S3OutputConfiguration fromConfigBlock(ConfigItem configItem) {
    Map<String, Object> configs = new HashMap<>();
    String[] stringValuedKeysToCopy = new String[] {
        SPOOL_DIR_KEY, S3_BUCKET_NAME_KEY, S3_LOG_DIR_KEY,
        S3_ACCESS_KEY, S3_SECRET_KEY, COMPRESSION_ALGO_KEY
    };

    for (String key : stringValuedKeysToCopy) {
      String value = configItem.getStringValue(key);
      if (value != null) {
        configs.put(key, value);
      }
    }

    String[] longValuedKeysToCopy = new String[] {
        ROLLOVER_SIZE_THRESHOLD_BYTES_KEY, ROLLOVER_TIME_THRESHOLD_SECS_KEY
    };

    Long[] defaultValuesForLongValuedKeys = new Long[] {
        DEFAULT_ROLLOVER_SIZE_THRESHOLD_BYTES, DEFAULT_ROLLOVER_TIME_THRESHOLD_SECS
    };

    for (int i = 0; i < longValuedKeysToCopy.length; i++) {
      configs.put(longValuedKeysToCopy[i], configItem.getLongValue(longValuedKeysToCopy[i], defaultValuesForLongValuedKeys[i]));
    }

    configs.put(ADDITIONAL_FIELDS_KEY, configItem.getNVList(ADDITIONAL_FIELDS_KEY));

    return new S3OutputConfiguration(configs);
  }
}
