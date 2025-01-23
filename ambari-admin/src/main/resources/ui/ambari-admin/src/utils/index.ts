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
export const encryptData = (data: string): string => {
    let encryptedValue = data;
    if(!data || typeof data !== "string") {
        console.error("Encryption needs string to be encrypted");
    }
    else{
        encryptedValue = data.split('').map((char) => {
            const asciiCode = char.charCodeAt(0);
            if (asciiCode >= 32 && asciiCode <= 126) {
                return String.fromCharCode(((asciiCode - 32 + 13) % 95) + 32);
            } else {
                return char;
            }
        }).join('');
    }
    //Need to change it to encrypted value once local storage encrypting is merged
    console.log("Encrypted Value",encryptedValue)
    return data
}
export const getFromLocalStorage = (key: string): string => {
    try {
        const value = localStorage.getItem(key);
        return value ? value : '';
    } catch (error) {
        throw new Error(`Error getting data from localStorage: ${error}`);
    }
}
export const setInLocalStorage = (key: string, value: string): void => {
    try {
        localStorage.setItem(key, value);
    } catch (error) {
        throw new Error(`Error setting data in localStorage: ${error}`);
    }
}
export const parseJSONData = (data: string): any => {
    try {
        return JSON.parse(data);
    } catch (error) {
        throw new Error(`Error parsing JSON data: ${error}`);
    }
}
export const decryptData = (data: string): string => {
    let encryptedValue = data;
    let decryptedValue = data;
    if(!data || typeof data !== "string"){
        console.error("Decryption needs string to be encrypted");
    }
    else{
        decryptedValue = encryptedValue.split('').map((char) => {
            const asciiCode = char.charCodeAt(0);
            if (asciiCode >= 32 && asciiCode <= 126) {
                return String.fromCharCode(((asciiCode - 32 - 13 + 95) % 95) + 32);
            } else {
                return char;
            }
        }).join('');
    }
    console.log("Decrypted Value",decryptedValue)
    //Need to change it to encrypted value once local storage encrypting is merged
    return data
}
