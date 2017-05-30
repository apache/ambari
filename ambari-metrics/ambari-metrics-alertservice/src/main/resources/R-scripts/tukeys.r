ams_tukeys <- function(train_data, test_data, n) {

#  res <- get_data(url)
#  data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
#  names(data) <- c("TS", res$metrics[[1]]$metricname)
#  train_data <- data[which(data$TS >= train_start & data$TS <= train_end), 2]
#  test_data <- data[which(data$TS >= test_start & data$TS <= test_end), ]

  anomalies <- data.frame()
  quantiles <- quantile(train_data[,2])
  iqr <- quantiles[4] - quantiles[2]

  for ( i in 1:length(test_data[,1])) {
    x <- test_data[i,2]
    lb <- quantiles[2] - n*iqr
    ub <- quantiles[4] + n*iqr
    if ( (x < lb)  || (x > ub) ) {
      anomaly <- c(test_data[i,1], x)
      anomalies <- rbind(anomalies, anomaly)
    }
  }
  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS", "Value")
  }
  return (anomalies)
}
