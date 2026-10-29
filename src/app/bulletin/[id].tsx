import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';
import { api, Bulletin } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function BulletinDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [item, setItem] = useState<Bulletin | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (auth.signedIn && id) {
      api<Bulletin>(`/bulletins/${encodeURIComponent(id)}`, auth.accessToken)
        .then(setItem)
        .catch((cause) => setError(String(cause)));
    }
  }, [auth.signedIn, auth.accessToken, id]);

  const isAlert = item?.category === 'alert';

  return (
    <Gate>
      <Page>
        <State error={error} loading={!item && !error} />
        {item ? (
          <>
            {/* Header Category Banner */}
            <View
              style={{
                backgroundColor: isAlert ? colors.redLight : colors.primaryLight,
                borderWidth: 1,
                borderColor: isAlert ? colors.redBorder : colors.primaryBorder,
                borderRadius: 20,
                padding: 18,
                marginBottom: 16,
              }}
            >
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                <Ionicons
                  name={isAlert ? 'warning' : 'newspaper'}
                  size={18}
                  color={isAlert ? colors.red : colors.primary}
                />
                <Text
                  style={{
                    fontSize: 12,
                    fontWeight: '800',
                    color: isAlert ? colors.red : colors.primary,
                    letterSpacing: 0.5,
                  }}
                >
                  {isAlert ? 'CẢNH BÁO KHẨN CẤP ĐÔ THỊ' : 'BẢN TIN CHÍNH THỐNG'}
                </Text>
              </View>

              <Text style={{ fontSize: 22, fontWeight: '800', color: colors.ink, lineHeight: 28 }}>
                {item.title}
              </Text>

              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 10 }}>
                {item.area ? (
                  <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                    <Ionicons name="location-outline" size={13} color={colors.muted} />
                    <Text style={{ fontSize: 12, color: colors.muted, fontWeight: '600' }}>
                      {item.area}
                    </Text>
                    <Text style={{ color: colors.borderDark }}>•</Text>
                  </View>
                ) : null}
                <Text style={{ fontSize: 12, color: colors.muted }}>
                  {formatDate(item.published_at)}
                </Text>
              </View>
            </View>

            {/* Summary Highlight Box */}
            <View
              style={{
                backgroundColor: colors.white,
                borderRadius: 16,
                borderLeftWidth: 4,
                borderLeftColor: isAlert ? colors.red : colors.primary,
                borderWidth: 1,
                borderColor: colors.border,
                padding: 16,
                marginBottom: 16,
              }}
            >
              <Text style={{ fontSize: 11, fontWeight: '800', color: isAlert ? colors.red : colors.primary, marginBottom: 4, letterSpacing: 0.5 }}>
                TÓM LƯỢC NỘI DUNG:
              </Text>
              <Text style={{ fontSize: 14.5, fontWeight: '600', color: colors.ink, lineHeight: 21 }}>
                {item.summary}
              </Text>
            </View>

            {/* Full Body Card */}
            <Card>
              <Text style={{ fontSize: 15, lineHeight: 24, color: colors.ink }}>
                {item.body}
              </Text>
            </Card>

            {/* Location Link to Map if Coordinates present */}
            {item.latitude !== null && item.longitude !== null ? (
              <Card style={{ backgroundColor: colors.white }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                  <Ionicons name="map" size={18} color={colors.primary} />
                  <Text style={styles.cardTitle}>Vị trí trên bản đồ</Text>
                </View>
                <Text style={[styles.muted, { marginBottom: 14 }]}>
                  Tọa độ sự vụ: {item.latitude.toFixed(5)}, {item.longitude.toFixed(5)}
                </Text>
                <Button
                  title="Mở tab Bản đồ để xem điểm này"
                  icon="navigate-outline"
                  secondary
                  onPress={() => router.push('/(tabs)/map')}
                />
              </Card>
            ) : null}
          </>
        ) : null}
      </Page>
    </Gate>
  );
}
