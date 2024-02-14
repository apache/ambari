import { ConstructedOptions } from './ConstructedOptions';
import { Module } from './Module';
import { LicenseWebpackPluginError } from './LicenseWebpackPluginError';
declare class ModuleProcessor {
    private context;
    private options;
    private errors;
    private modulePrefix;
    private licenseExtractor;
    constructor(context: string, options: ConstructedOptions, errors: LicenseWebpackPluginError[]);
    processFile(filename: string): string | null;
    processPackage(packageName: string): string | null;
    getPackageInfo(packageName: string): Module;
    private extractPackageName(filename);
    private isFromNodeModules(filename);
}
export { ModuleProcessor };
