import { encryptData, decryptData, getFromLocalStorage, parseJSONData, setInLocalStorage } from "./Utility.ts";
import { adminApi } from "./configs/axiosConfig.ts";
import { AxiosError } from 'axios';

const signOut = async () => {
    // var data = JSON.parse(decryptData(localStorage.getItem("ambari")));
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
        'Authorization': 'Basic ' + 'invalid_username:password'
    };

    try {
        const url = "/logout"
        await adminApi.request({
            url: url,
            method: 'GET',
            headers: headers
        });
    } catch (error) {
        const axiosError = error as AxiosError;
        throw new Error(`Logout failed with status: ${axiosError.response?.status}`);
    }
}
export default signOut;