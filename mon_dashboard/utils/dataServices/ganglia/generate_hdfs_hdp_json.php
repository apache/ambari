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
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=hdp_mon_hdfs_ops_report',
           'title' => 'NameNode Operation Counts',
           'description' => 'Counts of key operations on the NameNode, to give a feel for the high-level HDFS activity pattern',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=hdp_mon_jvm_gc_report',
           'title' => 'NameNode: JVM Garbage Collection',
           'description' => 'Key Garbage Collection stats for the NameNode\'s JVM',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=hdp_mon_rpc_latency_report',
           'title' => 'NameNode: RPC Average Latencies',
           'description' => 'Average latencies for processing and queue times on the NameNode, to give a feel for potential performance bottlenecks',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%'
        )
     )
  );

echo json_encode($data);

?>

