import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Bulletin } from '@/lib/api';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function News() {
  const { items, loading, error, refresh } = useApiList<Bulletin>('/bulletins');
  const [filter, setFilter] = useState<'all' | 'news' | 'alert'>('all');
  const visible = items.filter(item => filter === 'all' || item.category === filter);
  return <Gate><Page><Heading eyebrow="Thông tin địa phương" title="Tin & cảnh báo" subtitle="Nội dung do đơn vị vận hành OpenCity đăng tải." />
    <View style={{ flexDirection: 'row', gap: 8, marginBottom: 16 }}>{([['all', 'Tất cả'], ['news', 'Tin tức'], ['alert', 'Cảnh báo']] as const).map(([key, label]) => <Pressable key={key} onPress={() => setFilter(key)} style={{ backgroundColor: filter === key ? colors.green : colors.white, borderRadius: 18, paddingHorizontal: 15, paddingVertical: 10 }}><Text style={{ color: filter === key ? colors.white : colors.ink, fontWeight: '700' }}>{label}</Text></Pressable>)}</View>
    <Button title="Tải lại" icon="refresh-outline" onPress={refresh} secondary />
    <State loading={loading} error={error} empty={!loading && !error && visible.length === 0 ? 'Chưa có bản tin trong mục này.' : undefined} />
    {visible.map(item => <Card key={item.id} onPress={() => router.push({ pathname: '/bulletin/[id]', params: { id: item.id } })}>
      <Text style={{ color: item.category === 'alert' ? colors.red : colors.green, fontWeight: '800', marginBottom: 6 }}>{item.category === 'alert' ? 'CẢNH BÁO' : 'TIN TỨC'}</Text>
      <Text style={styles.cardTitle}>{item.title}</Text><Text style={styles.muted}>{item.summary}</Text>
      <Text style={[styles.muted, { marginTop: 10 }]}>{item.area ? `${item.area} · ` : ''}{formatDate(item.published_at)}</Text>
    </Card>)}
  </Page></Gate>;
}
