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

package org.apache.ambari.view.hawq;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Random;

/**
 * Servlet for HAWQ Queries list view.
 */
public class HAWQViewServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // TODO use constant
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);

    Random randNo = new Random();

    TimeZone timeZone = TimeZone.getTimeZone("PST");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    df.setTimeZone(timeZone);

    StringBuilder responseString = new StringBuilder("{ \"data\": [");

    String[] databaseNames = {"gpadmin", "postgres", "template1", "template2", "employee", "users", "payroll", "taxes", "benefits", "projects"};
    String[] userNames = {"newton aLex", "alex DeniSSov", "v", "jun", "bhuvNeshChaudhary", "laVjAiN", "m@tt", "5Z", "gt"};
    String[] clientHosts = {"c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org", "c6405.ambari.apache.org"};
    String[] applicationNames = {"hawq", "psql", "tableau", "excel", "hbase", "hive"};
    String[] duration = {"00:00:00", "02:01:00", "03:04:00"};

    int distribution = randNo.nextInt(3);
    int maxNoOfQUeries = 100;
    int noOfQueries = 0;

    switch(distribution) {
        case 0:
            noOfQueries = 0;
            break;
        case 2:
            noOfQueries = maxNoOfQUeries;
            break;
        case 1:
        default:
            noOfQueries = randNo.nextInt(maxNoOfQUeries);
    }

    int index = 1;
    long currTime = System.currentTimeMillis() / 1000;

    while(index <= noOfQueries) {
        long queryStartTime = currTime - (randNo.nextInt(55000) + 1) * 1000;
        long transactionStartTime = queryStartTime - (randNo.nextInt(40000) + 1) * 1000;
        long backendStartTime = transactionStartTime - (randNo.nextInt(100000) + 1) * 1000;

        String query = "{"+
            "    \"id\": " + (index++) + ","+
            "    \"type\": \"query\","+
            "    \"attributes\": {"+
            "      \"database-name\": \"" + databaseNames[randNo.nextInt(databaseNames.length)] + "\","+
            "      \"pid\": " + (randNo.nextInt(99999) + 1) + ","+
            "      \"user-name\": \"" + userNames[randNo.nextInt(userNames.length)] + "\","+
            "      \"waiting\": " + randNo.nextBoolean() + ","+
            "      \"waiting-resource\": " + randNo.nextBoolean() + ","+
            "      \"duration\": \"" + duration[randNo.nextInt(duration.length)] + "\","+
            "      \"query-start-time\": \"" + df.format(queryStartTime) + "\","+
            "      \"transaction-start-time\": \"" + df.format(transactionStartTime) + "\","+
            "      \"client-host\": \"" + clientHosts[randNo.nextInt(clientHosts.length)] + "\","+
            "      \"client-port\": \"" + (randNo.nextInt(99999) + 1) + "\","+
            "      \"application-name\": \"" + applicationNames[randNo.nextInt(applicationNames.length)] + "\""+
            "    }"+
            "  }";

        responseString.append(query).append(index <= noOfQueries ? "," : "");
    }

    responseString.append("]}");

    PrintWriter writer = response.getWriter();
    writer.print(responseString.toString());
  }
}
