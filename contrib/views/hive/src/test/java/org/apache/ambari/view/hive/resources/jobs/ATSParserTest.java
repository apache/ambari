/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.jobs;

import org.apache.ambari.view.hive.resources.jobs.atsJobs.*;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.List;

public class ATSParserTest {
  @Test
  public void testBase64() throws Exception {
    System.out.println(Arrays.toString(Base64.decodeBase64("HWvpjKiERZCy_le4s-odOQ")));
  }

  @Test
  public void testGetHiveJobsList() throws Exception {
    IATSParser jobLoader = new ATSParser(new ATSRequestsDelegateStub());

    List<HiveQueryId> jobs = jobLoader.getHiveQueryIdsList("hive");

    Assert.assertEquals(1, jobs.size());

    HiveQueryId job = jobs.get(0);
    Assert.assertEquals("hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0", job.entity);
    Assert.assertEquals(1423493324L, job.starttime);
    Assert.assertEquals("hive", job.user);
    Assert.assertEquals(1423493342L - 1423493324L, job.duration);
    Assert.assertEquals("select count(*) from z", job.query);

    Assert.assertEquals(1, job.dagNames.size());
    Assert.assertEquals("hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4", job.dagNames.get(0));

    Assert.assertEquals(2, job.stages.size());
    Assert.assertTrue(HiveQueryId.ATS_15_RESPONSE_VERSION > job.version);

    jobLoader = new ATSParser(new ATSV15RequestsDelegateStub());
    List<HiveQueryId> jobsv2 = jobLoader.getHiveQueryIdsList("hive");
    Assert.assertEquals(1, jobsv2.size());
    HiveQueryId jobv2 = jobsv2.get(0);
    Assert.assertTrue(HiveQueryId.ATS_15_RESPONSE_VERSION <= jobv2.version);
  }

  @Test
  public void testGetTezDAGByName() throws Exception {
    IATSParser jobLoader = new ATSParser(new ATSRequestsDelegateStub());

    TezDagId tezDag = jobLoader.getTezDAGByName("hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4");

    Assert.assertEquals("dag_1423156117563_0005_2", tezDag.entity);
    Assert.assertEquals("application_1423156117563_0005", tezDag.applicationId);
    Assert.assertEquals("SUCCEEDED", tezDag.status);
  }

  @Test
  public void testGetTezDagByEntity() throws Exception {
    IATSParser jobLoader = new ATSParser(new ATSV15RequestsDelegateStub());

    TezDagId tezDag = jobLoader.getTezDAGByEntity("hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4");

    Assert.assertEquals("dag_1423156117563_0005_2", tezDag.entity);
    Assert.assertEquals("application_1423156117563_0005", tezDag.applicationId);
    Assert.assertEquals("SUCCEEDED", tezDag.status);
  }

