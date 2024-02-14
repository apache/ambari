import { ConstructedOptions } from './ConstructedOptions';
import { Module } from './Module';
import { LicenseWebpackPluginError } from './LicenseWebpackPluginError';
declare class LicenseExtractor {
    private context;
    private options;
    private errors;
    static UNKNOWN_LICENSE: string;
    private modulePrefix;
    private moduleCache;
    constructor(context: string, options: ConstructedOptions, errors: LicenseWebpackPluginError[]);
    parsePackage(packageName: string): boolean;
    getCachedPackage(packageName: string): Module;
    private getLicenseName(packageJson);
    private getLicenseFilename(packageJson, licenseName);
    private getLicenseText(packageJson, licenseName);
    private readPackageJson(packageName);
}
export { LicenseExtractor };
