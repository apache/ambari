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


if (!isset($GLOBALS["puppetManifestDir"])) {
  $GLOBALS["puppetManifestDir"] = "/etc/puppet/master/manifests";
}
if (!isset($GLOBALS["puppetKickVersionFile"])) {
  $GLOBALS["puppetKickVersionFile"] = "/var/lib/puppet/puppet_kick_version.txt";
}
if (!isset($GLOBALS["puppetReportsDir"])) {
  $GLOBALS["puppetReportsDir"] = "/var/lib/puppet/reports";
}
if (!isset($GLOBALS["puppetKickTimeout"])) {
  $GLOBALS["puppetKickTimeout"] = 300;
}
if (!isset($GLOBALS["puppetMaxParallelKicks"])) {
  $GLOBALS["puppetMaxParallelKicks"] = 10;
}
if (!isset($GLOBALS["DRYRUN"])) {
  $GLOBALS["DRYRUN"] = FALSE;
}
if (!isset($GLOBALS["puppetModulesDirectory"])) {
  $GLOBALS["puppetModulesDirectory"] = "/etc/puppet/master/modules";
}

?>