  protected static class ATSV15RequestsDelegateStub extends ATSRequestsDelegateStub {
    /**
     * This returns the version field that the ATS v1.5 returns.
     */
    @Override
    public JSONObject hiveQueryIdList(String username) {
      return (JSONObject) JSONValue.parse(
        "{ \"entities\" : [ { \"domain\" : \"DEFAULT\",\n" +
          "        \"entity\" : \"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0\",\n" +
          "        \"entitytype\" : \"HIVE_QUERY_ID\",\n" +
          "        \"events\" : [ { \"eventinfo\" : {  },\n" +
          "              \"eventtype\" : \"QUERY_COMPLETED\",\n" +
          "              \"timestamp\" : 1423493342843\n" +
          "            },\n" +
          "            { \"eventinfo\" : {  },\n" +
          "              \"eventtype\" : \"QUERY_SUBMITTED\",\n" +
          "              \"timestamp\" : 1423493324355\n" +
          "            }\n" +
          "          ],\n" +
          "        \"otherinfo\" : { \"MAPRED\" : false,\n" +
          "            \"QUERY\" : \"{\\\"queryText\\\":\\\"select count(*) from z\\\",\\\"queryPlan\\\":{\\\"STAGE PLANS\\\":{\\\"Stage-1\\\":{\\\"Tez\\\":{\\\"DagName:\\\":\\\"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4\\\",\\\"Vertices:\\\":{\\\"Reducer 2\\\":{\\\"Reduce Operator Tree:\\\":{\\\"Group By Operator\\\":{\\\"mode:\\\":\\\"mergepartial\\\",\\\"aggregations:\\\":[\\\"count(VALUE._col0)\\\"],\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"Select Operator\\\":{\\\"expressions:\\\":\\\"_col0 (type: bigint)\\\",\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"File Output Operator\\\":{\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\",\\\"compressed:\\\":\\\"false\\\",\\\"table:\\\":{\\\"serde:\\\":\\\"org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe\\\",\\\"input format:\\\":\\\"org.apache.hadoop.mapred.TextInputFormat\\\",\\\"output format:\\\":\\\"org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat\\\"}}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}}},\\\"Map 1\\\":{\\\"Map Operator Tree:\\\":[{\\\"TableScan\\\":{\\\"alias:\\\":\\\"z\\\",\\\"children\\\":{\\\"Select Operator\\\":{\\\"children\\\":{\\\"Group By Operator\\\":{\\\"mode:\\\":\\\"hash\\\",\\\"aggregations:\\\":[\\\"count()\\\"],\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"Reduce Output Operator\\\":{\\\"sort order:\\\":\\\"\\\",\\\"value expressions:\\\":\\\"_col0 (type: bigint)\\\",\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 0 Data size: 40 Basic stats: PARTIAL Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 0 Data size: 40 Basic stats: PARTIAL Column stats: COMPLETE\\\"}}]}},\\\"Edges:\\\":{\\\"Reducer 2\\\":{\\\"parent\\\":\\\"Map 1\\\",\\\"type\\\":\\\"SIMPLE_EDGE\\\"}}}},\\\"Stage-0\\\":{\\\"Fetch Operator\\\":{\\\"limit:\\\":\\\"-1\\\",\\\"Processor Tree:\\\":{\\\"ListSink\\\":{}}}}},\\\"STAGE DEPENDENCIES\\\":{\\\"Stage-1\\\":{\\\"ROOT STAGE\\\":\\\"TRUE\\\"},\\\"Stage-0\\\":{\\\"DEPENDENT STAGES\\\":\\\"Stage-1\\\"}}}}\",\n" +
          "            \"STATUS\" : true,\n" +
          "            \"TEZ\" : true\n" +
          "            \"VERSION\" : 2\n" +
          "          },\n" +
          "        \"primaryfilters\" : { \"user\" : [ \"hive\" ] },\n" +
          "        \"relatedentities\" : {  },\n" +
          "        \"starttime\" : 1423493324355\n" +
          "      } ] }"
      );
    }
  }

  protected static class ATSRequestsDelegateStub implements ATSRequestsDelegate {

    @Override
    public String hiveQueryIdDirectUrl(String entity) {
      return null;
    }

    @Override
    public String hiveQueryIdOperationIdUrl(String operationId) {
      return null;
    }

    @Override
    public String tezDagDirectUrl(String entity) {
      return null;
    }

    @Override
    public String tezDagNameUrl(String name) {
      return null;
    }

    @Override
    public String tezVerticesListForDAGUrl(String dagId) {
      return null;
    }

