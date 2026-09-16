// HEADER_PRESETS 提供 Codex / Claude Code 常用上游 Header 模板，
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
            { header_key: 'OpenAI-Beta', header_value: 'responses=latest' },
        ],
    },
    {
        id: 'claude-code',
        name: 'Claude Code',
        headers: [
            { header_key: 'anthropic-version', header_value: '2023-06-01' },
            {
                header_key: 'anthropic-beta',
                header_value:
                    'interleaved-thinking-2025-05-14,code-execution-2025-05-22,extended-cache-ttl-2025-04-11,mcp-client-2025-04-04,output-128k-2025-02-19,prompt-caching-2024-07-31,token-efficient-tools-2025-02-19',
            },
        ],
    },
];
