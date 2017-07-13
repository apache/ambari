# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

args <- commandArgs(trailingOnly = TRUE)
options(repos=c("http://cran.rstudio.com"))
tryCatch({
  if (suppressWarnings(!require(args[1], character.only=T))) install.packages(args[1])
},
warning = function(w) {print(w); ifelse(grepl("unable to resolve", w) || grepl("non-zero exit status", w), quit(save="no", status=1), quit(save="no", status=0))},
error = function(e) quit(save="no", status=2))
quit(save="no", status=0)
