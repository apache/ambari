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


function deduceContentType ($fileToLoad)
{
  $contentType = '';

  $fileExtension = pathinfo($fileToLoad, PATHINFO_EXTENSION);

  if ($fileExtension == 'css')
  {
    $contentType = 'text/css';
  }
  elseif ($fileExtension == 'js' )
  {
    $contentType = 'application/x-javascript';
  }

  return $contentType;
}

/* main() */
$filesToLoad = explode('&', $_SERVER['QUERY_STRING']);

$contentType = '';
$responseBody = '';
$servingYuiFile = false;

foreach ($filesToLoad as $fileToLoad) 
{
  /* Assumes a request has only homogenous file types, which holds true for 
   * the combined requests YUI makes. 
   */
  if (empty($contentType))
  {
    $contentType = deduceContentType($fileToLoad);

    if (preg_match('/^yui/', $fileToLoad))
    {
      $servingYuiFile = true;
    }
  }

  $fileContents = file_get_contents('./' . $fileToLoad);

  if ($fileContents)
  {
    $responseBody .= $fileContents;
  }
}

header('Content-type: ' . $contentType);
header('Content-Length: ' . strlen($responseBody));

/* When we serve YUI files, make sure they're cached for a long time. */
if( $servingYuiFile )
{
  $validitySecs = 24 * 60 * 60; /* 1 day */

  header('Cache-Control: max-age=' . $validitySecs . ', must-revalidate, public');
}

echo $responseBody;

?>
