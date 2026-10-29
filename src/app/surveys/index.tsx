import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Pressable, Text, View } from 'react-native';
import { SurveySummary } from '@/lib/api';
import { useApiList } from '@/lib/use-api-list';
import { Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function Surveys() {
  const { items, loading, error, refresh } = useApiList<SurveySummary>('/surveys');

  return (
    <Gate>
      <Page>
        <Heading
          eyebrow="Ý kiến cộng đồng"
          title="Khảo sát cư dân"
          subtitle="Tham gia biểu quyết các chủ trương và chính sách đô thị. Kết quả được bảo mật và tổng hợp tự động."
        />

        <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginBottom: 12 }}>
          <Pressable
            onPress={refresh}
            style={{ flexDirection: 'row', alignItems: 'center', gap: 5, padding: 4 }}
          >
            <Ionicons name="refresh-outline" size={15} color={colors.primary} />
            <Text style={{ color: colors.primary, fontSize: 12.5, fontWeight: '700' }}>
              Làm mới
            </Text>
          </Pressable>
        </View>

        <State
          loading={loading}
          error={error}
          empty={!loading && !error && items.length === 0 ? 'Hiện chưa có cuộc khảo sát nào trong đơn vị của bạn.' : undefined}
        />

        {items.map((item) => {
          const isClosed = item.status === 'closed';
          const hasVoted = !!item.my_option_id;

          let badgeBg = colors.emeraldLight;
          let badgeText = '#047857';
          let badgeLabel = 'Đang tiếp nhận ý kiến';

          if (isClosed) {
            badgeBg = '#f1f5f9';
            badgeText = colors.muted;
            badgeLabel = 'Đã kết thúc';
          } else if (hasVoted) {
            badgeBg = colors.primaryLight;
            badgeText = colors.primary;
            badgeLabel = 'Đã biểu quyết';
          }

          return (
            <Card
              key={item.id}
              onPress={() => router.push({ pathname: '/surveys/[id]', params: { id: item.id } })}
            >
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <View
                  style={{
                    backgroundColor: badgeBg,
                    paddingHorizontal: 9,
                    paddingVertical: 3,
                    borderRadius: 999,
                  }}
                >
                  <Text style={{ color: badgeText, fontSize: 11, fontWeight: '800' }}>
                    {badgeLabel}
                  </Text>
                </View>

                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                  <Ionicons name="time-outline" size={13} color={colors.lightMuted} />
                  <Text style={{ fontSize: 11.5, color: colors.lightMuted }}>
                    {formatDate(item.created_at)}
                  </Text>
                </View>
              </View>

              <Text style={styles.cardTitle}>{item.title}</Text>
              <Text style={styles.muted} numberOfLines={2}>
                {item.description}
              </Text>

              <View
                style={{
                  flexDirection: 'row',
                  justifyContent: 'flex-end',
                  alignItems: 'center',
                  gap: 4,
                  marginTop: 12,
                  paddingTop: 10,
                  borderTopWidth: 1,
                  borderTopColor: colors.canvas,
                }}
              >
                <Text style={{ fontSize: 12, fontWeight: '700', color: colors.primary }}>
                  {hasVoted || isClosed ? 'Xem kết quả' : 'Bình chọn ngay'}
                </Text>
                <Ionicons name="arrow-forward" size={13} color={colors.primary} />
              </View>
            </Card>
          );
        })}
      </Page>
    </Gate>
  );
}
