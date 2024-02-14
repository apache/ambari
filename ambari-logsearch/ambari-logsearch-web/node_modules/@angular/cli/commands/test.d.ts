export interface TestOptions {
    watch?: boolean;
    codeCoverage?: boolean;
    singleRun?: boolean;
    browsers?: string;
    colors?: boolean;
    log?: string;
    port?: number;
    reporters?: string;
    sourcemaps?: boolean;
    progress?: boolean;
    config: string;
    poll?: number;
    environment?: string;
    app?: string;
}
declare const TestCommand: any;
export default TestCommand;
