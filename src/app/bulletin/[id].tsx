import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text } from 'react-native';
import { api, Bulletin } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function BulletinDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [item, setItem] = useState<Bulletin | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (auth.signedIn && id) api<Bulletin>(`/bulletins/${encodeURIComponent(id)}`, auth.accessToken).then(setItem).catch(cause => setError(String(cause))); }, [auth.signedIn, auth.accessToken, id]);
  return <Gate><Page><State error={error} loading={!item && !error} />{item ? <>
    <Heading eyebrow={item.category === 'alert' ? 'Cảnh báo' : 'Tin tức'} title={item.title} subtitle={`${item.area || 'OpenCity'} · ${formatDate(item.published_at)}`} />
    <Card><Text style={[styles.cardTitle, { color: item.category === 'alert' ? colors.red : colors.green }]}>{item.summary}</Text><Text style={[styles.muted, { marginTop: 12, fontSize: 16, lineHeight: 25 }]}>{item.body}</Text></Card>
    {item.latitude !== null && item.longitude !== null ? <Text style={styles.muted}>Vị trí: {item.latitude}, {item.longitude}. Xem điểm này trong tab Bản đồ.</Text> : null}
  </> : null}</Page></Gate>;
}
