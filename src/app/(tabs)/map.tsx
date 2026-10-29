import { Ionicons } from '@expo/vector-icons';
import { Camera, CameraRef, Map, Marker } from '@maplibre/maplibre-react-native';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Bulletin, Report } from '@/lib/api';
import { MAP_STYLE } from '@/lib/config';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, colors, Gate, Heading, Page, State, styles } from '@/lib/ui';

type Point = {
  id: string;
  title: string;
  description: string;
  latitude: number;
  longitude: number;
  kind: 'report' | 'alert';
};

export default function CityMap() {
  const reports = useApiList<Report>('/reports');
  const bulletins = useApiList<Bulletin>('/bulletins');
  const [selected, setSelected] = useState<Point | null>(null);
  const [filterMode, setFilterMode] = useState<'all' | 'report' | 'alert'>('all');
  const camera = useRef<CameraRef>(null);

  const points: Point[] = [
    ...reports.items
      .filter((item) => item.latitude !== null && item.longitude !== null)
      .map((item) => ({
        id: item.id,
        title: item.title,
        description: item.address,
        latitude: item.latitude!,
        longitude: item.longitude!,
        kind: 'report' as const,
      })),
    ...bulletins.items
      .filter((item) => item.category === 'alert' && item.latitude !== null && item.longitude !== null)
      .map((item) => ({
        id: item.id,
        title: item.title,
        description: item.area || item.summary,
        latitude: item.latitude!,
        longitude: item.longitude!,
        kind: 'alert' as const,
      })),
  ];

  const visiblePoints = points.filter(
    (p) => filterMode === 'all' || p.kind === filterMode,
  );

  const first = visiblePoints[0];
  useEffect(() => {
    if (first) {
      camera.current?.flyTo({
        center: [first.longitude, first.latitude],
        zoom: 13,
        duration: 700,
      });
    }
  }, [first?.id, first?.longitude, first?.latitude]);

  return (
    <Gate>
      <Page scroll={false}>
        <View style={{ padding: 18, paddingBottom: 6 }}>
          <Heading
            eyebrow="Định vị & Bản đồ"
            title="Bản đồ đô thị số"
            subtitle="Theo dõi trực quan các phản ánh hiện trường và điểm cảnh báo khu vực."
          />

          {/* Map Point Filter Buttons */}
          <View style={{ flexDirection: 'row', gap: 6, marginBottom: 8 }}>
            {[
              ['all', `Tất cả (${points.length})`],
              ['report', 'Phản ánh của tôi'],
              ['alert', 'Cảnh báo'],
            ].map(([key, label]) => {
              const isSelected = filterMode === key;
              return (
                <Pressable
                  key={key}
                  onPress={() => setFilterMode(key as 'all' | 'report' | 'alert')}
                  style={{
                    paddingHorizontal: 12,
                    paddingVertical: 6,
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

          <State
            loading={reports.loading || bulletins.loading}
            error={reports.error || bulletins.error}
            empty={
              !reports.loading && !bulletins.loading && points.length === 0
                ? 'Chưa có địa điểm hoặc cảnh báo nào có tọa độ để hiển thị.'
                : undefined
            }
          />
        </View>

        {/* Map Container */}
        <View
          style={{
            flex: 1,
            margin: 14,
            marginTop: 6,
            borderRadius: 22,
            overflow: 'hidden',
            borderWidth: 1,
            borderColor: colors.border,
          }}
        >
          <Map mapStyle={MAP_STYLE} style={{ flex: 1 }}>
            <Camera
              ref={camera}
              initialViewState={{ center: [105.8542, 21.0285], zoom: 12 }}
            />
            {visiblePoints.map((point) => (
              <Marker
                key={`${point.kind}-${point.id}`}
                id={`${point.kind}-${point.id}`}
                lngLat={[point.longitude, point.latitude]}
                onPress={() => setSelected(point)}
              >
                <View
                  style={{
                    backgroundColor: point.kind === 'alert' ? colors.red : colors.primary,
                    width: 28,
                    height: 28,
                    borderRadius: 14,
                    borderWidth: 3,
                    borderColor: colors.white,
                    justifyContent: 'center',
                    alignItems: 'center',
                    shadowColor: '#000',
                    shadowOffset: { width: 0, height: 2 },
                    shadowOpacity: 0.25,
                    shadowRadius: 4,
                    elevation: 5,
                  }}
                >
                  <Ionicons
                    name={point.kind === 'alert' ? 'warning' : 'camera'}
                    size={13}
                    color={colors.white}
                  />
                </View>
              </Marker>
            ))}
          </Map>

          {/* Selected Marker Callout Drawer */}
          {selected ? (
            <View
              style={{
                position: 'absolute',
                left: 14,
                right: 14,
                bottom: 14,
              }}
            >
              <Card
                style={{
                  padding: 16,
                  marginBottom: 0,
                  shadowOpacity: 0.15,
                  shadowRadius: 16,
                  elevation: 6,
                }}
              >
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <View
                    style={{
                      flexDirection: 'row',
                      alignItems: 'center',
                      gap: 5,
                      backgroundColor: selected.kind === 'alert' ? colors.redLight : colors.primaryLight,
                      paddingHorizontal: 8,
                      paddingVertical: 3,
                      borderRadius: 999,
                    }}
                  >
                    <Ionicons
                      name={selected.kind === 'alert' ? 'warning' : 'location'}
                      size={12}
                      color={selected.kind === 'alert' ? colors.red : colors.primary}
                    />
                    <Text
                      style={{
                        color: selected.kind === 'alert' ? colors.red : colors.primary,
                        fontSize: 10.5,
                        fontWeight: '800',
                      }}
                    >
                      {selected.kind === 'alert' ? 'CẢNH BÁO KHU VỰC' : 'PHẢN ÁNH HIỆN TRƯỜNG'}
                    </Text>
                  </View>

                  <Pressable
                    onPress={() => setSelected(null)}
                    style={{ padding: 4 }}
                  >
                    <Ionicons name="close" size={18} color={colors.muted} />
                  </Pressable>
                </View>

                <Text style={styles.cardTitle}>{selected.title}</Text>
                <Text style={[styles.muted, { marginBottom: 12 }]} numberOfLines={2}>
                  {selected.description}
                </Text>

                <Button
                  title="Xem chi tiết nội dung"
                  icon="arrow-forward"
                  onPress={() =>
                    router.push(
                      selected.kind === 'alert'
                        ? { pathname: '/bulletin/[id]', params: { id: selected.id } }
                        : { pathname: '/report/[id]', params: { id: selected.id } },
                    )
                  }
                />
              </Card>
            </View>
          ) : null}
        </View>
      </Page>
    </Gate>
  );
}
