import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from '@/lib/auth';
import { colors } from '@/lib/ui';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <StatusBar style="dark" />
        <Stack
          screenOptions={{
            headerStyle: { backgroundColor: colors.white },
            headerTintColor: colors.ink,
            headerTitleStyle: { fontWeight: '800', fontSize: 17 },
            headerShadowVisible: false,
            contentStyle: { backgroundColor: colors.canvas },
          }}
        >
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen name="auth" options={{ headerShown: false }} />
          <Stack.Screen name="report/new" options={{ title: 'Gửi phản ánh hiện trường' }} />
          <Stack.Screen name="report/[id]" options={{ title: 'Chi tiết phản ánh' }} />
          <Stack.Screen name="bulletin/[id]" options={{ title: 'Chi tiết bản tin' }} />
          <Stack.Screen name="surveys/index" options={{ title: 'Khảo sát ý kiến' }} />
          <Stack.Screen name="surveys/[id]" options={{ title: 'Biểu quyết khảo sát' }} />
        </Stack>
      </AuthProvider>
    </SafeAreaProvider>
  );
}
