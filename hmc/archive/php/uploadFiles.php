<?php

// include_once 'createCluster.php';
include_once 'dbLib.php';

//$requestData = file_get_contents('php://input');
//error_log($requestdata, 3, "/tmp/rez.log");
//$requestObj = json_decode($requestdata, true);

$clusterName = $_GET['clusterName'];
// Validate clusterName: TODO; FIXME

// initCluster($clusterName);

$identityFileDestination = getSshKeyFilePath($clusterName);
$hostsFileDestination = getHostsFilePath($clusterName);

// TODO: FIXME: Change echos to return error codes.
//echo '<pre>';
if (move_uploaded_file($_FILES['clusterDeployUserIdentityFile']['tmp_name'], $identityFileDestination)) {
    //echo "File is valid, and was successfully uploaded.\n";

  // Set the permissions
  chmod($identityFileDestination, 0400);// TODO: Error conditions
} else {
    //echo "Possible file upload attack!\n";
}

if (move_uploaded_file($_FILES['clusterHostsFile']['tmp_name'], $hostsFileDestination)) {
    //echo "File is valid, and was successfully uploaded.\n";
  } else {
    //echo "Possible file upload attack!\n";
}

// TODO: FIXME: Check file lengths.

$outjson = array(
                  "errorCode"=> 0,
 //                 "clusterName" => $clusterName,
 //                 "finalDestination" => $identityFileDestination,
 //                 "fileToBeMoved" => $_FILES['clusterDeployUserIdentityFile']['tmp_name'],
                );
header("Content-type: application/json");
print (json_encode($outjson));
?>
