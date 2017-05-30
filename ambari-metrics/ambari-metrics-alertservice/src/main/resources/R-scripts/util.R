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