<?php
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

// All end-points scripts are either required to include this file or
// set all the global vars defined in this file

// Debugging mode - whether to generate additional logs
$GLOBALS["HDP_MON_DEBUG_MODE"] = FALSE;

// Should be either an absolute path
// or relative based on location of dataServices dir
// as all end-point scripts are one level deep into dataServices
$GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"] =
  "conf/cluster_configuration.json";

?>
