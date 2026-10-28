import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from '@/lib/auth';

export default function RootLayout() {
  return <SafeAreaProvider><AuthProvider><StatusBar style="dark" />
    <Stack screenOptions={{ headerStyle: { backgroundColor: '#f4f8f7' }, headerTintColor: '#143832', contentStyle: { backgroundColor: '#f4f8f7' } }}>
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      <Stack.Screen name="auth" options={{ headerShown: false }} />
      <Stack.Screen name="report/new" options={{ title: 'Gửi phản ánh' }} />
      <Stack.Screen name="report/[id]" options={{ title: 'Chi tiết phản ánh' }} />
      <Stack.Screen name="bulletin/[id]" options={{ title: 'Bản tin' }} />
    </Stack>
  </AuthProvider></SafeAreaProvider>;
}
