#!/bin/sh

GRAPH_INFO_JSON_PATH="../../../src/dataServices/ganglia/graph_info";

JSON_PRETTY_PRINT="python -mjson.tool"

### WARNING: These PHP definitions have diverged from the actual JSON definitions
###          (which I started to modify directly), so running the scripts below
###          will result in data loss!

### php ./generate_dashboard_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/dashboard/all.json;
### php ./generate_dashboard_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/dashboard/custom/hdp.json;
### php ./generate_mapreduce_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/mapreduce/all.json;
### php ./generate_mapreduce_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/mapreduce/custom/hdp.json;
### php ./generate_hdfs_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/hdfs/all.json;
### php ./generate_hdfs_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/hdfs/custom/hdp.json;
