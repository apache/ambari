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

/** Constants. */
define("HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES", "Properties");
define("HDP_MON_RESPONSE_OPTION_KEY__TYPE", "Type");

define("HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE", "Uncacheable");
define("HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON", "JSON");
define("HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT", "JAVASCRIPT");

define("HDP_MON_QUERY_ARG__JSONP", "jsonp");

/** Spits out appropriate response headers, as per the options passed in. */
function hdp_mon_generate_response_headers( $response_options )
{
  if( $response_options[HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES] == HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE )
  {
    // Make the response uncache-able.
    header("Expires: Mon, 26 Jul 1997 05:00:00 GMT"); // Date in the past
    header("Last-Modified: " . gmdate("D, d M Y H:i:s") . " GMT"); // Always modified
    header("Cache-Control: no-cache, must-revalidate"); // HTTP/1.1
    header("Pragma: no-cache"); // HTTP/1.0
  }

  switch( $response_options[HDP_MON_RESPONSE_OPTION_KEY__TYPE] )
  {
    case HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON:
      {
        header('Content-type: application/json');
      }
      break;

    case HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT:
      {
        header('Content-type: application/javascript');
      }
      break;
  }
}

/** Given $response_data (which we expect to be a JSON string), generate an
 *  HTTP response, which includes emitting the necessary HTTP response headers
 *  followed by the response body (that is either plain ol' $response_data,
 *  or a JSONP wrapper around it).
 */
function hdp_mon_generate_response( $response_data )
{
  $jsonpFunctionName = NULL;
  if (isset($_GET[HDP_MON_QUERY_ARG__JSONP])) {
    $jsonpFunctionName = $_GET[HDP_MON_QUERY_ARG__JSONP];
  }

  hdp_mon_generate_response_headers( array
    ( HDP_MON_RESPONSE_OPTION_KEY__PROPERTIES => HDP_MON_RESPONSE_OPTION_VALUE__PROPERTIES_UNCACHEABLE,
      HDP_MON_RESPONSE_OPTION_KEY__TYPE =>
        isset( $jsonpFunctionName )  && $jsonpFunctionName != "" ?
          HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JAVASCRIPT :
          HDP_MON_RESPONSE_OPTION_VALUE__TYPE_JSON ) );

  if( isset( $jsonpFunctionName ) )
  {
    echo "$jsonpFunctionName( $response_data );";
  }
  else
  {
    echo $response_data;
  }
}

?>
