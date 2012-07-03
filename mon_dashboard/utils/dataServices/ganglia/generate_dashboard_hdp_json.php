<?php

$data = array
  (
     'Global' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%HDFSSlavesClusterName%&g=hdp_mon_hdfs_io_report',
           'title' => 'HDFS I/O',
           'description' => 'Bytes written to and read from HDFS, aggregated across all the DataNodes',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=hdp_mon_jobtracker_map_slot_report',
           'title' => 'Map Slot Utilization',
           'description' => 'Utilized Map slots (occupied + reserved) vs. Total Map slots',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=hdp_mon_jobtracker_reduce_slot_report',
           'title' => 'Reduce Slot Utilization',
           'description' => 'Utilized Reduce slots (occupied + reserved) vs. Total Reduce slots',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=hdp_mon_jobtracker_mapreduce_report',
           'title' => 'MapReduce Backlog',
           'description' => 'Waiting Maps and Reduces, to give a feel for a combined MapReduce backlog',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%'
        )
     )
  );

echo json_encode($data);

?>

