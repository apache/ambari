import { Options } from './Options';
declare class LicenseWebpackPlugin {
    private buildRoot;
    private options;
    private moduleProcessor;
    private template;
    private errors;
    constructor(options: Options);
    apply(compiler: any): void;
    private renderLicenseFile(packageNames);
    private findBuildRoot(context);
}
export { LicenseWebpackPlugin };
