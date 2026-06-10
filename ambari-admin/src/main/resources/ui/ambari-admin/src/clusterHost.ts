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