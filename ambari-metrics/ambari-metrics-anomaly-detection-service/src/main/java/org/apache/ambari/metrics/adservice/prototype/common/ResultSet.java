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
package org.apache.ambari.metrics.adservice.prototype.common;


import java.util.ArrayList;
import java.util.List;

public class ResultSet {

    public List<double[]> resultset = new ArrayList<>();

    public ResultSet(List<double[]> resultset) {
        this.resultset = resultset;
    }

    public void print() {
        System.out.println("Result : ");
        if (!resultset.isEmpty()) {
            for (int i = 0; i<resultset.get(0).length;i++) {
                for (double[] entity : resultset) {
                    System.out.print(entity[i] + " ");
                }
                System.out.println();
            }
        }
    }
}
