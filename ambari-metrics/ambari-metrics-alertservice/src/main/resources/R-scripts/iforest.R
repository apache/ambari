ams_iforest <- function(url, train_start, train_end, test_start, test_end, threshold_score) {
  
  res <- get_data(url)
  num_metrics <- length(res$metrics)
  anomalies <- data.frame()
  
  metricname <- res$metrics[[1]]$metricname
  data <- data.frame(as.numeric(names(res$metrics[[1]]$metrics)), as.numeric(res$metrics[[1]]$metrics))
  names(data) <- c("TS", res$metrics[[1]]$metricname)

  for (i in 2:num_metrics) {
    metricname <- res$metrics[[i]]$metricname
    df <- data.frame(as.numeric(names(res$metrics[[i]]$metrics)), as.numeric(res$metrics[[i]]$metrics))
    names(df) <- c("TS", res$metrics[[i]]$metricname)
    data <- merge(data, df)
  }
  
  algo_data <- data[ which(df$TS >= train_start & df$TS <= train_end) , ][c(1:num_metrics+1)]
  iForest <- IsolationTrees(algo_data)
  test_data <- data[ which(df$TS >= test_start & df$TS <= test_end) , ]
  
  if_res <- AnomalyScore(test_data[c(1:num_metrics+1)], iForest)
  for (i in 1:length(if_res$outF)) {
    index <- test_start+i-1
    if (if_res$outF[i] > threshold_score) {
      anomaly <- c(test_data[i,1], if_res$outF[i], if_res$pathLength[i])
      anomalies <- rbind(anomalies, anomaly)
    } 
  }
  
  if(length(anomalies) > 0) {
    names(anomalies) <- c("TS", "Anomaly Score", "Path length")
  }
  return (anomalies)
}