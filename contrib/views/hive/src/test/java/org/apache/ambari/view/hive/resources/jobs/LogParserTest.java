/**
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

package org.apache.ambari.view.hive.resources.jobs;

import org.junit.Assert;
import org.junit.Test;

public class LogParserTest {
    @Test
    public void testParseMRLog() {
        String log = "INFO : Number of reduce tasks determined at compile time: 1\n" +
            "INFO : In order to change the average load for a reducer (in bytes):\n" +
            "INFO : set hive.exec.reducers.bytes.per.reducer=<number>\n" +
            "INFO : In order to limit the maximum number of reducers:\n" +
            "INFO : set hive.exec.reducers.max=<number>\n" +
            "INFO : In order to set a constant number of reducers:\n" +
            "INFO : set mapreduce.job.reduces=<number>\n" +
            "WARN : Hadoop command-line option parsing not performed. Implement the Tool interface and execute your application with ToolRunner to remedy this.\n" +
            "INFO : number of splits:1\n" +
            "INFO : Submitting tokens for job: job_1421248330903_0003\n" +
            "INFO : The url to track the job: http://dataworker.hortonworks.com:8088/proxy/application_1421248330903_0003/\n" +
            "INFO : Starting Job = job_1421248330903_0003, Tracking URL = http://dataworker.hortonworks.com:8088/proxy/application_1421248330903_0003/\n" +
            "INFO : Kill Command = /usr/hdp/current/hadoop-client/bin/hadoop job -kill job_1421248330903_0003\n" +
            "INFO : Hadoop job information for Stage-1: number of mappers: 1; number of reducers: 1\n" +
            "INFO : 2015-01-21 15:03:55,979 Stage-1 map = 0%, reduce = 0%\n" +
            "INFO : 2015-01-21 15:04:07,503 Stage-1 map = 100%, reduce = 0%, Cumulative CPU 0.79 sec\n" +
            "INFO : 2015-01-21 15:04:17,384 Stage-1 map = 100%, reduce = 100%, Cumulative CPU 1.86 sec\n" +
            "INFO : MapReduce Total cumulative CPU time: 1 seconds 860 msec\n" +
            "INFO : Ended Job = job_1421248330903_0003";

        LogParser p = LogParser.parseLog(log);
        Assert.assertEquals(1, p.getAppsList().size());
        Assert.assertEquals("application_1421248330903_0003",(((LogParser.AppId) (p.getAppsList().toArray())[0])
                                                            .getIdentifier()));
    }

    @Test
    public void testParseTezLog() {
        String log = "INFO : Tez session hasn't been created yet. Opening session\n" +
            "INFO :\n" +
            "\n" +
            "INFO : Status: Running (Executing on YARN cluster with App id application_1423156117563_0003)\n" +
            "\n" +
            "INFO : Map 1: -/- Reducer 2: 0/1\n" +
            "INFO : Map 1: 0/1 Reducer 2: 0/1\n" +
            "INFO : Map 1: 0/1 Reducer 2: 0/1\n" +
            "INFO : Map 1: 0(+1)/1 Reducer 2: 0/1\n" +
            "INFO : Map 1: 0(+1)/1 Reducer 2: 0/1\n" +
            "INFO : Map 1: 1/1 Reducer 2: 0(+1)/1\n" +
            "INFO : Map 1: 1/1 Reducer 2: 1/1 ";

        LogParser p = LogParser.parseLog(log);
        Assert.assertEquals(1, p.getAppsList().size());
        Assert.assertEquals("application_1423156117563_0003",(((LogParser.AppId) (p.getAppsList().toArray())[0])
            .getIdentifier()));
    }
}
