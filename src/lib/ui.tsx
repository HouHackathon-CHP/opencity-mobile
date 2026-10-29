import { Ionicons } from '@expo/vector-icons';
import { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useAuth } from './auth';
import { LogoIcon } from '@/components/Logo';
import { colors } from './theme';
export { colors } from './theme';

export function Page({
  children,
  scroll = true,
}: {
  children: ReactNode;
  scroll?: boolean;
}) {
  if (!scroll) return <View style={styles.page}>{children}</View>;
  return (
    <ScrollView
      style={styles.page}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  );
}

export function Heading({
  eyebrow,
  title,
  subtitle,
}: {
  eyebrow?: string;
  title: string;
  subtitle?: string;
}) {
  return (
    <View style={{ marginBottom: 20 }}>
      {eyebrow ? <Text style={styles.eyebrow}>{eyebrow.toUpperCase()}</Text> : null}
      <Text style={styles.heading}>{title}</Text>
      {subtitle ? <Text style={styles.muted}>{subtitle}</Text> : null}
    </View>
  );
}

export function Card({
  children,
  onPress,
  style,
}: {
  children: ReactNode;
  onPress?: () => void;
  style?: object;
}) {
  if (onPress) {
    return (
      <Pressable
        onPress={onPress}
        style={({ pressed }) => [
          styles.card,
          style,
          pressed && { opacity: 0.88, transform: [{ scale: 0.99 }] },
        ]}
      >
        {children}
      </Pressable>
    );
  }
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Button({
  title,
  onPress,
  icon,
  disabled,
  secondary,
  danger,
}: {
  title: string;
  onPress: () => void;
  icon?: keyof typeof Ionicons.glyphMap;
  disabled?: boolean;
  secondary?: boolean;
  danger?: boolean;
}) {
  const iconColor = danger
    ? colors.red
    : secondary
    ? colors.primary
    : colors.white;

  return (
    <Pressable
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        secondary && styles.secondary,
        danger && styles.dangerButton,
        disabled && { opacity: 0.5 },
        pressed && !disabled && { opacity: 0.88, transform: [{ scale: 0.98 }] },
      ]}
    >
      {icon ? <Ionicons name={icon} size={19} color={iconColor} /> : null}
      <Text
        style={[
          styles.buttonText,
          secondary && { color: colors.primary },
          danger && { color: colors.red },
        ]}
      >
        {title}
      </Text>
    </Pressable>
  );
}

export function Badge({
  label,
  tone = 'default',
}: {
  label: string;
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info';
}) {
  let bg = '#f1f5f9';
  let text = colors.muted;

  if (tone === 'success') {
    bg = colors.emeraldLight;
    text = '#047857';
  } else if (tone === 'warning') {
    bg = colors.amberLight;
    text = '#b45309';
  } else if (tone === 'danger') {
    bg = colors.redLight;
    text = '#b91c1c';
  } else if (tone === 'info') {
    bg = colors.primaryLight;
    text = colors.primary;
  }

  return (
    <View style={[styles.badge, { backgroundColor: bg }]}>
      <Text style={[styles.badgeText, { color: text }]}>{label}</Text>
    </View>
  );
}

export function Field({
  label,
  value,
  onChangeText,
  multiline,
  placeholder,
  hint,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  multiline?: boolean;
  placeholder?: string;
  hint?: string;
}) {
  return (
    <View style={{ marginBottom: 16 }}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={[styles.input, multiline && { minHeight: 110, textAlignVertical: 'top' }]}
        value={value}
        onChangeText={onChangeText}
        multiline={multiline}
        placeholder={placeholder}
        placeholderTextColor="#94a3b8"
      />
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
    </View>
  );
}

export function Gate({ children }: { children: ReactNode }) {
  const auth = useAuth();
  if (!auth.ready) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    );
  }
  if (auth.signedIn) return <>{children}</>;

  return (
    <Page>
      <View style={styles.gateCard}>
        <View style={{ marginBottom: 16 }}>
          <LogoIcon size={58} />
        </View>
        <Heading
          eyebrow="Tài khoản công dân"
          title="Đăng nhập để tiếp tục"
          subtitle="Dùng tài khoản công dân DX-OS để gửi phản ánh hiện trường, theo dõi kết quả xử lý và tham gia khảo sát cộng đồng."
        />
        <Button
          title={auth.busy ? 'Đang kết nối Keycloak…' : 'Đăng nhập với DX-OS'}
          icon="log-in-outline"
          disabled={auth.busy}
          onPress={auth.signIn}
        />
        {auth.error ? <Text style={styles.error}>{auth.error}</Text> : null}
      </View>
    </Page>
  );
}

