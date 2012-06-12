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

/**
 * Parse repo file and get all enabled gpg keys
 * @param string $repoFile
 * @return mixed
 *   array (
 *      $currentRepoId = array ( "gpgkey" => $currentGpgLocation),
 *      ....
 *      )
 */
function getEnabledGpgKeyLocations($repoFile) {
  $logger = new HMCLogger("YumRepoConfigParser");

  $logger->log_info("Parsing gpg key info from " . $repoFile);
  if (!file_exists($repoFile)) {
    $logger->log_error("Invalid repo file provided, file=" . $repoFile);
    return FALSE;
  }

  $fileContents = file($repoFile,
      FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES );

  $logger->log_debug("Repo file contents, file=" . $repoFile
      . ", contents=" . print_r($fileContents, true));

  $response = array();

  $currentRepoId = "";
  $currentRepoEnabled = 1;
  $currentGpgCheck = -1;
  $currentGpgLocation = "";
  $globalGpgCheck = 0;

  foreach ($fileContents as $fLine) {
    $line = trim($fLine);
    if (substr($line, 0, 1) == "#") {
      continue;
    }

    $matches = array();
    $logger->log_debug("Parsing repo file, file=" . $repoFile
        . ", line=" . $line);
    $logger->log_debug("Current: "
        . ", repoId=" . $currentRepoId
        . ", repoEnabled=" . $currentRepoEnabled
        . ", gpgCheckEnabled=" . $currentGpgCheck
        . ", gpgCheckLocation=" . $currentGpgLocation);

    if (preg_match("/\[(.*)\]/", $line, $matches) > 0) {
      $newRepoId = $matches[1];
      $logger->log_debug("Found new repo id in repo file, file=" . $repoFile
          . ", repoId=" . $newRepoId);
      if ($currentRepoEnabled == 1
          && (($currentGpgCheck == -1 && $globalGpgCheck == 1)
              || ($currentGpgCheck == 1))) {
        if ($currentGpgLocation != "") {
          $logger->log_debug("Adding gpgkey $currentGpgLocation for repo"
              .", id=" . $currentRepoId);
          $response[$currentRepoId] = array ( "gpgkey" => $currentGpgLocation);
        }
      } else if ($currentRepoId != ""
          && $currentRepoId != "main") {
        $logger->log_debug("Skipping repo as repo/check not enabled"
            .", id=" . $currentRepoId);
      }
      $currentRepoId = $newRepoId;
      $currentGpgLocation = "";
      $currentGpgCheck = -1;
      $currentRepoEnabled = -1;
      continue;
    }

    $eIdx = strpos($line, "=");
    if ($eIdx === FALSE) {
      $logger->log_warn("Invalid line when parsing repo file, file=" . $repoFile
          . ", line=" . $line);
      continue;
    }

    $key = trim(substr($line, 0, $eIdx));
    $val = trim(substr($line, $eIdx+1));

    $logger->log_debug("Parsed line, key=" . $key . ", val=" . $val);

    if ($key == "gpgcheck") {
      if ($currentRepoId == "main") {
        $globalGpgCheck = intval($val);
      } else {
        $currentGpgCheck = intval($val);
      }
    } else if ($key == "enabled") {
      $currentRepoEnabled = intval($val);
    } else if ($key == "gpgkey") {
      $currentGpgLocation = $val;
    }
  }

  if ($currentRepoEnabled == 1
      && (($currentGpgCheck == -1 && $globalGpgCheck == 1)
           || ($currentGpgCheck == 1))) {
    if ($currentGpgLocation != "") {
      $response[$currentRepoId] = array ( "gpgkey" => $currentGpgLocation);
    }
  } else if ($currentRepoId != ""
      && $currentRepoId != "main") {
    $logger->log_debug("Skipping repo as repo/check not enabled"
        .", id=" . $currentRepoId);
  }

  return $response;
}

?>
