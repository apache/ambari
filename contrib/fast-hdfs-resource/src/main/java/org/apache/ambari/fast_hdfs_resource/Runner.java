/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.fast_hdfs_resource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import com.google.gson.Gson;

public class Runner {
  public static void main(String[] args)
      throws IOException, URISyntaxException {
    // 1 - Check arguments
    if (args.length != 2) {
      System.err.println("Incorrect number of arguments. Please provide:\n"
          + "1) Path to file with json\n"
          + "2) Path to Hadoop FS (fs.default.name form core-site.xml)\n"
          + "Exiting...");
      System.exit(1);
    }

    // 2 - Check if json-file exists
    final String jsonFilePath = args[0];
    final String fsName = args[1];
    File file = new File(jsonFilePath);

    if (!file.isFile()) {
      System.err
          .println("File " + jsonFilePath + " doesn't exist.\nExiting...");
      System.exit(1);
    }

    Gson gson = new Gson();
    Resource[] resources = null;
    DistributedFileSystem dfs = null;

    try {
      dfs = new DistributedFileSystem();

      // 3 - Load data from JSON
      resources = (Resource[]) gson.fromJson(new FileReader(jsonFilePath),
          Resource[].class);

      // 4 - Connect to HDFS
      dfs.initialize(new URI(fsName), new Configuration());

      for (Resource resource : resources) {
        System.out.println("Creating: " + resource);

        Resource.checkResourceParameters(resource, dfs);

        Path pathHadoop = new Path(resource.getTarget());
        if (resource.getAction().equals("create")) {
          // 5 - Create
          Resource.createResource(resource, dfs, pathHadoop);
          Resource.setMode(resource, dfs, pathHadoop);
          Resource.setOwner(resource, dfs, pathHadoop);
        } else if (resource.getAction().equals("delete")) {
          // 6 - Delete
          dfs.delete(pathHadoop, true);
        }
      }

    } finally {
      dfs.close();
    }

    System.out.println("All resources created.");
  }

}