export function State({
  loading,
  error,
  empty,
}: {
  loading?: boolean;
  error?: string | null;
  empty?: string;
}) {
  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    );
  }
  if (error) {
    return (
      <View style={styles.stateErrorCard}>
        <Ionicons name="alert-circle-outline" size={24} color={colors.red} />
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }
  if (empty) {
    return (
      <View style={styles.stateEmptyCard}>
        <Ionicons name="file-tray-outline" size={32} color={colors.lightMuted} />
        <Text style={styles.emptyText}>{empty}</Text>
      </View>
    );
  }
  return null;
}

export function formatDate(value: string) {
  try {
    return new Date(value).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return value;
  }
}

export const styles = StyleSheet.create({
  page: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  content: {
    padding: 18,
    paddingBottom: 60,
  },
  heading: {
    color: colors.ink,
    fontSize: 27,
    fontWeight: '800',
    lineHeight: 34,
    letterSpacing: -0.5,
    marginBottom: 6,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1.8,
    marginBottom: 6,
  },
  muted: {
    color: colors.muted,
    fontSize: 13.5,
    lineHeight: 20,
  },
  card: {
    backgroundColor: colors.white,
    borderRadius: 20,
    padding: 18,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: colors.border,
    shadowColor: '#0f172a',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.04,
    shadowRadius: 8,
    elevation: 2,
  },
  cardTitle: {
    color: colors.ink,
    fontSize: 16.5,
    fontWeight: '700',
    marginBottom: 5,
    lineHeight: 22,
  },
  button: {
    backgroundColor: colors.primary,
    minHeight: 50,
    paddingHorizontal: 20,
    borderRadius: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginBottom: 12,
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 3,
  },
  secondary: {
    backgroundColor: colors.pale,
    borderWidth: 1,
    borderColor: colors.primaryBorder,
    shadowOpacity: 0,
    elevation: 0,
  },
  dangerButton: {
    backgroundColor: colors.redLight,
    borderWidth: 1,
    borderColor: colors.redBorder,
    shadowOpacity: 0,
    elevation: 0,
  },
  buttonText: {
    color: colors.white,
    fontSize: 14.5,
    fontWeight: '700',
  },
  label: {
    color: colors.ink,
    fontSize: 13.5,
    fontWeight: '700',
    marginBottom: 7,
  },
  input: {
    backgroundColor: colors.white,
    borderRadius: 13,
    borderWidth: 1.5,
    borderColor: colors.border,
    padding: 13,
    fontSize: 15,
    color: colors.ink,
  },
  hint: {
    fontSize: 11.5,
    color: colors.lightMuted,
    marginTop: 5,
  },
  badge: {
    paddingHorizontal: 9,
    paddingVertical: 4,
    borderRadius: 999,
    alignSelf: 'flex-start',
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
  gateCard: {
    backgroundColor: colors.white,
    borderRadius: 24,
    padding: 24,
    borderWidth: 1,
    borderColor: colors.border,
    marginTop: 20,
    alignItems: 'center',
    textAlign: 'center',
  },
  gateIconWrap: {
    width: 64,
    height: 64,
    borderRadius: 20,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  error: {
    color: colors.red,
    marginTop: 12,
    lineHeight: 20,
    fontSize: 13,
  },
  center: {
    padding: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stateErrorCard: {
    backgroundColor: colors.redLight,
    borderWidth: 1,
    borderColor: colors.redBorder,
    borderRadius: 16,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginVertical: 12,
  },
  errorText: {
    color: '#b91c1c',
    fontSize: 13.5,
    flex: 1,
  },
  stateEmptyCard: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 18,
    padding: 32,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    marginVertical: 12,
  },
  emptyText: {
    color: colors.muted,
    fontSize: 13.5,
    textAlign: 'center',
  },
});
