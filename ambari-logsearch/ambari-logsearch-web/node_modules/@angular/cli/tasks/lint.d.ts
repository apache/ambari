export interface CliLintConfig {
    files?: (string | string[]);
    project?: string;
    tslintConfig?: string;
    exclude?: (string | string[]);
}
export declare class LintTaskOptions {
    fix: boolean;
    force: boolean;
    format?: string;
    silent?: boolean;
    typeCheck?: boolean;
    configs: Array<CliLintConfig>;
}
declare const _default: any;
export default _default;
