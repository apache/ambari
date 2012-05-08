<?php

$clusterName = $_GET['clusterName'];

header("Content-type: application/json");

/* All we need to do is give back a new txnId. */
$txnId = rand();

error_log( 'Generated new txnId: ' . $txnId );

$jsonOutput = array( 
  'txnId' => $txnId,
  'clusterName' => $clusterName );

print (json_encode($jsonOutput));

?>
