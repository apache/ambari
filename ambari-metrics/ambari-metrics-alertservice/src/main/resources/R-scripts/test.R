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


tukeys_anomalies <- data.frame()
ema_global_anomalies <- data.frame()
ema_daily_anomalies <- data.frame()
ks_anomalies <- data.frame()
hsdev_anomalies <- data.frame()

init <- function() {
  tukeys_anomalies <- data.frame()
  ema_global_anomalies <- data.frame()
  ema_daily_anomalies <- data.frame()
  ks_anomalies <- data.frame()
  hsdev_anomalies <- data.frame()
}

test_methods <- function(data) {

  init()
  #res <- get_data(url)
  #data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
  #names(data) <- c("TS", res$metrics[[1]]$metricname)

  limit <- data[length(data[,1]),1]
  step <- data[2,1] - data[1,1]

  train_start <- data[1,1]
  train_end <- get_next_day_boundary(train_start, step, limit)
  test_start <- train_end + step
  test_end <- get_next_day_boundary(test_start, step, limit)
  i <- 1
  day <- 24*60*60*1000

  while (test_start < limit) {

    print (i)
    i <- i + 1
    train_data <- data[which(data$TS >= train_start & data$TS <= train_end),]
    test_data <- data[which(data$TS >= test_start & data$TS <= test_end), ]

    #tukeys_anomalies <<- rbind(tukeys_anomalies, ams_tukeys(train_data, test_data, 3))
    #ema_global_anomalies <<- rbind(ema_global_anomalies, ema_global(train_data, test_data, 0.9, 3))
    #ema_daily_anomalies <<- rbind(ema_daily_anomalies, ema_daily(train_data, test_data, 0.9, 3))
    #ks_anomalies <<- rbind(ks_anomalies, ams_ks(train_data, test_data, 0.05))
    hsdev_train_data <- data[which(data$TS < test_start),]
    hsdev_anomalies <<- rbind(hsdev_anomalies, hsdev_daily(hsdev_train_data, test_data, 3, 3, day, 7*day))

    train_start <- test_start
    train_end <- get_next_day_boundary(train_start, step, limit)
    test_start <- train_end + step
    test_end <- get_next_day_boundary(test_start, step, limit)
  }
  return (hsdev_anomalies)
}

get_next_day_boundary <- function(start, step, limit) {

  if (start > limit) {
    return (-1)
  }

  while (start <= limit) {
    if (((start %% (24*60*60*1000)) - 28800000) == 0) {
      return (start)
    }
    start <- start + step
  }
  return (start)
}
