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

ams_ks <- function(train_data, test_data, p_value) {
  
#  res <- get_data(url)
#  data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
#  names(data) <- c("TS", res$metrics[[1]]$metricname)
#  train_data <- data[which(data$TS >= train_start & data$TS <= train_end), 2]
#  test_data <- data[which(data$TS >= test_start & data$TS <= test_end), 2]
  
  anomalies <- data.frame()
  res <- ks.test(train_data, test_data[,2])
  
  if (res[2] < p_value) {
    anomaly <- c(test_data[1,1], test_data[length(test_data),1], res[1], res[2])
    anomalies <- rbind(anomalies, anomaly)
  }
 
  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS Start", "TS End", "D", "p-value")
  }
  return (anomalies)
}