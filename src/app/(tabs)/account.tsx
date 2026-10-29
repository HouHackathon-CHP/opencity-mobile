import { Ionicons } from '@expo/vector-icons';
import { Text, View } from 'react-native';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, Gate, Heading, Page, styles } from '@/lib/ui';
import { LogoIcon } from '@/components/Logo';

export default function Account() {
  const auth = useAuth();

  return (
    <Gate>
      <Page>
        <Heading
          eyebrow="Hồ sơ cá nhân"
          title="Tài khoản công dân"
          subtitle="Quản lý định danh số, phiên đăng nhập và bảo mật thông tin trên thiết bị."
        />

        {/* Citizen ID Card Visual */}
        <View
          style={{
            backgroundColor: '#0f172a',
            borderRadius: 24,
            padding: 22,
            marginBottom: 20,
            shadowColor: '#0f172a',
            shadowOffset: { width: 0, height: 6 },
            shadowOpacity: 0.12,
            shadowRadius: 14,
            elevation: 4,
          }}
        >
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
              <LogoIcon size={44} />
              <View>
                <Text style={{ color: colors.white, fontSize: 16, fontWeight: '800' }}>
                  Công Dân Số OpenCity
                </Text>
                <Text style={{ color: '#93c5fd', fontSize: 12, fontWeight: '600' }}>
                  Định danh DX-OS Keycloak
                </Text>
              </View>
            </View>

            <View
              style={{
                backgroundColor: 'rgba(52, 211, 153, 0.15)',
                paddingHorizontal: 9,
                paddingVertical: 4,
                borderRadius: 999,
                borderWidth: 1,
                borderColor: 'rgba(52, 211, 153, 0.3)',
              }}
            >
              <Text style={{ color: '#34d399', fontSize: 11, fontWeight: '800' }}>
                XÁC THỰC
              </Text>
            </View>
          </View>

          <View style={{ gap: 8, paddingTop: 14, borderTopWidth: 1, borderTopColor: 'rgba(255, 255, 255, 0.1)' }}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
              <Text style={{ color: '#94a3b8', fontSize: 12 }}>Chuẩn bảo mật:</Text>
              <Text style={{ color: colors.white, fontSize: 12, fontWeight: '700' }}>
                OIDC / OAuth 2.0 PKCE
              </Text>
            </View>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
              <Text style={{ color: '#94a3b8', fontSize: 12 }}>Lưu trữ phiên:</Text>
              <Text style={{ color: colors.white, fontSize: 12, fontWeight: '700' }}>
                Expo SecureStore (Mã hóa phần cứng)
              </Text>
            </View>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
              <Text style={{ color: '#94a3b8', fontSize: 12 }}>Phạm vi đơn vị:</Text>
              <Text style={{ color: colors.white, fontSize: 12, fontWeight: '700' }}>
                Cộng đồng đô thị cơ sở
              </Text>
            </View>
          </View>
        </View>

        {/* Feature status information */}
        <Card>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 }}>
            <Ionicons name="shield-checkmark-outline" size={20} color={colors.primary} />
            <Text style={styles.cardTitle}>Quyền riêng tư & Minh bạch</Text>
          </View>
          <Text style={styles.muted}>
            Các phản ánh bạn gửi được mã hóa và bảo mật trong đơn vị quản trị. Hình ảnh và vị trí chỉ hiển thị cho cán bộ được giao thụ lý hồ sơ.
          </Text>
        </Card>

        {/* App Info Card */}
        <Card>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 }}>
            <Ionicons name="information-circle-outline" size={20} color={colors.muted} />
            <Text style={styles.cardTitle}>Về ứng dụng OpenCity</Text>
          </View>
          <Text style={styles.muted}>
            Phiên bản: 1.0.0 (Mã nguồn mở Apache-2.0)
          </Text>
          <Text style={[styles.muted, { marginTop: 4 }]}>
            Vận hành trên DX-OS OpenCore bởi tổ chức HouHackathon-CHP.
          </Text>
        </Card>

        {/* Sign Out Button */}
        <View style={{ marginTop: 12 }}>
          <Button
            title="Đăng xuất khỏi thiết bị"
            icon="log-out-outline"
            danger
            onPress={auth.signOut}
          />
        </View>
      </Page>
    </Gate>
  );
}
