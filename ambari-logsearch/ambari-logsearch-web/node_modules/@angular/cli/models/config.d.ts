import { CliConfig as CliConfigBase } from './config/config';
import { CliConfig as ConfigInterface } from '../lib/config/schema';
export declare const CLI_CONFIG_FILE_NAME = ".angular-cli.json";
export declare class CliConfig extends CliConfigBase<ConfigInterface> {
    static configFilePath(projectPath?: string): string;
    static getValue(jsonPath: string): any;
    static globalConfigFilePath(): string;
    static fromGlobal(): CliConfig;
    static fromProject(projectPath?: string): CliConfig;
    static addAliases(cliConfig: CliConfigBase<ConfigInterface>): void;
}
