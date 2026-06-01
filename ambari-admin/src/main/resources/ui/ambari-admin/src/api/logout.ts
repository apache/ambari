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
import {encryptData, decryptData, getFromLocalStorage, parseJSONData, setInLocalStorage} from "./Utility.ts";
import {adminApi} from "./configs/axiosConfig.ts";
import { AxiosError } from 'axios';

const signOut = async () => {
    let ambariKey = getFromLocalStorage('ambari');
    let data;
    if (ambariKey) {
        data = parseJSONData(decryptData(ambariKey));
    }
    delete data.app.authenticated;
    delete data.app.loginName;
    delete data.app.user;

    //with encrypting set data in LS
    setInLocalStorage('ambari', encryptData(JSON.stringify(data)));

    const headers = {
        'Authorization': 'Basic'
    };

    try {
        const url = "/logout"
        await adminApi.request({
            url: url,
            method: 'GET',
            headers: headers
        });
        localStorage.clear();
        window.location.replace("/#/login");
    } catch (error) {
        const axiosError = error as AxiosError;
        throw new Error(`Logout failed with status: ${axiosError.response?.status}`);
    }
}
export default signOut;