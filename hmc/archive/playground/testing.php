<?php
$outjson = '{
            "namenode": '.$namenodesuggestout.',
            "secondarynamenode": $secondarynamenodesuggestout,
            "jobtracker": $jobtrackersuggestout,
            "gangliacollector": $gangliacollectorsuggestout,
            "nagiosserver": $nagiosserversuggestout
           }';

print json_encode($outjson);
?>
