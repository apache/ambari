export declare class CliConfig<JsonType> {
    private _configPath;
    private _config;
    constructor(_configPath: string, schema: Object, configJson: JsonType, fallbacks?: JsonType[]);
    readonly config: JsonType;
    save(path?: string): void;
    serialize(mimetype?: string): string;
    alias(path: string, newPath: string): boolean;
    get(jsonPath?: string): any;
    typeOf(jsonPath: string): string;
    isDefined(jsonPath: string): boolean;
    deletePath(jsonPath: string): void;
    set(jsonPath: string, value: any): void;
    getPaths(baseJsonPath: string, keys: string[]): {
        [k: string]: any;
    };
    static fromJson<ConfigType>(content: ConfigType, ...global: ConfigType[]): CliConfig<ConfigType>;
    static fromConfigPath<T>(configPath: string, otherPath?: string[]): CliConfig<T>;
}
