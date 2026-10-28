import { Text } from 'react-native';
import { useAuth } from '@/lib/auth';
import { Button, Card, Gate, Heading, Page, styles } from '@/lib/ui';

export default function Account() {
  const auth = useAuth();
  return <Gate><Page><Heading eyebrow="Hồ sơ" title="Tài khoản công dân" subtitle="Phiên đăng nhập được lưu an toàn trên thiết bị." />
    <Card><Text style={styles.cardTitle}>Đã kết nối DX-OS</Text><Text style={styles.muted}>Bạn có thể xem phản ánh của mình, tin tức và cảnh báo trong cùng tenant.</Text></Card>
    <Button title="Đăng xuất" icon="log-out-outline" onPress={auth.signOut} secondary />
  </Page></Gate>;
}
