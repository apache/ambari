hsdev_daily <- function(train_data, test_data, n, num_historic_periods, interval, period) {

  #res <- get_data(url)
  #data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
  #names(data) <- c("TS", res$metrics[[1]]$metricname)
  anomalies <- data.frame()

  granularity <- train_data[2,1] - train_data[1,1]
  test_start <- test_data[1,1]
  test_end <- test_data[length(test_data[1,]),1]
  cat ("\n test_start : ", as.numeric(test_start))
  train_start <- test_start - num_historic_periods*period
  cat ("\n train_start : ", as.numeric(train_start))
  # round to start of day
  train_start <- train_start - (train_start %% interval)
  cat ("\n train_start after rounding: ", as.numeric(train_start))

  time <- as.POSIXlt(as.numeric(test_data[1,1])/1000, origin = "1970-01-01" ,tz = "GMT")
  test_data_day <- time$wday

  h_data <- c()
  for ( i in length(train_data[,1]):1) {
    ts <- train_data[i,1]
    if ( ts < train_start) {
      cat ("\n Breaking out of loop : ", ts)
      break
    }
    time <- as.POSIXlt(as.numeric(ts)/1000, origin = "1970-01-01" ,tz = "GMT")
    if (time$wday == test_data_day) {
      x <- train_data[i,2]
      h_data <- c(h_data, x)
    }
  }

  cat ("\n Train data length : ", length(train_data[,1]))
  cat ("\n Test data length : ", length(test_data[,1]))
  cat ("\n Historic data length : ", length(h_data))
  if (length(h_data) < 2*length(test_data[,1])) {
    cat ("\nNot enough training data")
    return (anomalies)
  }

  past_median <- median(h_data)
  cat ("\npast_median : ", past_median)
  past_sd <- sd(h_data)
  cat ("\npast_sd : ", past_sd)
  curr_median <- median(test_data[,2])
  cat ("\ncurr_median : ", curr_median)

  if (abs(curr_median - past_median) > n * past_sd) {
    anomaly <- c(test_start, test_end, curr_median, past_median, past_sd)
    anomalies <- rbind(anomalies, anomaly)
  }

  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS Start", "TS End", "Current Median", "Past Median", " Past SD")
  }

  return (anomalies)
}
