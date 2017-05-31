#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#url_prefix = 'http://104.196.95.78:3000/api/datasources/proxy/1/ws/v1/timeline/metrics?'
#url_suffix = '&startTime=1459972944&endTime=1491508944&precision=MINUTES'
#data_url <- paste(url_prefix, query, sep ="")
#data_url <- paste(data_url, url_suffix, sep="")

get_data <- function(url) {
  library(rjson)
  res <- fromJSON(readLines(url)[1])
  return (res)
}

find_index <- function(data, ts) {
  for (i in 1:length(data)) {
    if (as.numeric(ts) == as.numeric(data[i])) {
      return (i)
    }
  }
  return (-1)
}