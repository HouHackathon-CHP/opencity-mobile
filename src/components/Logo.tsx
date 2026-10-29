import { Image } from 'expo-image';
import { StyleSheet, Text, View } from 'react-native';
import { colors } from '@/lib/theme';

export function getLogoSvg(size: number = 64) {
  return `<svg width="${size}" height="${size}" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="oc_nexus_ring" x1="8" y1="8" x2="56" y2="56" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#2563EB" />
        <stop offset="50%" stop-color="#06B6D4" />
        <stop offset="100%" stop-color="#10B981" />
      </linearGradient>
      <linearGradient id="oc_nexus_core" x1="20" y1="16" x2="44" y2="48" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#38BDF8" />
        <stop offset="100%" stop-color="#2563EB" />
      </linearGradient>
    </defs>
    <circle
      cx="32"
      cy="32"
      r="24"
      stroke="url(#oc_nexus_ring)"
      stroke-width="5.5"
      stroke-linecap="round"
      stroke-dasharray="125 25"
      transform="rotate(-30 32 32)"
    />
    <path
      d="M32 17L41 26V43C41 44.1 40.1 45 39 45H25C23.9 45 23 44.1 23 43V26L32 17Z"
      fill="url(#oc_nexus_core)"
    />
    <path
      d="M28 45V34C28 31.8 29.8 30 32 30C34.2 30 36 31.8 36 34V45"
      fill="#0B132B"
      opacity="0.95"
    />
    <circle cx="32" cy="24" r="2.5" fill="#34D399" />
  </svg>`;
}

export function LogoIcon({ size = 36 }: { size?: number }) {
  const svg = getLogoSvg(size);
  const dataUri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;

  return (
    <Image
      source={{ uri: dataUri }}
      style={{ width: size, height: size }}
      contentFit="contain"
    />
  );
}

export function Logo({
  size = 36,
  showSubtitle = true,
  variant = 'default',
}: {
  size?: number;
  showSubtitle?: boolean;
  variant?: 'default' | 'white';
}) {
  const isWhite = variant === 'white';
  const mainColor = isWhite ? colors.white : colors.ink;
  const subColor = isWhite ? '#94a3b8' : colors.muted;

  return (
    <View style={localStyles.row}>
      <LogoIcon size={size} />
      <View style={localStyles.textCol}>
        <Text style={[localStyles.title, { fontSize: size * 0.58, color: mainColor }]}>
          Open<Text style={{ color: colors.primary }}>City</Text>
        </Text>
        {showSubtitle ? (
          <Text style={[localStyles.subtitle, { fontSize: Math.max(9, size * 0.26), color: subColor }]}>
            CỔNG ĐÔ THỊ SỐ
          </Text>
        ) : null}
      </View>
    </View>
  );
}

const localStyles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  textCol: {
    justifyContent: 'center',
  },
  title: {
    fontWeight: '800',
    letterSpacing: -0.5,
    lineHeight: 22,
  },
  subtitle: {
    fontWeight: '700',
    letterSpacing: 1.2,
    marginTop: 2,
  },
});
