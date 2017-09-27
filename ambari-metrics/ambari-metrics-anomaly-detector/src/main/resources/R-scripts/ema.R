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

#  EMA <- w * EMA + (1 - w) * x
# EMS <- sqrt( w * EMS^2 + (1 - w) * (x - EMA)^2 )
# Alarm = abs(x - EMA) > n * EMS

ema_global <- function(train_data, test_data, w, n) {
  
#  res <- get_data(url)
#  data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
#  names(data) <- c("TS", res$metrics[[1]]$metricname)
#  train_data <- data[which(data$TS >= train_start & data$TS <= train_end), 2]
#  test_data <- data[which(data$TS >= test_start & data$TS <= test_end), ]
  
  anomalies <- data.frame()
  ema <- 0
  ems <- 0

  #Train Step
  for (x in train_data) {
    ema <- w*ema + (1-w)*x
    ems <- sqrt(w* ems^2 + (1 - w)*(x - ema)^2)
  }
  
  for ( i in 1:length(test_data[,1])) {
    x <- test_data[i,2]
    if (abs(x - ema) > n*ems) {
      anomaly <- c(as.numeric(test_data[i,1]), x)
      # print (anomaly)
      anomalies <- rbind(anomalies, anomaly)
    }
    ema <- w*ema + (1-w)*x
    ems <- sqrt(w* ems^2 + (1 - w)*(x - ema)^2)
  }
  
  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS", "Value")
  }
  return (anomalies)
}

ema_daily <- function(train_data, test_data, w, n) {
  
#  res <- get_data(url)
#  data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
#  names(data) <- c("TS", res$metrics[[1]]$metricname)
#  train_data <- data[which(data$TS >= train_start & data$TS <= train_end), ]
#  test_data <- data[which(data$TS >= test_start & data$TS <= test_end), ]
  
  anomalies <- data.frame()
  ema <- vector("numeric", 7)
  ems <- vector("numeric", 7)
  
  #Train Step
  for ( i in 1:length(train_data[,1])) {
    x <- train_data[i,2]
    time <- as.POSIXlt(as.numeric(train_data[i,1])/1000, origin = "1970-01-01" ,tz = "GMT")
    index <- time$wday
    ema[index] <- w*ema[index] + (1-w)*x
    ems[index] <- sqrt(w* ems[index]^2 + (1 - w)*(x - ema[index])^2)
  }
  
  for ( i in 1:length(test_data[,1])) {
    x <- test_data[i,2]
    time <- as.POSIXlt(as.numeric(test_data[i,1])/1000, origin = "1970-01-01" ,tz = "GMT")
    index <- time$wday
    
    if (abs(x - ema[index+1]) > n*ems[index+1]) {
      anomaly <- c(as.numeric(test_data[i,1]), x)
      # print (anomaly)
      anomalies <- rbind(anomalies, anomaly)
    }
    ema[index+1] <- w*ema[index+1] + (1-w)*x
    ems[index+1] <- sqrt(w* ems[index+1]^2 + (1 - w)*(x - ema[index+1])^2)
  }
  
  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS", "Value")
  }
  return(anomalies)
}
