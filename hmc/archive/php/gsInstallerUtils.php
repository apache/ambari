<?php

$gsInstallerInstallDir = "/var/lib/gsInstaller/";

/**
 * Function to copy over gsInstaller into a working dir
 * where we can override settings and execute the deploy
 */
function setupGsInstaller($clusterId) {
  // empty work dir
  $workDir = getClusterDir($clusterId)."gsInstaller/";
  $retval = 0;
  system("rm -rf $workDir/*", $retval);
  // if ($retval != 0) {
    // die("Could not empty $workDir, error=$retval");
  //}
  $retval = 0;
  system("mkdir -p $workDir", $retval);
  //if ($retval != 0) {
    //die("Could not empty $workDir, error=$retval");
  //}
  
  // copy gs install code into work dir
  global $gsInstallerInstallDir;
  system("cp -rf $gsInstallerInstallDir/* $workDir/", $retval);
  if ($retval != 0) {
    die("Could not copy gsInstaller into $workDir, error=$retval");
  }  
}

function copyGsInstallPropsToRunDir($clusterId, $gsInstallPropsFile) {
  $workDir = getClusterDir($clusterId)."gsInstaller/";
  $retval = 0;
  system("cp -f $gsInstallPropsFile $workDir/*", $retval);
  if ($retval != 0) {
    die("Could not copy $gsInstallPropsFile into $workDir, error=$retval");
  }
}

function copyHostRoleFilesToRunDir($clusterId) {
  $workDir = getClusterDir($clusterId)."gsInstaller/";
  
  $file_list = array (
     "dashboardhost",
     "gangliaserver",
     "gateway",
     "hbasemaster",
     "hbasenodes",
     "hcatserver",
     "jobtracker",
     "nagiosserver",
     "namenode",
     "nodes",
     "oozieserver",
     "snamenode",
     "templetonnode",
     "zknodes",
     "gsInstaller.properties",
     "gsCluster.properties",
     "monInstaller.properties",
   );

  $errors = "";
  $installerInputDir = getInstallerInputDir($clusterId);
  
  foreach ($file_list as $filename) {
    $filepath = $installerInputDir.$filename;
    $retval = 0;
    system("cp -f $filepath $workDir/$filename", $retval);
    if ($retval != 0) {
      $errors .= ", could not copy $filepath to $workDir: error=$retval";
    }
  }
  
  if ($errors != "") {
    die ("Copy of host roles files failed, $errors");
  }
}

function triggerGsUnInstaller($clusterId) {
  $workDir = getClusterDir($clusterId)."gsInstaller/";
  $retval = 0;

  error_log("Running gs Uninstaller");
  $logfile = $workDir."gsUnInstall-console.out";
  $cmd="/var/www/html/KickAssHDPUI/cpp/runscriptasroot \""
          . " cd $workDir; "
          . " echo \"\" > $logfile " 
          . " echo y | sh ./gsUninstaller.sh 2>&1 >> $logfile; "
          . " \"";

  $handle = popen($cmd, "r");
  // Do some checks on return status: TODO
  //print $cmd;
  pclose($handle);
}

function triggerGsInstaller($clusterId) {
  $workDir = getClusterDir($clusterId)."gsInstaller/";
  $retval = 0;


  error_log("Running gs installer");
  $logfile = $workDir."gsInstall-console.out";
  $cmd="/var/www/html/KickAssHDPUI/cpp/runscriptasroot \""
          . " cd $workDir; "
          . " echo \"\" > $logfile " 
          . " sh createUsers.sh 2>&1 >> $logfile; "
          . " sh gsPreRequisites.sh 2>&1 >> $logfile; "
          . " echo y | sh ./gsInstaller.sh 2>&1 >> $logfile; "
          . " \"";

  error_log("Starting gsinstaller with $cmd".date("U"));
  $handle = popen($cmd, "r");
  // Do some checks on return status: TODO
  //print $cmd;
  pclose($handle);

  // shell_exec($cmd);
  error_log("Done with gsinstaller".date("U"));

  # TODO error check

  # TODO run moninstaller

  return $retval;
}

function getGsInstallerLogPath($clusterId) {
  return getClusterDir($clusterId)."gsInstaller/gsInstall.log";
}

function getGsInstallerStatusDumpPath($clusterId) {
  return getClusterDir($clusterId)."gsInstaller/gsInstallStatusDump.out";
}

function getLastNLinesFromGsInstallLogs($clusterId, $lastNlines) {
  $cmd = "tail -".$lastNlines." ".getGsInstallerLogPath($clusterId);
  $handle = popen($cmd, "r");
  // TODO error check
  $output = "";
  while (!feof($handle)) {
    $read = fread($handle, 2096);
    $output .= $read;
  }
  pclose($handle);
  return $output;
}

function getGsInstallLogs($clusterId) {
  if (!file_exists(getGsInstallerLogPath($clusterId))) {
     return FALSE;
  }
  return file_get_contents(getGsInstallerLogPath($clusterId));
}

function isGsInstallDone($clusterId) {
  $retval = 0;
  $outdump = array();
  $psGrepOutput = exec("ps -ef | grep -i \"gsInstaller.sh\" | grep -v grep | wc -l", $outdump, $retval);
  $output = trim($psGrepOutput);

  if ($retval != 0 || $output == "0") {
    return TRUE;
  }
  return FALSE;
}

// NULL for error
// 0 for success
// 1 for error
// -1 for installer running
// -2 for not installed
function getGsInstallExitStatus($clusterId) {
  $statusFile = getGsInstallerStatusDumpPath($clusterId);
  if (!file_exists($statusFile)) {
    return NULL;
  }
  $cmd = "grep \"^EXIT \" $statusFile";
  $retval = "";
  $output = array();
  $retcode = 0;
  $retval = exec($cmd, $output, $retcode);

  $installDone = isGsInstallDone($clusterId);
  if (!$installDone) {
     return -1;
  }

  if (!hasInstallStarted($clusterId)) {
     return -2;
  }

  if ($retcode != 0 && $installDone) {
     error_log("Install done but no exit stmt in status output");
     return NULL;
  }

  if (!defined($retval) || $retval == "") {
    return NULL;
  }

  if (strpos($retval, "EXIT OK") == 0) {
     return 0;
  }

  if (strpos($retval, "EXIT ERROR") == 0) {
     return 1;
  }

  return NULL;
}



?>
