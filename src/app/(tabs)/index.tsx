import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import catalog from '../../../services/catalog.json';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, Heading, Page, styles } from '@/lib/ui';
import { Logo } from '@/components/Logo';

type Service = (typeof catalog.services)[number];

const activeRoutes: Record<string, '/reports' | '/news' | '/map' | '/surveys'> = {
  'field-reports': '/reports',
  'report-map': '/map',
  'official-news': '/news',
  warnings: '/news',
  surveys: '/surveys',
};

export default function Home() {
  const auth = useAuth();
  const [category, setCategory] = useState('feedback');

  const selected = catalog.categories.find((item) => item.id === category);
  const services = catalog.services.filter((item) => item.category === category);

  const open = (service: Service) => {
    const route = activeRoutes[service.id];
    if (route) router.push(route);
  };

  return (
    <Page>
      {/* Top Welcome & City Branding */}
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
        <Logo size={36} showSubtitle />

        <Pressable
          onPress={() => router.push('/(tabs)/account')}
          style={{
            width: 42,
            height: 42,
            borderRadius: 21,
            backgroundColor: colors.primaryLight,
            borderWidth: 1.5,
            borderColor: colors.primaryBorder,
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Ionicons
            name={auth.signedIn ? 'person' : 'person-outline'}
            size={20}
            color={colors.primary}
          />
        </Pressable>
      </View>

      {/* Hero Atmosphere Banner */}
      <View
        style={{
          backgroundColor: '#0f172a',
          borderRadius: 24,
          padding: 22,
          marginBottom: 24,
          shadowColor: '#0f172a',
          shadowOffset: { width: 0, height: 8 },
          shadowOpacity: 0.15,
          shadowRadius: 16,
          elevation: 4,
        }}
      >
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 7, marginBottom: 10 }}>
          <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: '#34d399' }} />
          <Text style={{ color: '#93c5fd', fontSize: 11, fontWeight: '800', letterSpacing: 1.2 }}>
            KẾT NỐI TRỰC TUYẾN DX-OS
          </Text>
        </View>

        <Text style={{ color: colors.white, fontSize: 25, fontWeight: '800', letterSpacing: -0.4, lineHeight: 32, marginBottom: 8 }}>
          Đô thị thông minh trong tầm tay bạn
        </Text>

        <Text style={{ color: '#cbd5e1', fontSize: 13.5, lineHeight: 20, marginBottom: 20 }}>
          Gửi phản ánh sự cố hiện trường, theo dõi tiến trình xử lý minh bạch và tiếp nhận cảnh báo khẩn cấp từ chính quyền.
        </Text>

        <View style={{ flexDirection: 'row', gap: 10 }}>
          <View style={{ flex: 1 }}>
            <Button
              title="Gửi phản ánh"
              icon="add-circle"
              onPress={() => router.push('/report/new')}
            />
          </View>
          <View style={{ flex: 1 }}>
            <Button
              title="Xem bản đồ"
              icon="map"
              secondary
              onPress={() => router.push('/(tabs)/map')}
            />
          </View>
        </View>
      </View>

      {/* Quick Access Shortcut Tiles */}
      <View style={{ marginBottom: 24 }}>
        <Text style={{ fontSize: 14, fontWeight: '800', color: colors.ink, marginBottom: 12 }}>
          Lối tắt tiện ích
        </Text>
        <View style={{ flexDirection: 'row', gap: 10 }}>
          <Pressable
            onPress={() => router.push('/report/new')}
            style={({ pressed }) => [
              {
                flex: 1,
                backgroundColor: colors.white,
                borderRadius: 18,
                padding: 14,
                borderWidth: 1,
                borderColor: colors.border,
                alignItems: 'center',
                gap: 8,
              },
              pressed && { opacity: 0.85 },
            ]}
          >
            <View style={{ width: 40, height: 40, borderRadius: 12, backgroundColor: colors.primaryLight, justifyContent: 'center', alignItems: 'center' }}>
              <Ionicons name="camera" size={20} color={colors.primary} />
            </View>
            <Text style={{ fontSize: 12, fontWeight: '700', color: colors.ink, textAlign: 'center' }}>
              Phản ánh
            </Text>
          </Pressable>

          <Pressable
            onPress={() => router.push('/(tabs)/map')}
            style={({ pressed }) => [
              {
                flex: 1,
                backgroundColor: colors.white,
                borderRadius: 18,
                padding: 14,
                borderWidth: 1,
                borderColor: colors.border,
                alignItems: 'center',
                gap: 8,
              },
              pressed && { opacity: 0.85 },
            ]}
          >
            <View style={{ width: 40, height: 40, borderRadius: 12, backgroundColor: colors.emeraldLight, justifyContent: 'center', alignItems: 'center' }}>
              <Ionicons name="map" size={20} color={colors.emerald} />
            </View>
            <Text style={{ fontSize: 12, fontWeight: '700', color: colors.ink, textAlign: 'center' }}>
              Bản đồ
            </Text>
          </Pressable>

          <Pressable
            onPress={() => router.push('/surveys')}
            style={({ pressed }) => [
              {
                flex: 1,
                backgroundColor: colors.white,
                borderRadius: 18,
                padding: 14,
                borderWidth: 1,
                borderColor: colors.border,
                alignItems: 'center',
                gap: 8,
              },
              pressed && { opacity: 0.85 },
            ]}
          >
            <View style={{ width: 40, height: 40, borderRadius: 12, backgroundColor: colors.amberLight, justifyContent: 'center', alignItems: 'center' }}>
              <Ionicons name="stats-chart" size={20} color={colors.amber} />
            </View>
            <Text style={{ fontSize: 12, fontWeight: '700', color: colors.ink, textAlign: 'center' }}>
              Khảo sát
            </Text>
          </Pressable>

          <Pressable
            onPress={() => router.push('/(tabs)/news')}
            style={({ pressed }) => [
              {
                flex: 1,
                backgroundColor: colors.white,
                borderRadius: 18,
                padding: 14,
                borderWidth: 1,
                borderColor: colors.border,
                alignItems: 'center',
                gap: 8,
              },
              pressed && { opacity: 0.85 },
            ]}
          >
            <View style={{ width: 40, height: 40, borderRadius: 12, backgroundColor: colors.redLight, justifyContent: 'center', alignItems: 'center' }}>
              <Ionicons name="notifications" size={20} color={colors.red} />
            </View>
            <Text style={{ fontSize: 12, fontWeight: '700', color: colors.ink, textAlign: 'center' }}>
              Cảnh báo
            </Text>
          </Pressable>
        </View>
      </View>

      {/* Directory Category Filters */}
      <Heading
        eyebrow="Danh mục"
        title="Dịch vụ đô thị số"
        subtitle="Khám phá các dịch vụ theo từng lĩnh vực chuyên đề."
      />

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ gap: 8, paddingBottom: 14 }}
      >
        {catalog.categories.map((item) => {
          const isSelected = category === item.id;
          return (
            <Pressable
              key={item.id}
              onPress={() => setCategory(item.id)}
              style={{
                paddingHorizontal: 14,
                paddingVertical: 9,
                borderRadius: 20,
                backgroundColor: isSelected ? colors.primary : colors.white,
                borderWidth: 1,
                borderColor: isSelected ? colors.primary : colors.border,
                flexDirection: 'row',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <Text
                style={{
                  color: isSelected ? colors.white : colors.ink,
                  fontSize: 12.5,
                  fontWeight: '700',
                }}
              >
                {item.name}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      {/* Services List in selected category */}
      <View style={{ marginTop: 8 }}>
        <Text style={[styles.cardTitle, { marginBottom: 12, fontSize: 15, color: colors.muted }]}>
          {selected?.name} ({services.length} tiện ích)
        </Text>

        {services.map((service) => {
          const active = !!activeRoutes[service.id];
          return (
            <Card key={service.id} onPress={active ? () => open(service) : undefined}>
              <View style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 14 }}>
                <View
                  style={{
                    backgroundColor: active ? colors.primaryLight : '#f1f5f9',
                    borderRadius: 14,
                    padding: 10,
                    width: 44,
                    height: 44,
                    justifyContent: 'center',
                    alignItems: 'center',
                  }}
                >
                  <Ionicons
                    name={active ? 'checkmark-circle' : 'hourglass-outline'}
                    size={22}
                    color={active ? colors.primary : colors.muted}
                  />
                </View>

                <View style={{ flex: 1 }}>
                  <Text style={styles.cardTitle}>{service.name}</Text>
                  <Text style={styles.muted}>{service.description}</Text>

                  <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 8 }}>
                    <View
                      style={{
                        width: 6,
                        height: 6,
                        borderRadius: 3,
                        backgroundColor: active
                          ? colors.emerald
                          : service.status === 'needs-integration'
                          ? colors.muted
                          : colors.amber,
                      }}
                    />
                    <Text
                      style={{
                        color: active
                          ? colors.emerald
                          : service.status === 'needs-integration'
                          ? colors.muted
                          : colors.amber,
                        fontWeight: '700',
                        fontSize: 11.5,
                      }}
                    >
                      {active
                        ? 'Sẵn sàng dùng'
                        : service.status === 'needs-integration'
                        ? 'Chờ kết nối dữ liệu API'
                        : 'Đang trong lộ trình phát triển'}
                    </Text>
                  </View>
                </View>

                {active && (
                  <Ionicons
                    name="chevron-forward"
                    size={18}
                    color={colors.primary}
                    style={{ alignSelf: 'center' }}
                  />
                )}
              </View>
            </Card>
          );
        })}
      </View>

      {!auth.signedIn && (
        <View style={{ marginTop: 14 }}>
          <Button
            title="Đăng nhập tài khoản công dân"
            icon="log-in-outline"
            onPress={auth.signIn}
          />
        </View>
      )}
    </Page>
  );
}
