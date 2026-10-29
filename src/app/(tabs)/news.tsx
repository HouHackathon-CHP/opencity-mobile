import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Bulletin } from '@/lib/api';
import { useApiList } from '@/lib/use-api-list';
import { Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function News() {
  const { items, loading, error, refresh } = useApiList<Bulletin>('/bulletins');
  const [filter, setFilter] = useState<'all' | 'news' | 'alert'>('all');

  const visible = items.filter((item) => filter === 'all' || item.category === filter);

  return (
    <Gate>
      <Page>
        <Heading
          eyebrow="Thông tin địa phương"
          title="Tin & Cảnh báo"
          subtitle="Thông tin chính thống, tình hình thời tiết, sự cố giao thông và chỉ đạo điều hành."
        />

        {/* Category Filter Pills */}
        <View style={{ flexDirection: 'row', gap: 8, marginBottom: 16 }}>
          {(
            [
              ['all', 'Tất cả'],
              ['news', 'Tin tức'],
              ['alert', 'Cảnh báo'],
            ] as const
          ).map(([key, label]) => {
            const isSelected = filter === key;
            return (
              <Pressable
                key={key}
                onPress={() => setFilter(key)}
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  gap: 5,
                  backgroundColor: isSelected
                    ? key === 'alert'
                      ? colors.red
                      : colors.primary
                    : colors.white,
                  borderRadius: 18,
                  paddingHorizontal: 15,
                  paddingVertical: 9,
                  borderWidth: 1,
                  borderColor: isSelected
                    ? key === 'alert'
                      ? colors.red
                      : colors.primary
                    : colors.border,
                }}
              >
                {key === 'alert' && (
                  <Ionicons
                    name="warning-outline"
                    size={14}
                    color={isSelected ? colors.white : colors.red}
                  />
                )}
                <Text
                  style={{
                    color: isSelected ? colors.white : colors.ink,
                    fontWeight: '700',
                    fontSize: 12.5,
                  }}
                >
                  {label}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginBottom: 10 }}>
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
          empty={!loading && !error && visible.length === 0 ? 'Chưa có bản tin nào trong danh mục này.' : undefined}
        />

        {visible.map((item) => {
          const isAlert = item.category === 'alert';
          return (
            <Card
              key={item.id}
              style={
                isAlert
                  ? {
                      borderColor: colors.redBorder,
                      backgroundColor: '#fffdfd',
                    }
                  : undefined
              }
              onPress={() => router.push({ pathname: '/bulletin/[id]', params: { id: item.id } })}
            >
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <View
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    gap: 5,
                    backgroundColor: isAlert ? colors.redLight : colors.primaryLight,
                    paddingHorizontal: 8,
                    paddingVertical: 4,
                    borderRadius: 999,
                  }}
                >
                  <Ionicons
                    name={isAlert ? 'warning' : 'newspaper'}
                    size={13}
                    color={isAlert ? colors.red : colors.primary}
                  />
                  <Text
                    style={{
                      color: isAlert ? colors.red : colors.primary,
                      fontSize: 10.5,
                      fontWeight: '800',
                      letterSpacing: 0.5,
                    }}
                  >
                    {isAlert ? 'CẢNH BÁO KHẨN CẤP' : 'TIN ĐÔ THỊ'}
                  </Text>
                </View>

                {item.area ? (
                  <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                    <Ionicons name="location-outline" size={13} color={colors.muted} />
                    <Text style={{ fontSize: 11.5, color: colors.muted, fontWeight: '600' }}>
                      {item.area}
                    </Text>
                  </View>
                ) : null}
              </View>

              <Text style={styles.cardTitle}>{item.title}</Text>
              <Text style={styles.muted} numberOfLines={2}>
                {item.summary}
              </Text>

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
                <Text style={{ fontSize: 11.5, color: colors.lightMuted }}>
                  {formatDate(item.published_at)}
                </Text>

                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
                  <Text style={{ fontSize: 12, fontWeight: '700', color: colors.primary }}>
                    Đọc tiếp
                  </Text>
                  <Ionicons name="arrow-forward" size={13} color={colors.primary} />
                </View>
              </View>
            </Card>
          );
        })}
      </Page>
    </Gate>
  );
}
