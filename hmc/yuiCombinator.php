<?php

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
    $contentType = 'application/js';
  }

  return $contentType;
}

/* main() */
$filesToLoad = explode('&', $_SERVER['QUERY_STRING']);

$contentType = '';
$responseBody = '';

foreach ($filesToLoad as $fileToLoad) 
{
  /* Assumes a request has only homogenous file types, which holds true for 
   * the combined requests YUI makes. 
   */
  if (empty($contentType))
  {
    $contentType = deduceContentType($fileToLoad);
  }

  $fileContents = file_get_contents('./' . $fileToLoad);

  if ($fileContents)
  {
    $responseBody .= $fileContents;
  }
}

header('Content-type: ' . $contentType);
/* TODO XXX Add appropriate Cache-Control/Age/Last-Modified/Expires headers 
 * here to be super-efficient. 
 */

echo $responseBody;

?>
