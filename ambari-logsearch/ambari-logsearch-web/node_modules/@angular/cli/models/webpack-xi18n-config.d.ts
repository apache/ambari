import { NgCliWebpackConfig } from './webpack-config';
export interface XI18WebpackOptions {
    genDir?: string;
    buildDir?: string;
    i18nFormat?: string;
    locale?: string;
    outFile?: string;
    verbose?: boolean;
    progress?: boolean;
    app?: string;
}
export declare class XI18nWebpackConfig extends NgCliWebpackConfig {
    extractOptions: XI18WebpackOptions;
    appConfig: any;
    config: any;
    constructor(extractOptions: XI18WebpackOptions, appConfig: any);
    buildConfig(): any;
}
