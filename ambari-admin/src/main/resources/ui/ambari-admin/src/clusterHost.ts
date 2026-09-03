/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// export const DEV_VITE_API_PROXY_TARGET="http://sl73tskrapd1164.visa.com:8080"
// export const PROD_VITE_API_PROXY_TARGET=""

// // # When setting up the project, make sure to set the TOKEN environment variable.
// // # This should be a Basic Auth token generated from your username and password for the given cluster.
// // # You can do this by uncommenting the following line and replacing 'your-basic-auth-token' with your actual Basic Auth token. ( eg 'YWRtaW46VmlzYUAxMjM=')
// export const DEV_VITE_TOKEN="YWRtaW46VmlzYUAxMjM="


export const config={
    development:{
        VITE_API_PROXY_TARGET:"http://##REPLACE_YOUR_AMBARI_SERVER_URL_HERE",
        VITE_TOKEN:"##REPLACE_YOUR_AUTH_TOKEN_HERE"
    },
    production:{
        VITE_API_PROXY_TARGET:"",
        VITE_TOKEN:""
    }
    }
