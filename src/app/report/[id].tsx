import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';
import { api, Report } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { reportState } from '@/lib/report-state';
import { Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function ReportDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [item, setItem] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (auth.signedIn && id) {
      api<Report>(`/reports/${encodeURIComponent(id)}`, auth.accessToken)
        .then(setItem)
        .catch((cause) => setError(String(cause)));
    }
  }, [auth.signedIn, auth.accessToken, id]);

  return (
    <Gate>
      <Page>
        <State error={error} loading={!item && !error} />
        {item ? (
          <>
            {/* Status Header Badge */}
            <View
              style={{
                backgroundColor: '#0f172a',
                borderRadius: 22,
                padding: 20,
                marginBottom: 16,
              }}
            >
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <Text style={{ color: '#93c5fd', fontSize: 11, fontWeight: '800', letterSpacing: 1.2 }}>
                  HỒ SƠ #{item.id.slice(0, 8).toUpperCase()}
                </Text>

                <View
                  style={{
                    backgroundColor: colors.primary,
                    paddingHorizontal: 9,
                    paddingVertical: 4,
                    borderRadius: 999,
                  }}
                >
                  <Text style={{ color: colors.white, fontSize: 11, fontWeight: '800' }}>
                    {reportState(item.state)}
                  </Text>
                </View>
              </View>

              <Text style={{ color: colors.white, fontSize: 20, fontWeight: '800', marginBottom: 8, lineHeight: 26 }}>
                {item.title}
              </Text>

              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Text style={{ color: '#94a3b8', fontSize: 12 }}>
                  {item.category}
                </Text>
                <Text style={{ color: '#64748b' }}>•</Text>
                <Text style={{ color: '#94a3b8', fontSize: 12 }}>
                  {formatDate(item.created_at)}
                </Text>
              </View>
            </View>

            {/* Description Card */}
            <Card>
              <Text style={[styles.label, { color: colors.primary }]}>Nội dung phản ánh</Text>
              <Text style={[styles.muted, { fontSize: 14.5, lineHeight: 22, color: colors.ink }]}>
                {item.description}
              </Text>
            </Card>

            {/* Location Card */}
            <Card>
              <Text style={[styles.label, { color: colors.primary }]}>Địa điểm hiện trường</Text>
              <View style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 8, marginTop: 4 }}>
                <Ionicons name="location" size={18} color={colors.primary} style={{ marginTop: 2 }} />
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 14, fontWeight: '600', color: colors.ink }}>
                    {item.address}
                  </Text>
                  {item.latitude !== null && item.longitude !== null && (
                    <Text style={{ fontSize: 12, color: colors.muted, marginTop: 4 }}>
                      Tọa độ: {item.latitude.toFixed(5)}, {item.longitude.toFixed(5)}
                    </Text>
                  )}
                </View>
              </View>
            </Card>

            {/* Attachments Card */}
            {item.attachment_ids && item.attachment_ids.length > 0 && (
              <Card>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                  <Ionicons name="images" size={18} color={colors.primary} />
                  <Text style={styles.cardTitle}>Ảnh minh chứng</Text>
                </View>
                <Text style={[styles.muted, { marginTop: 4 }]}>
                  Đã tải lên {item.attachment_ids.length} ảnh hiện trường kèm theo hồ sơ.
                </Text>
              </Card>
            )}

            {/* Processing Timeline Stepper */}
            <Heading
              eyebrow="Kiểm toán quy trình"
              title="Tiến trình xử lý"
              subtitle="Lịch sử từng giai đoạn giải quyết hồ sơ của cán bộ."
            />

            {(item.history || []).length > 0 ? (
              item.history.map((event, index) => {
                const dateStr = event.at || event.timestamp || '';
                return (
                  <Card key={`${event.action}-${index}`} style={{ borderLeftWidth: 4, borderLeftColor: colors.primary }}>
                    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={[styles.cardTitle, { marginBottom: 2 }]}>
                        {reportState(event.action)}
                      </Text>
                      <Ionicons name="checkmark-circle" size={18} color={colors.emerald} />
                    </View>
                    {dateStr ? (
                      <Text style={[styles.muted, { fontSize: 12 }]}>
                        {formatDate(dateStr)}
                      </Text>
                    ) : null}
                  </Card>
                );
              })
            ) : (
              <Card>
                <Text style={styles.muted}>Quy trình đang được chuẩn bị khởi tạo.</Text>
              </Card>
            )}
          </>
        ) : null}
      </Page>
    </Gate>
  );
}