    @Override
    public JSONObject hiveQueryIdList(String username) {
      return (JSONObject) JSONValue.parse(
          "{ \"entities\" : [ { \"domain\" : \"DEFAULT\",\n" +
              "        \"entity\" : \"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0\",\n" +
              "        \"entitytype\" : \"HIVE_QUERY_ID\",\n" +
              "        \"events\" : [ { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"QUERY_COMPLETED\",\n" +
              "              \"timestamp\" : 1423493342843\n" +
              "            },\n" +
              "            { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"QUERY_SUBMITTED\",\n" +
              "              \"timestamp\" : 1423493324355\n" +
              "            }\n" +
              "          ],\n" +
              "        \"otherinfo\" : { \"MAPRED\" : false,\n" +
              "            \"QUERY\" : \"{\\\"queryText\\\":\\\"select count(*) from z\\\",\\\"queryPlan\\\":{\\\"STAGE PLANS\\\":{\\\"Stage-1\\\":{\\\"Tez\\\":{\\\"DagName:\\\":\\\"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4\\\",\\\"Vertices:\\\":{\\\"Reducer 2\\\":{\\\"Reduce Operator Tree:\\\":{\\\"Group By Operator\\\":{\\\"mode:\\\":\\\"mergepartial\\\",\\\"aggregations:\\\":[\\\"count(VALUE._col0)\\\"],\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"Select Operator\\\":{\\\"expressions:\\\":\\\"_col0 (type: bigint)\\\",\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"File Output Operator\\\":{\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\",\\\"compressed:\\\":\\\"false\\\",\\\"table:\\\":{\\\"serde:\\\":\\\"org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe\\\",\\\"input format:\\\":\\\"org.apache.hadoop.mapred.TextInputFormat\\\",\\\"output format:\\\":\\\"org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat\\\"}}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}}},\\\"Map 1\\\":{\\\"Map Operator Tree:\\\":[{\\\"TableScan\\\":{\\\"alias:\\\":\\\"z\\\",\\\"children\\\":{\\\"Select Operator\\\":{\\\"children\\\":{\\\"Group By Operator\\\":{\\\"mode:\\\":\\\"hash\\\",\\\"aggregations:\\\":[\\\"count()\\\"],\\\"outputColumnNames:\\\":[\\\"_col0\\\"],\\\"children\\\":{\\\"Reduce Output Operator\\\":{\\\"sort order:\\\":\\\"\\\",\\\"value expressions:\\\":\\\"_col0 (type: bigint)\\\",\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 0 Data size: 40 Basic stats: PARTIAL Column stats: COMPLETE\\\"}},\\\"Statistics:\\\":\\\"Num rows: 0 Data size: 40 Basic stats: PARTIAL Column stats: COMPLETE\\\"}}]}},\\\"Edges:\\\":{\\\"Reducer 2\\\":{\\\"parent\\\":\\\"Map 1\\\",\\\"type\\\":\\\"SIMPLE_EDGE\\\"}}}},\\\"Stage-0\\\":{\\\"Fetch Operator\\\":{\\\"limit:\\\":\\\"-1\\\",\\\"Processor Tree:\\\":{\\\"ListSink\\\":{}}}}},\\\"STAGE DEPENDENCIES\\\":{\\\"Stage-1\\\":{\\\"ROOT STAGE\\\":\\\"TRUE\\\"},\\\"Stage-0\\\":{\\\"DEPENDENT STAGES\\\":\\\"Stage-1\\\"}}}}\",\n" +
              "            \"STATUS\" : true,\n" +
              "            \"TEZ\" : true\n" +
              "          },\n" +
              "        \"primaryfilters\" : { \"user\" : [ \"hive\" ] },\n" +
              "        \"relatedentities\" : {  },\n" +
              "        \"starttime\" : 1423493324355\n" +
              "      } ] }"
      );
    }

    @Override
    public JSONObject hiveQueryIdByOperationId(String operationId) {
      throw new NotImplementedException();
    }

