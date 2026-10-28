import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { colors } from '@/lib/ui';

export default function TabLayout() {
  return <Tabs screenOptions={{ headerShown: false, tabBarActiveTintColor: colors.green, tabBarInactiveTintColor: colors.muted, tabBarStyle: { height: 65, paddingTop: 7, paddingBottom: 8, backgroundColor: colors.white, borderTopColor: '#dce9e3' }, tabBarLabelStyle: { fontSize: 11, fontWeight: '700' } }}>
    <Tabs.Screen name="index" options={{ title: 'Khám phá', tabBarIcon: ({ color, size }) => <Ionicons name="grid-outline" color={String(color)} size={size} /> }} />
    <Tabs.Screen name="reports" options={{ title: 'Phản ánh', tabBarIcon: ({ color, size }) => <Ionicons name="chatbubble-ellipses-outline" color={String(color)} size={size} /> }} />
    <Tabs.Screen name="news" options={{ title: 'Tin & cảnh báo', tabBarIcon: ({ color, size }) => <Ionicons name="newspaper-outline" color={String(color)} size={size} /> }} />
    <Tabs.Screen name="map" options={{ title: 'Bản đồ', tabBarIcon: ({ color, size }) => <Ionicons name="map-outline" color={String(color)} size={size} /> }} />
    <Tabs.Screen name="account" options={{ title: 'Tài khoản', tabBarIcon: ({ color, size }) => <Ionicons name="person-circle-outline" color={String(color)} size={size} /> }} />
  </Tabs>;
}
