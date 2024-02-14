import { BuildOptions } from './build-options';
export interface WebpackConfigOptions {
    projectRoot: string;
    buildOptions: BuildOptions;
    appConfig: any;
}
export declare class NgCliWebpackConfig {
    config: any;
    wco: WebpackConfigOptions;
    constructor(buildOptions: BuildOptions, appConfig: any);
    buildConfig(): any;
    getTargetConfig(webpackConfigOptions: WebpackConfigOptions): any;
    validateBuildOptions(buildOptions: BuildOptions): void;
    addTargetDefaults(buildOptions: BuildOptions): BuildOptions;
    mergeConfigs(buildOptions: BuildOptions, appConfig: any, projectRoot: string): {
        outputPath: string;
        deployUrl: any;
        baseHref: any;
    } & BuildOptions;
    addAppConfigDefaults(appConfig: any): any;
}
