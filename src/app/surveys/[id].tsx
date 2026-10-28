import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { api, SurveyDetail } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function SurveyDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [survey, setSurvey] = useState<SurveyDetail | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!auth.signedIn || !id) return;
    let active = true;
    setLoading(true); setError(null);
    api<SurveyDetail>(`/surveys/${encodeURIComponent(id)}`, auth.accessToken)
      .then(value => { if (active) setSurvey(value); })
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : 'Không tải được khảo sát.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [auth.signedIn, auth.accessToken, id]);
  const answer = async () => {
    if (!id || !selected || busy || !survey || survey.status !== 'open' || survey.my_option_id) return;
    setBusy(true); setError(null);
    try {
      const updated = await api<SurveyDetail>(`/surveys/${encodeURIComponent(id)}/answers`, auth.accessToken, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ option_id: selected }),
      });
      setSurvey(updated);
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Không gửi được ý kiến.'); }
    finally { setBusy(false); }
  };
  const results = survey?.total_responses !== null && survey?.total_responses !== undefined;
  return <Gate><Page>
    <State loading={loading} error={!survey ? error : null} />
    {survey ? <>
      <Heading eyebrow={survey.status === 'closed' ? 'Đã đóng' : 'Đang nhận ý kiến'} title={survey.title} subtitle={formatDate(survey.created_at)} />
      <Card><Text style={[styles.muted, { fontSize: 16, lineHeight: 24 }]}>{survey.description}</Text></Card>
      <Text style={[styles.cardTitle, { marginVertical: 10 }]}>{results ? 'Kết quả khảo sát' : 'Chọn một phương án'}</Text>
      {survey.options.map(option => {
        const chosen = survey.my_option_id === option.id || (!survey.my_option_id && selected === option.id);
        const count = option.votes ?? 0;
        const percent = results && survey.total_responses ? Math.round(count * 100 / survey.total_responses) : 0;
        return <Pressable key={option.id} accessibilityRole="radio" accessibilityState={{ selected: chosen, disabled: !!results }}
          disabled={!!results || survey.status === 'closed'} onPress={() => setSelected(option.id)}
          style={[styles.card, { borderColor: chosen ? colors.green : '#e3ece8' }]}>
          <Text style={[styles.cardTitle, { color: chosen ? colors.green : colors.ink }]}>{option.label}{survey.my_option_id === option.id ? ' ✓' : ''}</Text>
          {results ? <><View style={{ height: 7, backgroundColor: colors.pale, borderRadius: 4, marginTop: 7 }}><View style={{ width: `${percent}%`, height: 7, backgroundColor: colors.green, borderRadius: 4 }} /></View><Text style={[styles.muted, { marginTop: 7 }]}>{count} ý kiến · {percent}%</Text></> : null}
        </Pressable>;
      })}
      {!results && survey.status === 'open' ? <Button title={busy ? 'Đang gửi...' : 'Gửi ý kiến'} icon="send-outline" disabled={!selected || busy} onPress={answer} /> : null}
      {results ? <Text style={styles.muted}>Tổng cộng {survey.total_responses} ý kiến trong đơn vị của bạn.</Text> : null}
      {survey.status === 'closed' && !results ? <Text style={styles.muted}>Khảo sát đã đóng.</Text> : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </> : null}
  </Page></Gate>;
}