    @Override
    public JSONObject tezDagByName(String name) {
      return (JSONObject) JSONValue.parse(
          "{ \"entities\" : [ { \"domain\" : \"DEFAULT\",\n" +
              "        \"entity\" : \"dag_1423156117563_0005_2\",\n" +
              "        \"entitytype\" : \"TEZ_DAG_ID\",\n" +
              "        \"events\" : [ { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"DAG_FINISHED\",\n" +
              "              \"timestamp\" : 1423493342484\n" +
              "            },\n" +
              "            { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"DAG_STARTED\",\n" +
              "              \"timestamp\" : 1423493325803\n" +
              "            },\n" +
              "            { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"DAG_INITIALIZED\",\n" +
              "              \"timestamp\" : 1423493325794\n" +
              "            },\n" +
              "            { \"eventinfo\" : {  },\n" +
              "              \"eventtype\" : \"DAG_SUBMITTED\",\n" +
              "              \"timestamp\" : 1423493325578\n" +
              "            }\n" +
              "          ],\n" +
              "        \"otherinfo\" : { \"applicationId\" : \"application_1423156117563_0005\",\n" +
              "            \"counters\" : { \"counterGroups\" : [ { \"counterGroupDisplayName\" : \"org.apache.tez.common.counters.DAGCounter\",\n" +
              "                      \"counterGroupName\" : \"org.apache.tez.common.counters.DAGCounter\",\n" +
              "                      \"counters\" : [ { \"counterDisplayName\" : \"NUM_SUCCEEDED_TASKS\",\n" +
              "                            \"counterName\" : \"NUM_SUCCEEDED_TASKS\",\n" +
              "                            \"counterValue\" : 2\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"TOTAL_LAUNCHED_TASKS\",\n" +
              "                            \"counterName\" : \"TOTAL_LAUNCHED_TASKS\",\n" +
              "                            \"counterValue\" : 2\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"DATA_LOCAL_TASKS\",\n" +
              "                            \"counterName\" : \"DATA_LOCAL_TASKS\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          }\n" +
              "                        ]\n" +
              "                    },\n" +
              "                    { \"counterGroupDisplayName\" : \"File System Counters\",\n" +
              "                      \"counterGroupName\" : \"org.apache.tez.common.counters.FileSystemCounter\",\n" +
              "                      \"counters\" : [ { \"counterDisplayName\" : \"FILE_BYTES_READ\",\n" +
              "                            \"counterName\" : \"FILE_BYTES_READ\",\n" +
              "                            \"counterValue\" : 57\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"FILE_BYTES_WRITTEN\",\n" +
              "                            \"counterName\" : \"FILE_BYTES_WRITTEN\",\n" +
              "                            \"counterValue\" : 82\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"FILE_READ_OPS\",\n" +
              "                            \"counterName\" : \"FILE_READ_OPS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"FILE_LARGE_READ_OPS\",\n" +
              "                            \"counterName\" : \"FILE_LARGE_READ_OPS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"FILE_WRITE_OPS\",\n" +
              "                            \"counterName\" : \"FILE_WRITE_OPS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"HDFS_BYTES_READ\",\n" +
              "                            \"counterName\" : \"HDFS_BYTES_READ\",\n" +
              "                            \"counterValue\" : 287\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"HDFS_BYTES_WRITTEN\",\n" +
              "                            \"counterName\" : \"HDFS_BYTES_WRITTEN\",\n" +
              "                            \"counterValue\" : 2\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"HDFS_READ_OPS\",\n" +
              "                            \"counterName\" : \"HDFS_READ_OPS\",\n" +
              "                            \"counterValue\" : 16\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"HDFS_LARGE_READ_OPS\",\n" +
              "                            \"counterName\" : \"HDFS_LARGE_READ_OPS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"HDFS_WRITE_OPS\",\n" +
              "                            \"counterName\" : \"HDFS_WRITE_OPS\",\n" +
              "                            \"counterValue\" : 2\n" +
              "                          }\n" +
              "                        ]\n" +
              "                    },\n" +
              "                    { \"counterGroupDisplayName\" : \"org.apache.tez.common.counters.TaskCounter\",\n" +
              "                      \"counterGroupName\" : \"org.apache.tez.common.counters.TaskCounter\",\n" +
              "                      \"counters\" : [ { \"counterDisplayName\" : \"REDUCE_INPUT_GROUPS\",\n" +
              "                            \"counterName\" : \"REDUCE_INPUT_GROUPS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"REDUCE_INPUT_RECORDS\",\n" +
              "                            \"counterName\" : \"REDUCE_INPUT_RECORDS\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"COMBINE_INPUT_RECORDS\",\n" +
              "                            \"counterName\" : \"COMBINE_INPUT_RECORDS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SPILLED_RECORDS\",\n" +
              "                            \"counterName\" : \"SPILLED_RECORDS\",\n" +
              "                            \"counterValue\" : 2\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"NUM_SHUFFLED_INPUTS\",\n" +
              "                            \"counterName\" : \"NUM_SHUFFLED_INPUTS\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"NUM_SKIPPED_INPUTS\",\n" +
              "                            \"counterName\" : \"NUM_SKIPPED_INPUTS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"NUM_FAILED_SHUFFLE_INPUTS\",\n" +
              "                            \"counterName\" : \"NUM_FAILED_SHUFFLE_INPUTS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"MERGED_MAP_OUTPUTS\",\n" +
              "                            \"counterName\" : \"MERGED_MAP_OUTPUTS\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"GC_TIME_MILLIS\",\n" +
              "                            \"counterName\" : \"GC_TIME_MILLIS\",\n" +
              "                            \"counterValue\" : 389\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"CPU_MILLISECONDS\",\n" +
              "                            \"counterName\" : \"CPU_MILLISECONDS\",\n" +
              "                            \"counterValue\" : 2820\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"PHYSICAL_MEMORY_BYTES\",\n" +
              "                            \"counterName\" : \"PHYSICAL_MEMORY_BYTES\",\n" +
              "                            \"counterValue\" : 490799104\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"VIRTUAL_MEMORY_BYTES\",\n" +
              "                            \"counterName\" : \"VIRTUAL_MEMORY_BYTES\",\n" +
              "                            \"counterValue\" : 1558253568\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"COMMITTED_HEAP_BYTES\",\n" +
              "                            \"counterName\" : \"COMMITTED_HEAP_BYTES\",\n" +
              "                            \"counterValue\" : 312475648\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"INPUT_RECORDS_PROCESSED\",\n" +
              "                            \"counterName\" : \"INPUT_RECORDS_PROCESSED\",\n" +
              "                            \"counterValue\" : 3\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"OUTPUT_RECORDS\",\n" +
              "                            \"counterName\" : \"OUTPUT_RECORDS\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"OUTPUT_BYTES\",\n" +
              "                            \"counterName\" : \"OUTPUT_BYTES\",\n" +
              "                            \"counterValue\" : 3\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"OUTPUT_BYTES_WITH_OVERHEAD\",\n" +
              "                            \"counterName\" : \"OUTPUT_BYTES_WITH_OVERHEAD\",\n" +
              "                            \"counterValue\" : 11\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"OUTPUT_BYTES_PHYSICAL\",\n" +
              "                            \"counterName\" : \"OUTPUT_BYTES_PHYSICAL\",\n" +
              "                            \"counterValue\" : 25\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"ADDITIONAL_SPILLS_BYTES_WRITTEN\",\n" +
              "                            \"counterName\" : \"ADDITIONAL_SPILLS_BYTES_WRITTEN\",\n" +
              "                            \"counterValue\" : 25\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"ADDITIONAL_SPILLS_BYTES_READ\",\n" +
              "                            \"counterName\" : \"ADDITIONAL_SPILLS_BYTES_READ\",\n" +
              "                            \"counterValue\" : 25\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"ADDITIONAL_SPILL_COUNT\",\n" +
              "                            \"counterName\" : \"ADDITIONAL_SPILL_COUNT\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SHUFFLE_BYTES\",\n" +
              "                            \"counterName\" : \"SHUFFLE_BYTES\",\n" +
              "                            \"counterValue\" : 25\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SHUFFLE_BYTES_DECOMPRESSED\",\n" +
              "                            \"counterName\" : \"SHUFFLE_BYTES_DECOMPRESSED\",\n" +
              "                            \"counterValue\" : 11\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SHUFFLE_BYTES_TO_MEM\",\n" +
              "                            \"counterName\" : \"SHUFFLE_BYTES_TO_MEM\",\n" +
              "                            \"counterValue\" : 25\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SHUFFLE_BYTES_TO_DISK\",\n" +
              "                            \"counterName\" : \"SHUFFLE_BYTES_TO_DISK\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"SHUFFLE_BYTES_DISK_DIRECT\",\n" +
              "                            \"counterName\" : \"SHUFFLE_BYTES_DISK_DIRECT\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"NUM_MEM_TO_DISK_MERGES\",\n" +
              "                            \"counterName\" : \"NUM_MEM_TO_DISK_MERGES\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"NUM_DISK_TO_DISK_MERGES\",\n" +
              "                            \"counterName\" : \"NUM_DISK_TO_DISK_MERGES\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          }\n" +
              "                        ]\n" +
              "                    },\n" +
              "                    { \"counterGroupDisplayName\" : \"HIVE\",\n" +
              "                      \"counterGroupName\" : \"HIVE\",\n" +
              "                      \"counters\" : [ { \"counterDisplayName\" : \"CREATED_FILES\",\n" +
              "                            \"counterName\" : \"CREATED_FILES\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"DESERIALIZE_ERRORS\",\n" +
              "                            \"counterName\" : \"DESERIALIZE_ERRORS\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"RECORDS_IN_Map_1\",\n" +
              "                            \"counterName\" : \"RECORDS_IN_Map_1\",\n" +
              "                            \"counterValue\" : 3\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"RECORDS_OUT_INTERMEDIATE_Map_1\",\n" +
              "                            \"counterName\" : \"RECORDS_OUT_INTERMEDIATE_Map_1\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"RECORDS_OUT_Reducer_2\",\n" +
              "                            \"counterName\" : \"RECORDS_OUT_Reducer_2\",\n" +
              "                            \"counterValue\" : 1\n" +
              "                          }\n" +
              "                        ]\n" +
              "                    },\n" +
              "                    { \"counterGroupDisplayName\" : \"Shuffle Errors\",\n" +
              "                      \"counterGroupName\" : \"Shuffle Errors\",\n" +
              "                      \"counters\" : [ { \"counterDisplayName\" : \"BAD_ID\",\n" +
              "                            \"counterName\" : \"BAD_ID\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"CONNECTION\",\n" +
              "                            \"counterName\" : \"CONNECTION\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"IO_ERROR\",\n" +
              "                            \"counterName\" : \"IO_ERROR\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"WRONG_LENGTH\",\n" +
              "                            \"counterName\" : \"WRONG_LENGTH\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"WRONG_MAP\",\n" +
              "                            \"counterName\" : \"WRONG_MAP\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          },\n" +
              "                          { \"counterDisplayName\" : \"WRONG_REDUCE\",\n" +
              "                            \"counterName\" : \"WRONG_REDUCE\",\n" +
              "                            \"counterValue\" : 0\n" +
              "                          }\n" +
              "                        ]\n" +
              "                    }\n" +
              "                  ] },\n" +
              "            \"dagPlan\" : { \"dagName\" : \"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4\",\n" +
              "                \"edges\" : [ { \"dataMovementType\" : \"SCATTER_GATHER\",\n" +
              "                      \"dataSourceType\" : \"PERSISTED\",\n" +
              "                      \"edgeDestinationClass\" : \"org.apache.tez.runtime.library.input.OrderedGroupedKVInput\",\n" +
              "                      \"edgeId\" : \"533454263\",\n" +
              "                      \"edgeSourceClass\" : \"org.apache.tez.runtime.library.output.OrderedPartitionedKVOutput\",\n" +
              "                      \"inputVertexName\" : \"Map 1\",\n" +
              "                      \"outputVertexName\" : \"Reducer 2\",\n" +
              "                      \"schedulingType\" : \"SEQUENTIAL\"\n" +
              "                    } ],\n" +
              "                \"version\" : 1,\n" +
              "                \"vertices\" : [ { \"additionalInputs\" : [ { \"class\" : \"org.apache.tez.mapreduce.input.MRInputLegacy\",\n" +
              "                            \"initializer\" : \"org.apache.hadoop.hive.ql.exec.tez.HiveSplitGenerator\",\n" +
              "                            \"name\" : \"z\"\n" +
              "                          } ],\n" +
              "                      \"outEdgeIds\" : [ \"533454263\" ],\n" +
              "                      \"processorClass\" : \"org.apache.hadoop.hive.ql.exec.tez.MapTezProcessor\",\n" +
              "                      \"vertexName\" : \"Map 1\"\n" +
              "                    },\n" +
              "                    { \"additionalOutputs\" : [ { \"class\" : \"org.apache.tez.mapreduce.output.MROutput\",\n" +
              "                            \"name\" : \"out_Reducer 2\"\n" +
              "                          } ],\n" +
              "                      \"inEdgeIds\" : [ \"533454263\" ],\n" +
              "                      \"processorClass\" : \"org.apache.hadoop.hive.ql.exec.tez.ReduceTezProcessor\",\n" +
              "                      \"vertexName\" : \"Reducer 2\"\n" +
              "                    }\n" +
              "                  ]\n" +
              "              },\n" +
              "            \"diagnostics\" : \"\",\n" +
              "            \"endTime\" : 1423493342484,\n" +
              "            \"initTime\" : 1423493325794,\n" +
              "            \"numCompletedTasks\" : 2,\n" +
              "            \"numFailedTaskAttempts\" : 0,\n" +
              "            \"numFailedTasks\" : 0,\n" +
              "            \"numKilledTaskAttempts\" : 0,\n" +
              "            \"numKilledTasks\" : 0,\n" +
              "            \"numSucceededTasks\" : 2,\n" +
              "            \"startTime\" : 1423493325803,\n" +
              "            \"status\" : \"SUCCEEDED\",\n" +
              "            \"timeTaken\" : 16681,\n" +
              "            \"vertexNameIdMapping\" : { \"Map 1\" : \"vertex_1423156117563_0005_2_00\",\n" +
              "                \"Reducer 2\" : \"vertex_1423156117563_0005_2_01\"\n" +
              "              }\n" +
              "          },\n" +
              "        \"primaryfilters\" : { \"applicationId\" : [ \"application_1423156117563_0005\" ],\n" +
              "            \"dagName\" : [ \"hive_20150209144848_c3a5a07b-c3b6-4f57-a6d5-3dadecdd6fd0:4\" ],\n" +
              "            \"status\" : [ \"SUCCEEDED\" ],\n" +
              "            \"user\" : [ \"hive\" ]\n" +
              "          },\n" +
              "        \"relatedentities\" : {  },\n" +
              "        \"starttime\" : 1423493325578\n" +
              "      } ] }"
      );
    }

    @Override
    public JSONObject tezVerticesListForDAG(String dagId) {
      return null;
    }

    @Override
    public JSONObject tezDagByEntity(String entity) {
      return tezDagByName(entity);
    }
  }
}