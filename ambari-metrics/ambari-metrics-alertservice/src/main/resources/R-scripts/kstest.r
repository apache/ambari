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