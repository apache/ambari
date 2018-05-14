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

package org.apache.ambari.view.hive2.internal;


import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

/**
 * Holder for query submitted by the user
 * This may contain multiple hive queries
 */
public class HiveQuery {

    private String query;

    public HiveQuery(String query) {
        this.query = query;
    }

    public HiveQueries fromMultiLineQuery(String multiLineQuery){
        return new HiveQueries(multiLineQuery);
    }


    public static class HiveQueries{

        static final String QUERY_SEP = ";";
        Collection<HiveQuery> hiveQueries;

        private HiveQueries(String userQuery) {
            hiveQueries = FluentIterable.from(Arrays.asList(userQuery.split(QUERY_SEP)))
                    .transform(new Function<String, HiveQuery>() {
                        @Nullable
                        @Override
                        public HiveQuery apply(@Nullable String input) {
                            return new HiveQuery(input.trim());
                        }
                    }).toList();
        }





    }




};
