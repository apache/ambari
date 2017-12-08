/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.detection

import org.apache.ambari.metrics.adservice.configuration.SparkConfiguration
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}
import org.apache.spark.launcher.SparkAppHandle.State

/**
  * Class to run Spark jobs given a set of arguments
  */
class SparkApplicationRunner{

  var config: SparkConfiguration = _
  val env: java.util.HashMap[String, String] = new java.util.HashMap()
  env.put("SPARK_PRINT_LAUNCH_COMMAND", "1")
  val sparkArgs: Map[String, String] = Map.empty

  def this(config: SparkConfiguration) = {
    this()
    this.config = config
  }

  /**
    * Run Spark Job.
    * @param appName Name of the application
    * @param appClass Application Class
    * @param appArgs Command Line args to be submitted to Spark.
    * @return Handle to the Spark job.
    */
  def runSparkJob(appName: String,
                  appClass: String,
                  appArgs: Iterable[String]): SparkAppHandle = {

    val launcher: SparkLauncher = new SparkLauncher(env)
      .setAppName(appName)
      .setSparkHome(config.getSparkHome)
      .setAppResource(config.getJarFile)
      .setMainClass(appClass)
      .setMaster(config.getMaster)

    for (arg <- appArgs) {
      launcher.addAppArgs(arg)
    }

    for ((name,value) <- sparkArgs) {
      launcher.addSparkArg(name, value)
    }

    val handle: SparkAppHandle = launcher.startApplication()
    handle
  }

  /**
    * Helper method to check if a Spark job is running.
    * @param handle
    * @return
    */
  def isRunning(handle: SparkAppHandle): Boolean = {
    handle.getState.equals(State.CONNECTED) || handle.getState.equals(State.SUBMITTED) || handle.getState.equals(State.RUNNING)
  }
}
