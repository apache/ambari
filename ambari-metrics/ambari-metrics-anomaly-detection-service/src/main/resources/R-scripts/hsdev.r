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

hsdev_daily <- function(train_data, test_data, n, num_historic_periods, interval, period) {

  #res <- get_data(url)
  #data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
  #names(data) <- c("TS", res$metrics[[1]]$metricname)
  anomalies <- data.frame()

  granularity <- train_data[2,1] - train_data[1,1]
  test_start <- test_data[1,1]
  test_end <- test_data[length(test_data[1,]),1]
  train_start <- test_start - num_historic_periods*period
  # round to start of day
  train_start <- train_start - (train_start %% interval)

  time <- as.POSIXlt(as.numeric(test_data[1,1])/1000, origin = "1970-01-01" ,tz = "GMT")
  test_data_day <- time$wday

  h_data <- c()
  for ( i in length(train_data[,1]):1) {
    ts <- train_data[i,1]
    if ( ts < train_start) {
      break
    }
    time <- as.POSIXlt(as.numeric(ts)/1000, origin = "1970-01-01" ,tz = "GMT")
    if (time$wday == test_data_day) {
      x <- train_data[i,2]
      h_data <- c(h_data, x)
    }
  }

  if (length(h_data) < 2*length(test_data[,1])) {
    cat ("\nNot enough training data")
    return (anomalies)
  }

  past_median <- median(h_data)
  past_sd <- sd(h_data)
  curr_median <- median(test_data[,2])

  if (abs(curr_median - past_median) > n * past_sd) {
    anomaly <- c(test_start, test_end, curr_median, past_median, past_sd)
    anomalies <- rbind(anomalies, anomaly)
  }

  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS Start", "TS End", "Current Median", "Past Median", "Past SD")
  }

  return (anomalies)
}
