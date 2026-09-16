// HEADER_PRESETS 提供常用上游 Header 模板，
// 便于在渠道高级设置中一键追加，避免手工逐条填写。
export interface HeaderPair {
    header_key: string;
    header_value: string;
}

export interface HeaderPreset {
    id: string;
    name: string;
    headers: HeaderPair[];
}

export const HEADER_PRESETS: HeaderPreset[] = [
    {
        id: 'codex',
        name: 'Codex',
        headers: [
            {
                header_key: 'User-Agent',
                header_value: 'codex_cli_rs/0.101.0 (Mac OS 26.0.1; arm64) Apple_Terminal/464',
            },
            { header_key: 'Originator', header_value: 'codex_cli_rs' },
            { header_key: 'Version', header_value: '0.101.0' },
        ],
    },
];
