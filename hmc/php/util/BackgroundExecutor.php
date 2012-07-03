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


function doParentExit($errCode) {
  print "Done with parent\n";
  exit ($errCode);
}

function usage() {
  $usageStr = <<<USAGEDOC
Usage:
  --txn-id       Transaction ID
  --no-fork      Whether to fork again
  --command      Command to run
  --args         Args for command ( optional )
  --logfile      Logfile to redirect output to
  --help         Print this help message.
USAGEDOC;

  print $usageStr . "\n";
}

$shortopts  = "ht:c:a:l:n";
$longopts  = array(
    "txn-id:",     // Txn ID
    "command:",    // Command to execute
    "args:",      // Args for command
    "logfile:",   // Log file
    "no-fork",     // Whether to fork again - by default always forks
    "help",        // Help - print usage
);

$options = @getopt($shortopts, $longopts);

$txnId = "";
$command = "";
$args = "";
$logFile = "";
$doFork = TRUE;

// print "Executing process " . implode (" ", $argv) . "\n";

foreach ($options as $opt => $val) {
  if ($opt == "txn-id" || $opt == "t") {
    $txnId = $val;
  }
  else if ($opt == "command" || $opt == "c") {
    $command = $val;
  }
  else if ($opt == "args" || $opt == "a") {
    $args = $val;
  }
  else if ($opt == "logfile" || $opt == "l") {
    $logFile = $val;
  }
  else if ($opt == "no-fork" || $opt == "n") {
    $doFork = FALSE;
  }
  else if ($opt == "help" || $opt == "h") {
    usage();
    doParentExit (0);
  }
  else {
    print "Invalid option passed to script, option=" . $opt . "\n";
    usage();
    doParentExit (1);
  }
}

if ($command == "" || $txnId == "") {
  print "One of transaction id or command not specified\n";
  usage();
  doParentExit (1);
}

$execCommand = "$command $args";
if ($logFile != "") {
  $execCommand .= " > $logFile 2>&1 ";
}
print "Executing $execCommand\n";

if ($doFork) {
  $pid = pcntl_fork();
  if ($pid === FALSE || $pid == -1) {
    print "Could not fork process\n";
    doParentExit (1);
  } else if ($pid > 0) {
    flush();
    print "Background Child Process PID:$pid\n";
    flush();
    doParentExit (0);
  }
}

// in child process or if fork disabled
$output = array();
unset($output);
$errCode = 0;
exec($execCommand, $output, $errCode);
exit ($errCode);

?>
