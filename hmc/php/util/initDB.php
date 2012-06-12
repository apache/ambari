<?php
include_once '../util/Logger.php';
include_once "../conf/Config.inc";

print "database is " . $GLOBALS["DB_PATH"] . "\n";
$db = new PDO("sqlite:" . $GLOBALS["DB_PATH"]);
$sql = file_get_contents(realpath( $GLOBALS["DB_PATH"] );

$db->exec($sql) or die(print_r($db->errorInfo(), true));

?>
