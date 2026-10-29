import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { Platform } from 'react-native';
import { colors } from '@/lib/ui';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.muted,
        tabBarStyle: {
          height: Platform.OS === 'ios' ? 86 : 68,
          paddingTop: 8,
          paddingBottom: Platform.OS === 'ios' ? 28 : 10,
          backgroundColor: colors.white,
          borderTopColor: colors.border,
          borderTopWidth: 1,
          elevation: 8,
          shadowColor: '#0f172a',
          shadowOffset: { width: 0, height: -4 },
          shadowOpacity: 0.04,
          shadowRadius: 10,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '700',
          marginTop: 2,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Khám phá',
          tabBarIcon: ({ color, focused, size }) => (
            <Ionicons
              name={focused ? 'grid' : 'grid-outline'}
              color={String(color)}
              size={size || 22}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="reports"
        options={{
          title: 'Phản ánh',
          tabBarIcon: ({ color, focused, size }) => (
            <Ionicons
              name={focused ? 'chatbubbles' : 'chatbubbles-outline'}
              color={String(color)}
              size={size || 22}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="news"
        options={{
          title: 'Tin tức',
          tabBarIcon: ({ color, focused, size }) => (
            <Ionicons
              name={focused ? 'newspaper' : 'newspaper-outline'}
              color={String(color)}
              size={size || 22}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="map"
        options={{
          title: 'Bản đồ',
          tabBarIcon: ({ color, focused, size }) => (
            <Ionicons
              name={focused ? 'map' : 'map-outline'}
              color={String(color)}
              size={size || 22}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="account"
        options={{
          title: 'Tài khoản',
          tabBarIcon: ({ color, focused, size }) => (
            <Ionicons
              name={focused ? 'person-circle' : 'person-circle-outline'}
              color={String(color)}
              size={size || 22}
            />
          ),
        }}
      />
    </Tabs>
  );
}
