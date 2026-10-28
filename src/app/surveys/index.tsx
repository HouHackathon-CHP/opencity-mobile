import { router } from 'expo-router';
import { Text, View } from 'react-native';
import { SurveySummary } from '@/lib/api';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function Surveys() {
  const { items, loading, error, refresh } = useApiList<SurveySummary>('/surveys');
  return <Gate><Page>
    <Heading eyebrow="Tương tác cộng đồng" title="Khảo sát ý kiến" subtitle="Chọn một phương án cho mỗi khảo sát. Kết quả hiện sau khi bạn trả lời hoặc khi khảo sát đóng." />
    <Button title="Tải lại" icon="refresh-outline" onPress={refresh} secondary />
    <State loading={loading} error={error} empty={!loading && !error && items.length === 0 ? 'Chưa có khảo sát trong đơn vị của bạn.' : undefined} />
    {items.map(item => <Card key={item.id} onPress={() => router.push({ pathname: '/surveys/[id]', params: { id: item.id } })}>
      <Text style={styles.cardTitle}>{item.title}</Text>
      <Text style={styles.muted}>{item.description}</Text>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 12, gap: 10 }}>
        <Text style={{ color: item.status === 'closed' ? colors.muted : colors.green, fontWeight: '700' }}>{item.status === 'closed' ? 'Đã đóng' : item.my_option_id ? 'Đã trả lời' : 'Đang nhận ý kiến'}</Text>
        <Text style={styles.muted}>{formatDate(item.created_at)}</Text>
      </View>
    </Card>)}
  </Page></Gate>;
}
