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
