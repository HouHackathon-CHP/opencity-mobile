import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text } from 'react-native';
import { api, Report } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { reportState } from '@/lib/report-state';
import { Card, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function ReportDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [item, setItem] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (auth.signedIn && id) api<Report>(`/reports/${encodeURIComponent(id)}`, auth.accessToken).then(setItem).catch(cause => setError(String(cause))); }, [auth.signedIn, auth.accessToken, id]);
  return <Gate><Page><State error={error} loading={!item && !error} />{item ? <>
    <Heading eyebrow={reportState(item.state)} title={item.title} subtitle={`${item.category} · ${formatDate(item.created_at)}`} />
    <Card><Text style={styles.label}>Nội dung</Text><Text style={styles.muted}>{item.description}</Text></Card>
    <Card><Text style={styles.label}>Địa điểm</Text><Text style={styles.muted}>{item.address}</Text></Card>
    <Heading title="Tiến trình xử lý" />
    {(item.history || []).map((event, index) => <Card key={`${event.action}-${index}`}><Text style={styles.cardTitle}>{reportState(event.action)}</Text><Text style={styles.muted}>{event.at || event.timestamp ? formatDate(event.at || event.timestamp || '') : ''}</Text></Card>)}
    {item.attachment_ids.length ? <Text style={styles.muted}>Đã gửi {item.attachment_ids.length} ảnh đính kèm.</Text> : null}
  </> : null}</Page></Gate>;
}
