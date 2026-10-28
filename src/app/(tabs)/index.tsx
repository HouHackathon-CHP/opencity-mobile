import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import catalog from '../../../services/catalog.json';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, Heading, Page, styles } from '@/lib/ui';

type Service = (typeof catalog.services)[number];
const activeRoutes: Record<string, '/reports' | '/news' | '/map'> = {
  'field-reports': '/reports', 'report-map': '/map', 'official-news': '/news', warnings: '/news',
};
export default function Home() {
  const auth = useAuth();
  const [category, setCategory] = useState('feedback');
  const selected = catalog.categories.find(item => item.id === category);
  const services = catalog.services.filter(item => item.category === category);
  const open = (service: Service) => { const route = activeRoutes[service.id]; if (route) router.push(route); };
  return <Page>
    <View style={{ backgroundColor: colors.green, borderRadius: 22, padding: 23, marginBottom: 22 }}>
      <Text style={{ color: '#c2ead9', fontWeight: '700', letterSpacing: 2, marginBottom: 9 }}>OPEN CITY / CÔNG DÂN</Text>
      <Text style={{ color: colors.white, fontSize: 28, fontWeight: '800', marginBottom: 9 }}>Thành phố trong tay bạn</Text>
      <Text style={{ color: '#e1f5ea', lineHeight: 21, marginBottom: 18 }}>Gửi phản ánh, theo dõi xử lý và cập nhật thông tin địa phương.</Text>
      <Button title="Gửi phản ánh" icon="add-circle-outline" onPress={() => router.push('/report/new')} secondary />
    </View>
    <Heading eyebrow="Dịch vụ" title="Danh mục tiện ích" subtitle="Những mục chưa kết nối được hiển thị rõ trạng thái." />
    <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginBottom: 19 }}>
      {catalog.categories.map(item => <Pressable key={item.id} onPress={() => setCategory(item.id)} style={{ paddingHorizontal: 12, paddingVertical: 9, borderRadius: 18, backgroundColor: category === item.id ? colors.green : colors.white, borderWidth: 1, borderColor: category === item.id ? colors.green : '#dfeae4' }}><Text style={{ color: category === item.id ? colors.white : colors.ink, fontSize: 12, fontWeight: '700' }}>{item.name}</Text></Pressable>)}
    </View>
    <Text style={[styles.cardTitle, { marginBottom: 12 }]}>{selected?.name}</Text>
    {services.map(service => {
      const active = !!activeRoutes[service.id];
      return <Card key={service.id} onPress={active ? () => open(service) : undefined}>
        <View style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 13 }}>
          <View style={{ backgroundColor: active ? colors.pale : colors.canvas, borderRadius: 13, padding: 10 }}><Ionicons name={active ? 'checkmark-circle-outline' : 'time-outline'} size={23} color={active ? colors.green : colors.muted} /></View>
          <View style={{ flex: 1 }}><Text style={styles.cardTitle}>{service.name}</Text><Text style={styles.muted}>{service.description}</Text><Text style={{ color: active ? colors.green : colors.amber, fontWeight: '700', fontSize: 12, marginTop: 9 }}>{active ? 'Đang dùng được' : service.status === 'needs-integration' ? 'Chờ kết nối dữ liệu' : 'Đang phát triển'}</Text></View>
        </View>
      </Card>;
    })}
    {!auth.signedIn ? <Button title="Đăng nhập" icon="log-in-outline" onPress={auth.signIn} /> : null}
  </Page>;
}
