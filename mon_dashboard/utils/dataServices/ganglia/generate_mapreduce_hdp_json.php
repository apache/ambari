<?php

$data = array
  (
     'Global' => array
     (
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
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=hdp_mon_jvm_gc_report',
           'title' => 'JobTracker: JVM Garbage Collection',
           'description' => 'Key Garbage Collection stats for the JobTracker\'s JVM',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=hdp_mon_rpc_latency_report',
           'title' => 'JobTracker: RPC Average Latencies',
           'description' => 'Average latencies for processing and queue times on the JobTracker, to give a feel for potential performance bottlenecks',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%'
        )
     )
  );

echo json_encode($data);

?>

