import { Camera, CameraRef, Map, Marker } from '@maplibre/maplibre-react-native';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Text, View } from 'react-native';
import { Bulletin, Report } from '@/lib/api';
import { MAP_STYLE } from '@/lib/config';
import { useApiList } from '@/lib/use-api-list';
import { Button, Card, colors, Gate, Heading, Page, State, styles } from '@/lib/ui';

type Point = { id: string; title: string; description: string; latitude: number; longitude: number; kind: 'report' | 'alert' };
export default function CityMap() {
  const reports = useApiList<Report>('/reports');
  const bulletins = useApiList<Bulletin>('/bulletins');
  const [selected, setSelected] = useState<Point | null>(null);
  const camera = useRef<CameraRef>(null);
  const points: Point[] = [
    ...reports.items.filter(item => item.latitude !== null && item.longitude !== null).map(item => ({ id: item.id, title: item.title, description: item.address, latitude: item.latitude!, longitude: item.longitude!, kind: 'report' as const })),
    ...bulletins.items.filter(item => item.category === 'alert' && item.latitude !== null && item.longitude !== null).map(item => ({ id: item.id, title: item.title, description: item.area || item.summary, latitude: item.latitude!, longitude: item.longitude!, kind: 'alert' as const })),
  ];
  const first = points[0];
  useEffect(() => { if (first) camera.current?.flyTo({ center: [first.longitude, first.latitude], zoom: 12, duration: 650 }); }, [first?.id, first?.longitude, first?.latitude]);
  return <Gate><Page scroll={false}><View style={{ padding: 20, paddingBottom: 0 }}><Heading eyebrow="Địa điểm" title="Bản đồ thành phố" subtitle="Phản ánh của bạn và cảnh báo có tọa độ." />
    <State loading={reports.loading || bulletins.loading} error={reports.error || bulletins.error} empty={!reports.loading && !bulletins.loading && points.length === 0 ? 'Chưa có địa điểm nào để hiển thị.' : undefined} />
  </View><View style={{ flex: 1, margin: 12, borderRadius: 18, overflow: 'hidden' }}>
    <Map mapStyle={MAP_STYLE} style={{ flex: 1 }}>
      <Camera ref={camera} initialViewState={{ center: [105.8542, 21.0285], zoom: 12 }} />
      {points.map(point => <Marker key={`${point.kind}-${point.id}`} id={`${point.kind}-${point.id}`} lngLat={[point.longitude, point.latitude]} onPress={() => setSelected(point)}><View style={{ backgroundColor: point.kind === 'alert' ? colors.red : colors.green, width: 21, height: 21, borderRadius: 11, borderWidth: 3, borderColor: colors.white }} /></Marker>)}
    </Map>
    {selected ? <View style={{ position: 'absolute', left: 12, right: 12, bottom: 12 }}><Card><Text style={{ color: selected.kind === 'alert' ? colors.red : colors.green, fontWeight: '800' }}>{selected.kind === 'alert' ? 'CẢNH BÁO' : 'PHẢN ÁNH'}</Text><Text style={styles.cardTitle}>{selected.title}</Text><Text style={[styles.muted, { marginBottom: 9 }]}>{selected.description}</Text><Button title="Xem chi tiết" onPress={() => router.push(selected.kind === 'alert' ? { pathname: '/bulletin/[id]', params: { id: selected.id } } : { pathname: '/report/[id]', params: { id: selected.id } })} /></Card></View> : null}
  </View></Page></Gate>;
}
