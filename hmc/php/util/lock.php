<?php

$logger = new HMCLogger("Lock");
$lockFile = $GLOBALS['HMC_CLUSTER_PATH']."/lockfile";
$GLOBALS['fileHdl'] = 0;


function LockAcquire($suffixT = "") {
  global $lockFile;
  global $logger;
  $suffix = strtolower($suffixT);

  $suffixLockFile = $lockFile;
  if ($suffix != "") {
    $suffixLockFile = $lockFile.".".$suffix;
  }

  if ($suffix != "" && !file_exists($suffixLockFile)) {
    $h = fopen($suffixLockFile, "x");
    if ($h !== FALSE) {
      fclose($h);
    }
  }

  $logger->log_trace("About to acquire lock using $suffixLockFile for pid: " .
    json_encode(posix_getpid()));
  $fileHdl = fopen($suffixLockFile, "r");
  $GLOBALS['fileHdl'.$suffix] = $fileHdl;
  $retval = flock($fileHdl, LOCK_EX);
  /* Uncomment for debugging purposes
  $e = new Exception;
  $logger->log_trace("Acquired Lock Status for Process: "
    . json_encode(posix_getpid()). " : ". json_encode($retval) . " backtrace: ".
  $e->getTraceAsString());
  unset($e);
   */
  return;
}

function LockRelease($suffixT = "") {
  global $lockFile;
  global $logger;
  $suffix = strtolower($suffixT);

  $suffixLockFile = $lockFile;
  if ($suffix != "") {
    $suffixLockFile = $lockFile.".".$suffix;
  }

  $logger->log_trace("Trying to release lock using $suffixLockFile for process: "
     . json_encode(posix_getpid()));
  $fileHdl = $GLOBALS['fileHdl'.$suffix];
  $retval = flock($fileHdl, LOCK_UN);
  /* Uncomment for debugging purposes
  $e = new Exception;
  $logger->log_trace("Released Lock Status for Process: "
  . json_encode(posix_getpid()). " : ". json_encode($retval) . " backtrace: ".
  $e->getTraceAsString());
  unset($e);
   */
}

?>
