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
import org.apache.hadoop.fs.FileSystem;

import com.google.gson.Gson;

public class Runner {
  public static void main(String[] args)
      throws IOException, URISyntaxException {
    // 1 - Check arguments
    if (args.length != 1) {
      System.err.println("Incorrect number of arguments. Please provide:\n"
          + "1) Path to json file\n"
          + "Exiting...");
      System.exit(1);
    }

    // 2 - Check if json-file exists
    final String jsonFilePath = args[0];
    File file = new File(jsonFilePath);

    if (!file.isFile()) {
      System.err
          .println("File " + jsonFilePath + " doesn't exist.\nExiting...");
      System.exit(1);
    }

    Gson gson = new Gson();
    Resource[] resources = null;
    FileSystem dfs = null;

    try {
      Configuration conf = new Configuration();
      dfs = FileSystem.get(conf);

      // 3 - Load data from JSON
      resources = (Resource[]) gson.fromJson(new FileReader(jsonFilePath),
          Resource[].class);

      // 4 - Connect to HDFS
      System.out.println("Using filesystem uri: " + FileSystem.getDefaultUri(conf).toString());
      dfs.initialize(FileSystem.getDefaultUri(conf), conf);
      
      for (Resource resource : resources) {
        System.out.println("Creating: " + resource);

        Resource.checkResourceParameters(resource, dfs);

        Path pathHadoop = new Path(resource.getTarget());
        if (!resource.isManageIfExists() && dfs.exists(pathHadoop)) {
          System.out.println("Skipping the operation for not managed DFS directory " + resource.getTarget() +
                             " since immutable_paths contains it.");
          continue;
        }

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
    } 
    catch(Exception e) {
       System.out.println("Exception occurred, Reason: " + e.getMessage());
       e.printStackTrace();
    }
    finally {
      dfs.close();
    }

    System.out.println("All resources created.");
  }

}
