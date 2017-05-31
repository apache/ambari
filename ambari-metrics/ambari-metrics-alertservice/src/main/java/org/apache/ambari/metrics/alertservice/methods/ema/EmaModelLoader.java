/**
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
package org.apache.ambari.metrics.alertservice.methods.ema;

import com.google.gson.Gson;
import com.sun.org.apache.commons.logging.Log;
import com.sun.org.apache.commons.logging.LogFactory;
import org.apache.spark.SparkContext;
import org.apache.spark.mllib.util.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmaModelLoader implements Loader<EmaModel> {
    private static final Log LOG = LogFactory.getLog(EmaModelLoader.class);

    @Override
    public EmaModel load(SparkContext sc, String path) {
        Gson gson = new Gson();
        try {
            String fileString = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            return gson.fromJson(fileString, EmaModel.class);
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }
}
