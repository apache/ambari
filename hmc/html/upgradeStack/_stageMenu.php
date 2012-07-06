<?php
/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
*/

switch ($requestPage) {
  case 'uninstall.php':
    $uninstallStageClass = 'current';
    $reconfigureStageClass = 'unvisited';
    $deployStageClass = 'unvisited';
    break;
  case 'reconfigure.php':
    $uninstallStageClass = 'visited';
    $reconfigureStageClass = 'current';
    $deployStageClass = 'unvisited';
    break;
  case 'deploy.php':
    $uninstallStageClass = 'visited';
    $reconfigureStageClass = 'visited';
    $deployStageClass = 'current';
    break;
}

?>
  <div id="stageTabsContainer">
    <ul id="stageTabs">
      <li id="uninstallStage" class="<?php echo $uninstallStageClass ?>">
        <div>
          <span class="stageNumber">1.</span>
          Uninstall Services
        </div>
      </li>
      <li id="reconfigureStage" class="<?php echo $reconfigureStageClass ?>">
        <div>
          <span class="stageNumber">2.</span>
          Reconfigure Services
        </div>
      </li>
      <li id="deployStage" class="<?php echo $deployStageClass ?>">
        <div>
          <span class="stageNumber">3.</span>
          Deploy New Version
        </div>
      </li>
    </ul>
  </div>