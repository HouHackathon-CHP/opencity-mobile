import { Ionicons } from '@expo/vector-icons';
import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { api, SurveyDetail } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, formatDate, Gate, Heading, Page, State, styles } from '@/lib/ui';

export default function SurveyDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const auth = useAuth();
  const [survey, setSurvey] = useState<SurveyDetail | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.signedIn || !id) return;
    let active = true;
    setLoading(true);
    setError(null);
    api<SurveyDetail>(`/surveys/${encodeURIComponent(id)}`, auth.accessToken)
      .then((value) => {
        if (active) setSurvey(value);
      })
      .catch((cause) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Không tải được nội dung khảo sát.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [auth.signedIn, auth.accessToken, id]);

  const answer = async () => {
    if (!id || !selected || busy || !survey || survey.status !== 'open' || survey.my_option_id) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const updated = await api<SurveyDetail>(
        `/surveys/${encodeURIComponent(id)}/answers`,
        auth.accessToken,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ option_id: selected }),
        },
      );
      setSurvey(updated);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể gửi ý kiến biểu quyết.');
    } finally {
      setBusy(false);
    }
  };

  const results = survey?.total_responses !== null && survey?.total_responses !== undefined;

  return (
    <Gate>
      <Page>
        <State loading={loading} error={!survey ? error : null} />
        {survey ? (
          <>
            {/* Header info */}
            <View
              style={{
                backgroundColor: '#0f172a',
                borderRadius: 22,
                padding: 20,
                marginBottom: 16,
              }}
            >
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <Text style={{ color: '#93c5fd', fontSize: 11, fontWeight: '800', letterSpacing: 1.2 }}>
                  {survey.status === 'closed' ? 'PHIÊN ĐÃ ĐÓNG' : 'ĐANG MỞ LẤY Ý KIẾN'}
                </Text>

                <View
                  style={{
                    backgroundColor: survey.status === 'closed' ? '#334155' : colors.primary,
                    paddingHorizontal: 9,
                    paddingVertical: 3,
                    borderRadius: 999,
                  }}
                >
                  <Text style={{ color: colors.white, fontSize: 11, fontWeight: '800' }}>
                    {survey.status === 'closed'
                      ? 'Đã kết thúc'
                      : survey.my_option_id
                      ? 'Đã biểu quyết'
                      : 'Đang tiếp nhận'}
                  </Text>
                </View>
              </View>

              <Text style={{ color: colors.white, fontSize: 20, fontWeight: '800', lineHeight: 26, marginBottom: 8 }}>
                {survey.title}
              </Text>

              <Text style={{ color: '#94a3b8', fontSize: 12 }}>
                Công bố ngày: {formatDate(survey.created_at)}
              </Text>
            </View>

            {/* Description Card */}
            <Card>
              <Text style={[styles.label, { color: colors.primary }]}>Mục đích & Phạm vi khảo sát</Text>
              <Text style={[styles.muted, { fontSize: 14.5, lineHeight: 22, color: colors.ink }]}>
                {survey.description}
              </Text>
            </Card>

            {/* Options Title */}
            <View style={{ marginVertical: 8 }}>
              <Text style={{ fontSize: 15, fontWeight: '800', color: colors.ink }}>
                {results ? 'Kết quả biểu quyết tổng hợp' : 'Chọn một phương án để bình chọn:'}
              </Text>
            </View>

            {/* Options List */}
            {survey.options.map((option) => {
              const chosen =
                survey.my_option_id === option.id || (!survey.my_option_id && selected === option.id);
              const count = option.votes ?? 0;
              const percent =
                results && survey.total_responses
                  ? Math.round((count * 100) / survey.total_responses)
                  : 0;

              return (
                <Pressable
                  key={option.id}
                  accessibilityRole="radio"
                  accessibilityState={{ selected: chosen, disabled: !!results }}
                  disabled={!!results || survey.status === 'closed'}
                  onPress={() => setSelected(option.id)}
                  style={[
                    styles.card,
                    {
                      borderColor: chosen ? colors.primary : colors.border,
                      borderWidth: chosen ? 1.5 : 1,
                      backgroundColor: chosen ? colors.pale : colors.white,
                    },
                  ]}
                >
                  <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
                    <View
                      style={{
                        width: 22,
                        height: 22,
                        borderRadius: 11,
                        borderWidth: 2,
                        borderColor: chosen ? colors.primary : colors.borderDark,
                        backgroundColor: chosen ? colors.primary : colors.white,
                        justifyContent: 'center',
                        alignItems: 'center',
                      }}
                    >
                      {chosen && <Ionicons name="checkmark" size={14} color={colors.white} />}
                    </View>

                    <Text
                      style={[
                        styles.cardTitle,
                        {
                          flex: 1,
                          marginBottom: 0,
                          color: chosen ? colors.primary : colors.ink,
                        },
                      ]}
                    >
                      {option.label}
                    </Text>

                    {survey.my_option_id === option.id && (
                      <View
                        style={{
                          backgroundColor: colors.emeraldLight,
                          paddingHorizontal: 7,
                          paddingVertical: 2,
                          borderRadius: 999,
                        }}
                      >
                        <Text style={{ color: '#047857', fontSize: 10.5, fontWeight: '800' }}>
                          Ý kiến của bạn
                        </Text>
                      </View>
                    )}
                  </View>

                  {/* Results Progress Bar */}
                  {results ? (
                    <View style={{ marginTop: 12 }}>
                      <View
                        style={{
                          height: 7,
                          backgroundColor: '#e2e8f0',
                          borderRadius: 4,
                          overflow: 'hidden',
                        }}
                      >
                        <View
                          style={{
                            width: `${percent}%`,
                            height: 7,
                            backgroundColor: colors.primary,
                            borderRadius: 4,
                          }}
                        />
                      </View>
                      <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 6 }}>
                        <Text style={{ fontSize: 12, color: colors.muted }}>
                          {count} phiếu bình chọn
                        </Text>
                        <Text style={{ fontSize: 12, fontWeight: '700', color: colors.primary }}>
                          {percent}%
                        </Text>
                      </View>
                    </View>
                  ) : null}
                </Pressable>
              );
            })}

            {/* Submit Vote Button */}
            {!results && survey.status === 'open' ? (
              <View style={{ marginTop: 12 }}>
                <Button
                  title={busy ? 'Đang gửi ý kiến…' : 'Xác nhận biểu quyết'}
                  icon="send"
                  disabled={!selected || busy}
                  onPress={answer}
                />
              </View>
            ) : null}

            {/* Results Total Banner */}
            {results ? (
              <View
                style={{
                  padding: 14,
                  backgroundColor: colors.white,
                  borderWidth: 1,
                  borderColor: colors.border,
                  borderRadius: 16,
                  flexDirection: 'row',
                  alignItems: 'center',
                  gap: 8,
                  marginTop: 6,
                }}
              >
                <Ionicons name="people" size={18} color={colors.primary} />
                <Text style={{ fontSize: 12.5, color: colors.muted }}>
                  Tổng cộng có <Text style={{ fontWeight: '800', color: colors.ink }}>{survey.total_responses}</Text> lượt ý kiến ghi nhận trong đơn vị.
                </Text>
              </View>
            ) : null}

            {survey.status === 'closed' && !results ? (
              <Text style={[styles.muted, { textAlign: 'center', marginTop: 12 }]}>
                Phiên khảo sát này đã kết thúc.
              </Text>
            ) : null}

            {error ? <Text style={styles.error}>{error}</Text> : null}
          </>
        ) : null}
      </Page>
    </Gate>
  );
}
