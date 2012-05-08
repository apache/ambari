<?php

if (!isset($GLOBALS["puppetManifestDir"])) {
  $GLOBALS["puppetManifestDir"] = "/etc/puppet/master/manifests";
}
if (!isset($GLOBALS["puppetKickVersionFile"])) {
  $GLOBALS["puppetKickVersionFile"] = "/var/lib/puppet/puppet_kick_version.txt";
}
if (!isset($GLOBALS["puppetReportsDir"])) {
  $GLOBALS["puppetReportsDir"] = "/var/lib/puppet/reports";
}
if (!isset($GLOBALS["puppetKickTimeout"])) {
  $GLOBALS["puppetKickTimeout"] = 300;
}
if (!isset($GLOBALS["puppetMaxParallelKicks"])) {
  $GLOBALS["puppetMaxParallelKicks"] = 10;
}
if (!isset($GLOBALS["DRYRUN"])) {
  $GLOBALS["DRYRUN"] = FALSE;
}
if (!isset($GLOBALS["puppetModulesDirectory"])) {
  $GLOBALS["puppetModulesDirectory"] = "/etc/puppet/master/modules";
}

?>
