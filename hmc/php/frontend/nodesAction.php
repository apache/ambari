<?php

include_once '../util/Logger.php';
include_once '../conf/Config.inc';
include_once 'localDirs.php';
include_once "../util/lock.php";
include_once '../db/HMCDBAccessor.php';

include_once 'commandUtils.php';
include_once "../util/HMCTxnUtils.php";

$logger = new HMCLogger("setupNodes");
$dbAccessor = new HMCDBAccessor($GLOBALS["DB_PATH"]);

$clusterName = $_GET['clusterName'];
$action = $_GET['action'];
$deployUser = $_POST['ClusterDeployUser'];

$propertiesArr = $dbAccessor->getConfigPropertiesMetaInfo();
if ($propertiesArr["result"] != 0) {
  $logger->log_error("Could not get config meta info from DB, error="
      . $propertiesArr["error"]);
  print json_encode(array( "result" => 1, "error" => "Error in config properties meta info"));
  return;
}

// Use meta info defaults
// Override with current svc configs
// Override with POST params

$repoFilePath = $propertiesArr["configs"]["yum_repo_file"]["value"];
$hdpArtifactsDownloadUrl = $propertiesArr["configs"]["apache_artifacts_download_url"]["value"];
$gplArtifactsDownloadUrl = $propertiesArr["configs"]["gpl_artifacts_download_url"]["value"];

$currentConfigs = $dbAccessor->getServiceConfig($clusterName);
if ($currentConfigs["result"] != 0) {
  $logger->log_error("Could not get configs from DB, error="
      . $currentConfigs["error"]);
  print json_encode(array( "result" => 1, "error" => "Could not get configs from DB"));
  return;
}

if (isset($currentConfigs["properties"]["yum_repo_file"])
    && $currentConfigs["properties"]["yum_repo_file"] != "") {
  $repoFilePath = $currentConfigs["properties"]["yum_repo_file"];
}

if (isset($currentConfigs["properties"]["apache_artifacts_download_url"])
    && $currentConfigs["properties"]["apache_artifacts_download_url"] != "") {
  $hdpArtifactsDownloadUrl = $currentConfigs["properties"]["apache_artifacts_download_url"];
}

if (isset($currentConfigs["properties"]["gpl_artifacts_download_url"])
    && $currentConfigs["properties"]["gpl_artifacts_download_url"] != "") {
  $gplArtifactsDownloadUrl = $currentConfigs["properties"]["gpl_artifacts_download_url"];
}

if (isset($_POST['yumRepoFilePath'])
    && trim($_POST['yumRepoFilePath']) != "") {
  $repoFilePath = trim($_POST['yumRepoFilePath']);
}

if (isset($_POST['hdpArtifactsDownloadUrl'])
    && trim($_POST['hdpArtifactsDownloadUrl']) != "") {
  $hdpArtifactsDownloadUrl = trim($_POST['hdpArtifactsDownloadUrl']);
}

if (isset($_POST['gplArtifactsDownloadUrl'])
    && trim($_POST['gplArtifactsDownloadUrl']) != "") {
  $gplArtifactsDownloadUrl = trim($_POST['gplArtifactsDownloadUrl']);
}

header("Content-type: application/json");

if (!file_exists($repoFilePath)) {
  $logger->log_warn("Invalid repo file provided, file does not exist"
      . ", repoFile=" . $repoFilePath);
  print (json_encode(array(
      "result" => 1,
      "error" => "Invalid repo file path specified"
  )
  ));
  return;
}

// TODO - error checks for download urls
/*
if (parse_url($hdpArtifactsDownloadUrl) === FALSE
    || parse_url($gplArtifactsDownloadUrl) === FALSE) {
  $logger->log_warn("Invalid download urls provided, could not parse"
      . ", hdpArtifactsDownloadUrl=" . $hdpArtifactsDownloadUrl
      . ", gplArtifactsDownloadUrl=" . $gplArtifactsDownloadUrl);

  print (json_encode(array(
        "result" => 1,
        "error" => "Invalid download urls specified")));
  return;
}
*/

$configs =  array ( "yum_repo_file" => $repoFilePath,
                    "apache_artifacts_download_url" => $hdpArtifactsDownloadUrl,
                    "gpl_artifacts_download_url" => $gplArtifactsDownloadUrl);
$dbResponse = $dbAccessor->updateServiceConfigs($clusterName, $configs);
if ($dbResponse["result"] != 0) {
  $logger->log_error("Got error while persisting configs: ".$dbResponse["error"]);
  return $dbResponse;
}

$stagesFiles = "";
if ($action == "addNodes") {
  $stagesFile = "./addNodes/stages.php";
} else if ($action == "uninstall") {
  $stagesFile = "./uninstall/stages.php";
} else {
  print (json_encode(array(
    "result" => 1,
    "error" => "Invalid action",
  )
  ));
  return;
}

error_log("ClusterName: ".$clusterName);

$logger->log_debug("ClusterName: $clusterName\n");
$logger->log_debug("Deploy User: $deployUser\n");

// this api just creates a new transaction id for the db
$status = "";
$createTxResult = HMCTxnUtils::createNewTransaction($dbAccessor, $clusterName, $status);
if ($createTxResult == FALSE) {
  $msg = "SETUP: Failed to create new transaction in background: $createTxResult\n";
  $logger->log_error($msg);
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
}

$rootTxnId = $createTxResult;
$logger->log_error("Txn Id ===== $rootTxnId\n");
$logger->log_error("Clustername ===== $clusterName");

$cmd = $GLOBALS["PHP_EXEC_PATH"] . " " . "../util/sequentialScriptRunner.php";
// $cmd = $GLOBALS["PHP_EXEC_PATH"] . " " . "./addNodes/addNodesOrchestrator.php";

$hostsFile = getHostsFilePath($clusterName);

$args = "$clusterName $deployUser $rootTxnId $hostsFile $stagesFile";

$execBackgroundResult = HMCTxnUtils::execBackgroundProcess($dbAccessor, $clusterName, $rootTxnId, $cmd, $args, "");
if ($execBackgroundResult == FALSE) {
  $msg = "Failed to execute addNodesOrchestrator in background: $execBackgroundResult\n";
  $logger->log_error($msg);
  print (json_encode(array("result" => 1, "error" => "$msg")));
  return;
}

print (json_encode(array(
  "result" => 0,
  "error" => "",
  "response" => array(
    "clusterName" => $clusterName,
    "txnId" => $rootTxnId,
  )
)
));

?>
