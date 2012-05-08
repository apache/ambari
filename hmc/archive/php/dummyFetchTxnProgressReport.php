<?php

$txnId = $_GET['txnId'];

define('LAST_PROGRESS_STATE_INDEX_FILE', '/tmp/rezDeployProgressStateIndex' . $txnId);

function fetchLastProgressStateIndex()
{
  $lastProgressStateIndex = 0;

  if( file_exists(LAST_PROGRESS_STATE_INDEX_FILE) )
  {
    $lastProgressStateIndex = trim( file_get_contents(LAST_PROGRESS_STATE_INDEX_FILE) );
  }

  return $lastProgressStateIndex;
}

function storeLastProgressStateIndex( $latestProgressStateIndex )
{
  file_put_contents(LAST_PROGRESS_STATE_INDEX_FILE, $latestProgressStateIndex);
}

$lastProgressStateIndex = fetchLastProgressStateIndex();

$progressStates = array( 'State1', 'State2', 'State3', 'State4', 'State5' );
$currentProgressStateIndex = $lastProgressStateIndex;

/* Progress to the next state only if we haven't already reached the end. 
 *
 * We expect callers to stop to call this webservice once this condition is
 * reached in any case, but let's be safe all the same.
 */
if( $lastProgressStateIndex < count($progressStates) )
{
  /* Randomize the rate of our progress, in steps of 1. */
  $currentProgressStateIndex = (rand() % 2) ? ($lastProgressStateIndex + 1) : ($lastProgressStateIndex);

  /* Update our disk cookie. */
  storeLastProgressStateIndex( $currentProgressStateIndex );
}

$jsonOutput = array( 
    'txnId' => $txnId, 
    'progressStates' => $progressStates,
    'currentProgressStateIndex' => $currentProgressStateIndex,
    'encounteredError' => (rand() % 20) ? false : true );

header("Content-type: application/json");

print (json_encode($jsonOutput));

?>
