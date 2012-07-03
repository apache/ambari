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


/*
$dbHandle = sqlite_popen("../../../db/data.db", 666, $error);
assert($dbHandle !== FALSE);

$schema_dump = file_get_contents("../../../db/schema.dump");
$ret = sqlite_exec($dbHandle, sqlite_escape_string($schema_dump), $error);
assert($ret == TRUE);

print realpath("../../../db/data.db");
$pdo = new PDO("sqlite:".realpath("../../../db/data.db"));
$sql = file_get_contents(realpath( '../../../db/schema.dump') );

//$sql = str_replace( '?', $sql );
//$st = $pdo->prepare( $sql, array( PDO::ATTR_EMULATE_PREPARES => true ) );
//$st = $pdo->prepare( $sql);
//$st = $pdo->exec("CREATE TABLE Clusters (  cluster_name TEXT PRIMARY KEY, version TEXT, state TEXT )");
//print "\nfirst one".$st;
//$result=$st->execute();
*/

//$db = new PDO("sqlite:/var/www/html/KickAssHDPUI/db/data.db");
$db = new PDO("sqlite:".$argv[1]);

// $db = new PDO("sqlite:/tmp/testing1.db");
//print $db->exec("CREATE TABLE Clusters (  cluster_name TEXT PRIMARY KEY, version TEXT, state TEXT )");
//print "\n";
//$ret = $db->exec("CREATE TABLE testing (Id INTEGER)");
#$sql = file_get_contents(realpath( '/var/www/html/KickAssHDPUI/db/schema.dump') );
$sql = file_get_contents(realpath( $argv[2]));
//$sql = file_get_contents(realpath( '/var/www/html/KickAssHDPUI/db/test.dump') );
// print $sql;


/*
$sql = 'BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS "Clusters" (
  cluster_name TEXT, -- Cluster name
  version TEXT, -- version of the hadoop stack installed
  state, TEXT, -- cluster state - installed, config in progress etc
  PRIMARY KEY (cluster_name)
);


COMMIT;';
*/

$db->exec($sql) or die(print_r($db->errorInfo(), true));

//$st = $db->exec("CREATE TABLE Clusters (  cluster_name TEXT PRIMARY KEY, version TEXT, state TEXT )");
/*$st = $db->exec('CREATE TABLE "Clusters" (
  cluster_name TEXT, -- Cluster name
    version TEXT, -- version of the hadoop stack installed
      state TEXT, -- cluster state - installed, config in progress etc
        PRIMARY KEY (cluster_name)
        );');
print "\nsecondone".$ret;
/*
$dbHandle = new SQLite3("../../../db/data.db", 666, $error);
assert($dbHandle !== FALSE);

$schema_dump = file_get_contents("../../../db/schema.dump");
$ret = $dbHandle->exec(sqlite_escape_string($schema_dump));
assert($ret == TRUE);

*/


// Show all information, defaults to INFO_ALL
//phpinfo();

?>
