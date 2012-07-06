<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


include_once '../../../php/util/Logger.php';
include_once '../../../php/conf/Config.inc';
include_once "../../../php/util/lock.php";
include_once '../../../php/db/HMCDBAccessor.php';

$GLOBALS["HMC_LOG_LEVEL"] = HMCLogger::DEBUG;
$GLOBALS["HMC_LOG_FILE"] = "./hmc.log";

system("rm -rf ./hmc.log");

assert_options(ASSERT_BAIL, 1);

$db = new HMCDBAccessor("data.db");

$result = $db->getCurrentAmbariVersion();
print_r($result);
$result = $db->getCurrentClusterStackVersion("pptest");

?>
