<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

include_once("../common/global_configs.inc");
include_once("../common/common.inc");
include_once("../common/cluster_configuration.inc");

hdp_mon_load_cluster_configuration();

$GANGLIA_WEB_ROOT = $GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_ROOT"];

include_once("$GANGLIA_WEB_ROOT/conf_default.php");
include_once("$GANGLIA_WEB_ROOT/get_context.php");
include_once("$GANGLIA_WEB_ROOT/ganglia.php");
include_once("$GANGLIA_WEB_ROOT/get_ganglia.php");

include_once("../common/response.inc");

/** main() */
$metric_names = array();

switch ($context)
{
  case "host":
  case "node":
    {
      /* The names of the metrics from $hostname are stored as the
       * keys of $metrics in host/node context.
       */
      $metric_names = array_keys( $metrics );
    }
    break;

  case "physical":
  case "cluster-summary":
  case "cluster":
    {
      /* The names of the metrics for $clustername are those
       * of the first $hostname in $metrics.
       *
       * If this feels hacky, fret not - this is exactly how
       * GangliaWeb chooses to perform this operation as well.
       */
      $first_host = key( $metrics );
      $metric_names = array_keys( $metrics[$first_host] );
    }
    break;
}

if( !empty($metric_names) )
{
  hdp_mon_generate_response( json_encode( $metric_names ) );
}
else
{
  header("HTTP/1.0 400 Bad Request");
}

?>
