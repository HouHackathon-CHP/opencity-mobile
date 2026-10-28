import { Ionicons } from '@expo/vector-icons';
import { ReactNode } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useAuth } from './auth';

export const colors = { ink: '#143832', muted: '#617873', green: '#166a53', pale: '#e5f2ec', canvas: '#f4f8f7', white: '#fff', amber: '#d8752d', red: '#ba4547' };
export function Page({ children, scroll = true }: { children: ReactNode; scroll?: boolean }) {
  if (!scroll) return <View style={styles.page}>{children}</View>;
  return <ScrollView style={styles.page} contentContainerStyle={styles.content}>{children}</ScrollView>;
}
export function Heading({ eyebrow, title, subtitle }: { eyebrow?: string; title: string; subtitle?: string }) {
  return <View style={{ marginBottom: 20 }}>{eyebrow ? <Text style={styles.eyebrow}>{eyebrow.toUpperCase()}</Text> : null}<Text style={styles.heading}>{title}</Text>{subtitle ? <Text style={styles.muted}>{subtitle}</Text> : null}</View>;
}
export function Card({ children, onPress }: { children: ReactNode; onPress?: () => void }) {
  if (onPress) return <Pressable onPress={onPress} style={styles.card}>{children}</Pressable>;
  return <View style={styles.card}>{children}</View>;
}
export function Button({ title, onPress, icon, disabled, secondary }: { title: string; onPress: () => void; icon?: keyof typeof Ionicons.glyphMap; disabled?: boolean; secondary?: boolean }) {
  return <Pressable disabled={disabled} onPress={onPress} style={[styles.button, secondary && styles.secondary, disabled && { opacity: 0.5 }]}>{icon ? <Ionicons name={icon} size={19} color={secondary ? colors.green : colors.white} /> : null}<Text style={[styles.buttonText, secondary && { color: colors.green }]}>{title}</Text></Pressable>;
}
export function Field({ label, value, onChangeText, multiline, placeholder }: { label: string; value: string; onChangeText: (value: string) => void; multiline?: boolean; placeholder?: string }) {
  return <View style={{ marginBottom: 16 }}><Text style={styles.label}>{label}</Text><TextInput style={[styles.input, multiline && { minHeight: 108, textAlignVertical: 'top' }]} value={value} onChangeText={onChangeText} multiline={multiline} placeholder={placeholder} placeholderTextColor="#899d97" /></View>;
}
export function Gate({ children }: { children: ReactNode }) {
  const auth = useAuth();
  if (!auth.ready) return <View style={styles.center}><ActivityIndicator color={colors.green} /></View>;
  if (auth.signedIn) return <>{children}</>;
  return <Page><Heading eyebrow="Tài khoản" title="Đăng nhập để tiếp tục" subtitle="Dùng tài khoản công dân DX-OS để gửi phản ánh và xem dữ liệu của bạn." /><Button title={auth.busy ? 'Đang đăng nhập...' : 'Đăng nhập'} icon="log-in-outline" disabled={auth.busy} onPress={auth.signIn} />{auth.error ? <Text style={styles.error}>{auth.error}</Text> : null}</Page>;
}
export function State({ loading, error, empty }: { loading?: boolean; error?: string | null; empty?: string }) {
  if (loading) return <View style={styles.center}><ActivityIndicator color={colors.green} /></View>;
  if (error) return <Text style={styles.error}>{error}</Text>;
  if (empty) return <Text style={styles.muted}>{empty}</Text>;
  return null;
}
export function formatDate(value: string) { return new Date(value).toLocaleString('vi-VN'); }
export const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.canvas }, content: { padding: 20, paddingBottom: 50 },
  heading: { color: colors.ink, fontSize: 29, fontWeight: '800', lineHeight: 35, marginBottom: 7 },
  eyebrow: { color: colors.green, fontSize: 12, fontWeight: '800', letterSpacing: 1.8, marginBottom: 8 },
  muted: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  card: { backgroundColor: colors.white, borderRadius: 18, padding: 18, marginBottom: 12, borderWidth: 1, borderColor: '#e3ece8' },
  cardTitle: { color: colors.ink, fontSize: 17, fontWeight: '700', marginBottom: 5 },
  button: { backgroundColor: colors.green, minHeight: 50, paddingHorizontal: 18, borderRadius: 13, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 9, marginBottom: 12 },
  secondary: { backgroundColor: colors.pale }, buttonText: { color: colors.white, fontSize: 15, fontWeight: '700' },
  label: { color: colors.ink, fontSize: 14, fontWeight: '700', marginBottom: 7 },
  input: { backgroundColor: colors.white, borderRadius: 12, borderWidth: 1, borderColor: '#d5e1dc', padding: 13, fontSize: 16, color: colors.ink },
  error: { color: colors.red, marginTop: 12, lineHeight: 21 }, center: { padding: 30, alignItems: 'center', justifyContent: 'center' },
});
