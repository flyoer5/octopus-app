import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslations } from 'use-intl';
import { Send, Square, Trash2 } from 'lucide-react';
import { apiRequest } from '@/api/client';
import { Button } from '@/components/ui/button';

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
}

interface ChannelStat {
    channel_id: number;
    channel_name: string;
    models: { model_id: number; model_name: string }[];
}

interface ApiKeyItem {
    id: number;
    api_key: string;
    name: string;
}

// Chat 页面：选择渠道与模型，通过 octopus 网关做流式对话测试。
export function Chat() {
    const t = useTranslations('chat');
    const [channelId, setChannelId] = useState('');
    const [modelName, setModelName] = useState('');
    const [apiKeyId, setApiKeyId] = useState('');
    const [input, setInput] = useState('');
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [streaming, setStreaming] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);
    const abortRef = useRef<AbortController | null>(null);

    const channelsQuery = useQuery({
        queryKey: ['chat', 'channels'],
        queryFn: () => apiRequest<ChannelStat[]>('/api/v1/channel/stats'),
    });
    const apikeysQuery = useQuery({
        queryKey: ['chat', 'apikeys'],
        queryFn: () => apiRequest<ApiKeyItem[]>('/api/v1/apikey/list'),
    });

    const channels = channelsQuery.data ?? [];
    const apikeys = apikeysQuery.data ?? [];
    const selectedChannel = channels.find((c) => c.channel_id === Number(channelId));
    const selectedKey = apikeys.find((k) => k.id === Number(apiKeyId));

    useEffect(() => {
        setModelName('');
    }, [channelId]);

    useEffect(() => {
        scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
    }, [messages, streaming]);

    async function send() {
        const content = input.trim();
        if (!content || !modelName || !selectedKey || streaming) {
            return;
        }
        const nextMessages: ChatMessage[] = [...messages, { role: 'user', content }];
        setMessages([...nextMessages, { role: 'assistant', content: '' }]);
        setInput('');
        setStreaming(true);

        const controller = new AbortController();
        abortRef.current = controller;

        try {
            const response = await fetch('/v1/chat/completions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${selectedKey.api_key}`,
                },
                body: JSON.stringify({
                    model: modelName,
                    messages: nextMessages,
                    stream: true,
                }),
                signal: controller.signal,
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text.slice(0, 300));
            }
            if (!response.body) {
                throw new Error('no response body');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            for (;;) {
                const { done, value } = await reader.read();
                if (done) {
                    break;
                }
                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop() ?? '';
                for (const line of lines) {
                    const trimmed = line.trim();
                    if (!trimmed.startsWith('data:')) {
                        continue;
                    }
                    const data = trimmed.slice(5).trim();
                    if (data === '[DONE]') {
                        continue;
                    }
                    try {
                        const json = JSON.parse(data) as {
                            choices?: { delta?: { content?: string } }[];
                        };
                        const delta = json.choices?.[0]?.delta?.content;
                        if (delta) {
                            appendAssistant(delta);
                        }
                    } catch {
                        // 忽略无法解析的 SSE 片段
                    }
                }
            }
        } catch (error) {
            appendAssistant(
                `\n[错误] ${error instanceof Error ? error.message : String(error)}`,
            );
        } finally {
            setStreaming(false);
            abortRef.current = null;
        }
    }

    function appendAssistant(delta: string) {
        setMessages((prev) => {
            const copy = [...prev];
            if (copy.length > 0) {
                const last = copy[copy.length - 1];
                copy[copy.length - 1] = { role: 'assistant', content: last.content + delta };
            }
            return copy;
        });
    }

    function stopStreaming() {
        abortRef.current?.abort();
    }

    function clearChat() {
        if (streaming) {
            abortRef.current?.abort();
        }
        setMessages([]);
    }

    const selectClass =
        'h-9 w-full rounded-xl border border-border bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring';

    return (
        <div className="flex h-full min-h-0 flex-col gap-3">
            <div className="grid grid-cols-1 gap-2 md:grid-cols-4">
                <label className="flex flex-col gap-1">
                    <span className="text-xs text-muted-foreground">{t('channel')}</span>
                    <select className={selectClass} value={channelId} onChange={(e) => setChannelId(e.target.value)}>
                        <option value="">{t('selectPlaceholder')}</option>
                        {channels.map((c) => (
                            <option key={c.channel_id} value={c.channel_id}>
                                {c.channel_name}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="flex flex-col gap-1">
                    <span className="text-xs text-muted-foreground">{t('model')}</span>
                    <select
                        className={selectClass}
                        value={modelName}
                        onChange={(e) => setModelName(e.target.value)}
                        disabled={!selectedChannel}
                    >
                        <option value="">{t('selectPlaceholder')}</option>
                        {(selectedChannel?.models ?? []).map((m) => (
                            <option key={m.model_id} value={m.model_name}>
                                {m.model_name}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="flex flex-col gap-1">
                    <span className="text-xs text-muted-foreground">{t('apiKey')}</span>
                    <select className={selectClass} value={apiKeyId} onChange={(e) => setApiKeyId(e.target.value)}>
                        <option value="">{t('selectPlaceholder')}</option>
                        {apikeys.map((k) => (
                            <option key={k.id} value={k.id}>
                                {k.name || k.api_key}
                            </option>
                        ))}
                    </select>
                </label>
                <div className="flex items-end justify-end">
                    <Button variant="secondary" size="sm" onClick={clearChat} className="rounded-xl">
                        <Trash2 className="size-4" />
                        {t('clear')}
                    </Button>
                </div>
            </div>

            <div
                ref={scrollRef}
                className="min-h-0 flex-1 overflow-y-auto overscroll-contain rounded-3xl border border-border bg-card p-4"
            >
                {messages.length === 0 ? (
                    <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
                        {t('emptyHint')}
                    </div>
                ) : (
                    <div className="space-y-3">
                        {messages.map((msg, idx) => (
                            <div
                                key={idx}
                                className={`max-w-[85%] whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm ${
                                    msg.role === 'user'
                                        ? 'ml-auto bg-primary text-primary-foreground'
                                        : 'bg-muted text-foreground'
                                }`}
                            >
                                {msg.content}
                                {msg.role === 'assistant' &&
                                    idx === messages.length - 1 &&
                                    streaming && (
                                        <span className="ml-0.5 inline-block h-4 w-2 animate-pulse bg-current align-middle" />
                                    )}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="flex items-end gap-2">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault();
                            void send();
                        }
                    }}
                    placeholder={t('inputPlaceholder')}
                    rows={2}
                    className="max-h-40 min-h-0 flex-1 resize-none rounded-2xl border border-border bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
                {streaming ? (
                    <Button variant="secondary" onClick={stopStreaming} className="h-11 rounded-xl px-4">
                        <Square className="size-4" />
                    </Button>
                ) : (
                    <Button
                        onClick={() => void send()}
                        disabled={!modelName || !selectedKey || !input.trim()}
                        className="h-11 rounded-xl px-4"
                    >
                        <Send className="size-4" />
                    </Button>
                )}
            </div>
        </div>
    );
}
