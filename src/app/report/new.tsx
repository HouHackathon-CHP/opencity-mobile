import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import * as Location from 'expo-location';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Image, Pressable, Text, View } from 'react-native';
import { createReport, Photo } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button, Card, colors, Field, Gate, Heading, Page, styles } from '@/lib/ui';

const DRAFT_KEY = 'opencity.report-draft.v1';
const categories = ['Hạ tầng', 'Môi trường', 'Giao thông', 'Chiếu sáng', 'Cây xanh', 'Khác'];

type Draft = {
  title: string;
  description: string;
  category: string;
  address: string;
  latitude?: number;
  longitude?: number;
};

export default function NewReport() {
  const auth = useAuth();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState(categories[0]);
  const [address, setAddress] = useState('');
  const [latitude, setLatitude] = useState<number | undefined>();
  const [longitude, setLongitude] = useState<number | undefined>();
  const [photo, setPhoto] = useState<Photo | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    AsyncStorage.getItem(DRAFT_KEY)
      .then((value) => {
        if (!value) return;
        const draft = JSON.parse(value) as Draft;
        setTitle(draft.title || '');
        setDescription(draft.description || '');
        setCategory(draft.category || categories[0]);
        setAddress(draft.address || '');
        setLatitude(draft.latitude);
        setLongitude(draft.longitude);
      })
      .catch(() => {});
  }, []);

  const values = { title, description, category, address, latitude, longitude };

  const saveDraft = async () => {
    await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(values));
    setMessage('Đã lưu bản nháp trên thiết bị của bạn. Lưu ý ảnh cần đính kèm lại khi gửi.');
  };

  const choosePhoto = async (camera: boolean) => {
    if (camera) {
      const permission = await ImagePicker.requestCameraPermissionsAsync();
      if (!permission.granted) {
        setMessage('Vui lòng cấp quyền truy cập Camera trong cài đặt thiết bị.');
        return;
      }
    }
    const result = camera
      ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.8 })
      : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });

    if (!result.canceled) {
      const asset = result.assets[0];
      setPhoto({ uri: asset.uri, fileName: asset.fileName, mimeType: asset.mimeType });
      setMessage(null);
    }
  };

  const locate = async () => {
    setMessage('Đang kết nối vệ tinh GPS để lấy vị trí...');
    try {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (!permission.granted) {
        setMessage('Vui lòng cấp quyền vị trí để ứng dụng ghim tọa độ chính xác.');
        return;
      }
      let timer: ReturnType<typeof setTimeout> | undefined;
      const point = await Promise.race([
        (async () => {
          const recent = await Location.getLastKnownPositionAsync({
            maxAge: 120_000,
            requiredAccuracy: 100,
          });
          return recent ?? (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
        })(),
        new Promise<never>((_, reject) => {
          timer = setTimeout(() => reject(new Error('location_timeout')), 10_000);
        }),
      ]).finally(() => {
        if (timer) clearTimeout(timer);
      });

      setLatitude(point.coords.latitude);
      setLongitude(point.coords.longitude);
      setMessage('Đã gắn tọa độ GPS hiện tại thành công.');
    } catch {
      setMessage('Không lấy được vị trí GPS. Bạn vẫn có thể nhập địa chỉ số nhà bằng tay.');
    }
  };

  const submit = async () => {
    if (title.trim().length < 5 || description.trim().length < 15 || !address.trim()) {
      setMessage('Tiêu đề cần tối thiểu 5 ký tự, mô tả từ 15 ký tự và bắt buộc có địa chỉ.');
      return;
    }
    setBusy(true);
    setMessage(null);
    try {
      const created = await createReport(auth.accessToken, { ...values, photo });
      await AsyncStorage.removeItem(DRAFT_KEY);
      if (created.warnings?.length) {
        Alert.alert('Phản ánh đã gửi', `Lưu ý: ${created.warnings.join('; ')}`);
      }
      router.replace({ pathname: '/report/[id]', params: { id: created.id } });
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : 'Không thể gửi phản ánh tới máy chủ.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Gate>
      <Page>
        <Heading
          eyebrow="Phản ánh hiện trường"
          title="Tạo phản ánh sự cố"
          subtitle="Cung cấp thông tin chi tiết và bằng chứng hình ảnh để đơn vị chức năng thụ lý kịp thời."
        />

        {/* Section 1: Classification & Title */}
        <Card>
          <Field
            label="Tiêu đề sự việc *"
            value={title}
            onChangeText={setTitle}
            placeholder="Ví dụ: Đèn đường bị vỡ tại ngã tư Lê Lợi"
            hint="Tối thiểu 5 ký tự"
          />

          <Text style={styles.label}>Lĩnh vực liên quan *</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
            {categories.map((item) => {
              const isSelected = category === item;
              return (
                <Pressable
                  key={item}
                  onPress={() => setCategory(item)}
                  style={{
                    backgroundColor: isSelected ? colors.primary : '#f1f5f9',
                    borderRadius: 16,
                    paddingHorizontal: 12,
                    paddingVertical: 7,
                    borderWidth: 1,
                    borderColor: isSelected ? colors.primary : colors.border,
                  }}
                >
                  <Text
                    style={{
                      color: isSelected ? colors.white : colors.ink,
                      fontWeight: '700',
                      fontSize: 12,
                    }}
                  >
                    {item}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </Card>

        {/* Section 2: Details */}
        <Card>
          <Field
            label="Mô tả cụ thể sự cố *"
            value={description}
            onChangeText={setDescription}
            multiline
            placeholder="Mô tả tình trạng, mức độ ảnh hưởng đến giao thông hoặc sinh hoạt..."
            hint="Tối thiểu 15 ký tự"
          />

          <Field
            label="Địa chỉ vị trí *"
            value={address}
            onChangeText={setAddress}
            placeholder="Số nhà, đường phố, phường/xã"
          />
        </Card>

        {/* Section 3: GPS & Photo Evidence */}
        <Card>
          <Text style={styles.cardTitle}>Vị trí GPS & Hình ảnh minh chứng</Text>

          {/* GPS Status Row */}
          <View
            style={{
              flexDirection: 'row',
              alignItems: 'center',
              gap: 8,
              padding: 10,
              backgroundColor: latitude !== undefined ? colors.emeraldLight : '#f1f5f9',
              borderRadius: 12,
              marginBottom: 12,
            }}
          >
            <Ionicons
              name={latitude !== undefined ? 'location' : 'location-outline'}
              size={18}
              color={latitude !== undefined ? colors.emerald : colors.muted}
            />
            <Text
              style={{
                fontSize: 12,
                fontWeight: '600',
                color: latitude !== undefined ? '#047857' : colors.muted,
                flex: 1,
              }}
            >
              {latitude !== undefined
                ? `Đã gắn GPS: ${latitude.toFixed(5)}, ${longitude?.toFixed(5)}`
                : 'Chưa gắn tọa độ bản đồ'}
            </Text>
          </View>

          <Button
            title="Lấy vị trí GPS hiện tại"
            icon="locate-outline"
            onPress={locate}
            secondary
          />

          {/* Photos row */}
          <View style={{ flexDirection: 'row', gap: 10, marginTop: 4 }}>
            <View style={{ flex: 1 }}>
              <Button
                title="Chụp ảnh"
                icon="camera-outline"
                onPress={() => choosePhoto(true)}
                secondary
              />
            </View>
            <View style={{ flex: 1 }}>
              <Button
                title="Thư viện"
                icon="images-outline"
                onPress={() => choosePhoto(false)}
                secondary
              />
            </View>
          </View>

          {photo ? (
            <View style={{ marginTop: 10, position: 'relative' }}>
              <Image
                source={{ uri: photo.uri }}
                style={{ height: 180, borderRadius: 14, width: '100%' }}
              />
              <Pressable
                onPress={() => setPhoto(null)}
                style={{
                  position: 'absolute',
                  top: 10,
                  right: 10,
                  backgroundColor: 'rgba(15, 23, 42, 0.75)',
                  width: 32,
                  height: 32,
                  borderRadius: 16,
                  justifyContent: 'center',
                  alignItems: 'center',
                }}
              >
                <Ionicons name="trash-outline" size={17} color={colors.white} />
              </Pressable>
            </View>
          ) : null}
        </Card>

        {/* Submit & Draft buttons */}
        <View style={{ marginTop: 8 }}>
          <Button
            title={busy ? 'Đang gửi phản ánh…' : 'Xác nhận gửi phản ánh'}
            icon="send"
            disabled={busy}
            onPress={submit}
          />
          <Button
            title="Lưu bản nháp vào máy"
            icon="save-outline"
            onPress={saveDraft}
            secondary
          />
        </View>

        {message ? (
          <View
            style={{
              padding: 12,
              borderRadius: 12,
              backgroundColor: colors.amberLight,
              borderWidth: 1,
              borderColor: colors.amberBorder,
              marginTop: 6,
            }}
          >
            <Text style={{ color: '#b45309', fontSize: 13, lineHeight: 18 }}>
              {message}
            </Text>
          </View>
        ) : null}
      </Page>
    </Gate>
  );
}
