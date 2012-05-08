<?php

$stagesInfo = array(
    "CleanupProcesses" => array(
      "description" => "Cleaning up processes",
      "scriptName" => "./uninstall/cleanupProcesses.php",
      ),
    "YumCleanupAndHttpDStop" => array(
      "description" => "Yum cleanup and httpd stop",
      "scriptName" => "./uninstall/yumCleanupHttpdStop.php",
      ),
    "LogCleanup" => array(
      "description" => "Log directories cleanup",
      "scriptName" => "./uninstall/deleteLogDirs.php",
      ),
    );

?>
