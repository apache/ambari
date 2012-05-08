<?php

$requestdata = file_get_contents('php://input');

error_log($requestdata, 3, "/tmp/rez.log");

$requestjson = json_decode($requestdata, true);

header("Content-type: application/json");

$x = array( 'a' => 'b', 'c' => 'd', 'e' => array(1,2,3) );

print(json_encode($x));

?>
