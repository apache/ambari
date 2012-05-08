<?php

$data = array
  (
     'Global' => array
     (
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=load_report',
           'title' => 'Load Report',
           'description' => 'Key load metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=mem_report',
           'title' => 'Memory Report',
           'description' => 'Key memory metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=cpu_report',
           'title' => 'CPU Report',
           'description' => 'Key CPU metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=network_report',
           'title' => 'Network I/O Report',
           'description' => 'Key network I/O metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        )
     )
  );

echo json_encode($data);

?>
