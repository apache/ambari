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
package org.apache.ambari.metrics.adservice.prototype.methods.ema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.SparkContext;
import org.apache.spark.mllib.util.Loader;

public class EmaModelLoader implements Loader<EmaTechnique> {
    private static final Log LOG = LogFactory.getLog(EmaModelLoader.class);

    @Override
    public EmaTechnique load(SparkContext sc, String path) {
        return new EmaTechnique(0.5,3);
//        Gson gson = new Gson();
//        try {
//            String fileString = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
//            return gson.fromJson(fileString, EmaTechnique.class);
//        } catch (IOException e) {
//            LOG.error(e);
//        }
//        return null;
    }
}
