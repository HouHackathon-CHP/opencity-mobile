import { router } from 'expo-router';
import { Text, View } from 'react-native';
import { Report } from '@/lib/api';
import { reportState } from '@/lib/report-state';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function Reports() {
  const { items, loading, error, refresh } = useApiList<Report>('/reports');
  return <Gate><Page><Heading eyebrow="Phản ánh hiện trường" title="Phản ánh của tôi" subtitle="Theo dõi trạng thái và lịch sử xử lý từng phản ánh." />
    <Button title="Tạo phản ánh" icon="add-circle-outline" onPress={() => router.push('/report/new')} />
    <Button title="Tải lại" icon="refresh-outline" onPress={refresh} secondary />
    <State loading={loading} error={error} empty={!loading && !error && items.length === 0 ? 'Bạn chưa gửi phản ánh nào.' : undefined} />
    {items.map(item => <Card key={item.id} onPress={() => router.push({ pathname: '/report/[id]', params: { id: item.id } })}>
      <Text style={styles.cardTitle}>{item.title}</Text>
      <Text style={styles.muted}>{item.address}</Text>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 12 }}><Text style={{ color: '#166a53', fontWeight: '700' }}>{reportState(item.state)}</Text><Text style={styles.muted}>{formatDate(item.created_at)}</Text></View>
    </Card>)}
  </Page></Gate>;
}
