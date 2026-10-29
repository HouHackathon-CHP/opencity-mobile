import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Report } from '@/lib/api';
import { reportState } from '@/lib/report-state';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

function statusBadgeColors(state: string) {
  switch (state) {
    case 'resolved':
      return { bg: colors.emeraldLight, text: '#047857' };
    case 'assigned':
      return { bg: colors.pale, text: colors.primary };
    case 'triaged':
      return { bg: colors.amberLight, text: '#b45309' };
    case 'submitted':
    default:
      return { bg: '#e0f2fe', text: '#0369a1' };
  }
}

export default function Reports() {
  const { items, loading, error, refresh } = useApiList<Report>('/reports');
  const [filterState, setFilterState] = useState<string>('all');

  const filteredItems = items.filter((item) => {
    if (filterState === 'all') return true;
    return item.state === filterState;
  });

  return (
    <Gate>
      <Page>
        <Heading
          eyebrow="Tương tác công dân"
          title="Hồ sơ phản ánh"
          subtitle="Giám sát minh bạch từng bước xử lý của cơ quan chức năng đối với vấn đề bạn phản ánh."
        />

        {/* Primary Action Button */}
        <Button
          title="Gửi phản ánh sự cố mới"
          icon="camera"
          onPress={() => router.push('/report/new')}
        />

        {/* State Filter Tabs */}
        <View style={{ flexDirection: 'row', gap: 6, marginVertical: 12, flexWrap: 'wrap' }}>
          {[
            ['all', `Tất cả (${items.length})`],
            ['submitted', 'Mới'],
            ['triaged', 'Đã tiếp nhận'],
            ['assigned', 'Đang xử lý'],
            ['resolved', 'Hoàn tất'],
          ].map(([key, label]) => {
            const isSelected = filterState === key;
            return (
              <Pressable
                key={key}
                onPress={() => setFilterState(key)}
                style={{
                  paddingHorizontal: 12,
                  paddingVertical: 7,
                  borderRadius: 16,
                  backgroundColor: isSelected ? colors.ink : colors.white,
                  borderWidth: 1,
                  borderColor: isSelected ? colors.ink : colors.border,
                }}
              >
                <Text
                  style={{
                    color: isSelected ? colors.white : colors.muted,
                    fontSize: 12,
                    fontWeight: '700',
                  }}
                >
                  {label}
                </Text>
              </Pressable>
            );
          })}
        </View>

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
          empty={
            !loading && !error && filteredItems.length === 0
              ? items.length === 0
                ? 'Bạn chưa gửi phản ánh nào. Hãy nhấn "Gửi phản ánh sự cố mới" để bắt đầu.'
                : 'Không có phản ánh nào trong mục này.'
              : undefined
          }
        />

        {filteredItems.map((item) => {
          const badge = statusBadgeColors(item.state);
          return (
            <Card
              key={item.id}
              onPress={() => router.push({ pathname: '/report/[id]', params: { id: item.id } })}
            >
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <Text style={{ fontSize: 11, fontWeight: '800', color: colors.primary, textTransform: 'uppercase', letterSpacing: 0.5 }}>
                  {item.category}
                </Text>

                <View
                  style={{
                    backgroundColor: badge.bg,
                    paddingHorizontal: 8,
                    paddingVertical: 3,
                    borderRadius: 999,
                  }}
                >
                  <Text style={{ color: badge.text, fontSize: 11, fontWeight: '800' }}>
                    {reportState(item.state)}
                  </Text>
                </View>
              </View>

              <Text style={styles.cardTitle}>{item.title}</Text>

              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 4 }}>
                <Ionicons name="location-outline" size={14} color={colors.muted} />
                <Text style={[styles.muted, { flex: 1 }]} numberOfLines={1}>
                  {item.address}
                </Text>
              </View>

              <View
                style={{
                  flexDirection: 'row',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: 12,
                  paddingTop: 10,
                  borderTopWidth: 1,
                  borderTopColor: colors.canvas,
                }}
              >
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                  <Ionicons name="time-outline" size={13} color={colors.lightMuted} />
                  <Text style={{ color: colors.lightMuted, fontSize: 11.5 }}>
                    {formatDate(item.created_at)}
                  </Text>
                </View>

                {item.attachment_ids?.length > 0 && (
                  <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                    <Ionicons name="image-outline" size={13} color={colors.primary} />
                    <Text style={{ color: colors.primary, fontSize: 11.5, fontWeight: '700' }}>
                      {item.attachment_ids.length} ảnh
                    </Text>
                  </View>
                )}
              </View>
            </Card>
          );
        })}
      </Page>
    </Gate>
  );
}
