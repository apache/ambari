<?php
function ReadNthWordsOfAllLinesFromFile($fileName, $num)
{
  $loc = $num - 1;
  if ($loc < 0) {
   $loc = $num;
  }
  $handle = fopen($fileName, "r");
  $retValue = array();
  if ($handle) {
    while (($buffer = fgets($handle, 4096)) !== false) {
      $fullLine = explode(' ', $buffer);
      array_push($retValue, trim($fullLine[$loc]));
    }
     
    if (!feof($handle)) {
      echo "Error: unexpected fgets() fail\n"; // TODO: Fix
    }
  }
  return $retValue;
}


?>
