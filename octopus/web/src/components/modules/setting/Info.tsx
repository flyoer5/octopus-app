import { useTranslations } from 'use-intl';
import { Info } from 'lucide-react';

// SettingInfo 展示应用信息（无版本校验）。
export function SettingInfo() {
    const t = useTranslations('setting');

    return (
        <div className="rounded-3xl border border-border bg-card p-6 space-y-3">
            <h2 className="text-lg font-bold text-card-foreground flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('info.title')}
            </h2>
            <p className="text-sm text-muted-foreground">
                Octopus · LLM API 聚合网关（定制版）
            </p>
        </div>
    );
}
