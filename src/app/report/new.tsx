import AsyncStorage from '@react-native-async-storage/async-storage';
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
type Draft = { title: string; description: string; category: string; address: string; latitude?: number; longitude?: number };
export default function NewReport() {
  const auth = useAuth();
  const [title, setTitle] = useState(''); const [description, setDescription] = useState('');
  const [category, setCategory] = useState(categories[0]); const [address, setAddress] = useState('');
  const [latitude, setLatitude] = useState<number | undefined>(); const [longitude, setLongitude] = useState<number | undefined>();
  const [photo, setPhoto] = useState<Photo | null>(null);
  const [busy, setBusy] = useState(false); const [message, setMessage] = useState<string | null>(null);
  useEffect(() => { AsyncStorage.getItem(DRAFT_KEY).then(value => { if (!value) return; const draft = JSON.parse(value) as Draft; setTitle(draft.title); setDescription(draft.description); setCategory(draft.category); setAddress(draft.address); setLatitude(draft.latitude); setLongitude(draft.longitude); }).catch(() => {}); }, []);
  const values = { title, description, category, address, latitude, longitude };
  const saveDraft = async () => { await AsyncStorage.setItem(DRAFT_KEY, JSON.stringify(values)); setMessage('Đã lưu bản nháp trên thiết bị. Ảnh cần chọn lại khi gửi.'); };
  const choosePhoto = async (camera: boolean) => {
    if (camera) { const permission = await ImagePicker.requestCameraPermissionsAsync(); if (!permission.granted) { setMessage('Hãy cấp quyền camera để chụp ảnh.'); return; } }
    const result = camera ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.8 }) : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 0.8 });
    if (!result.canceled) { const asset = result.assets[0]; setPhoto({ uri: asset.uri, fileName: asset.fileName, mimeType: asset.mimeType }); }
  };
  const locate = async () => {
    try {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (!permission.granted) { setMessage('Hãy cấp quyền vị trí nếu muốn ghim điểm trên bản đồ.'); return; }
      const point = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      setLatitude(point.coords.latitude); setLongitude(point.coords.longitude);
      setMessage('Đã gắn vị trí hiện tại.');
    } catch { setMessage('Không lấy được vị trí. Bạn vẫn có thể nhập địa chỉ và gửi.'); }
  };
  const submit = async () => {
    if (title.trim().length < 5 || description.trim().length < 15 || !address.trim()) { setMessage('Tiêu đề tối thiểu 5 ký tự, nội dung 15 ký tự và cần địa chỉ.'); return; }
    setBusy(true); setMessage(null);
    try {
      const created = await createReport(auth.accessToken, { ...values, photo });
      await AsyncStorage.removeItem(DRAFT_KEY);
      if (created.warnings?.length) Alert.alert('Phản ánh đã gửi', `Ảnh chưa lưu được: ${created.warnings.join('; ')}`);
      router.replace({ pathname: '/report/[id]', params: { id: created.id } });
    } catch (cause) { setMessage(cause instanceof Error ? cause.message : 'Không thể gửi phản ánh.'); }
    finally { setBusy(false); }
  };
  return <Gate><Page><Heading eyebrow="Phản ánh hiện trường" title="Gửi phản ánh" subtitle="Mô tả vấn đề rõ ràng để đơn vị xử lý có đủ thông tin." />
    <Field label="Tiêu đề" value={title} onChangeText={setTitle} placeholder="Ví dụ: Đèn đường không sáng" />
    <Text style={styles.label}>Nhóm vấn đề</Text><View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginBottom: 16 }}>{categories.map(item => <Pressable key={item} onPress={() => setCategory(item)} style={{ backgroundColor: category === item ? colors.green : colors.white, borderRadius: 18, paddingHorizontal: 13, paddingVertical: 9 }}><Text style={{ color: category === item ? colors.white : colors.ink, fontWeight: '700' }}>{item}</Text></Pressable>)}</View>
    <Field label="Mô tả" value={description} onChangeText={setDescription} multiline placeholder="Vấn đề xuất hiện khi nào, mức độ ảnh hưởng..." />
    <Field label="Địa chỉ" value={address} onChangeText={setAddress} placeholder="Số nhà, đường, phường/xã" />
    <Card><Text style={styles.cardTitle}>Vị trí & ảnh</Text><Text style={[styles.muted, { marginBottom: 13 }]}>{latitude === undefined ? 'Chưa gắn tọa độ' : `Đã gắn tọa độ ${latitude.toFixed(5)}, ${longitude?.toFixed(5)}`}</Text>
      <Button title="Lấy vị trí hiện tại" icon="locate-outline" onPress={locate} secondary />
      <Button title="Chọn ảnh" icon="image-outline" onPress={() => choosePhoto(false)} secondary />
      <Button title="Chụp ảnh" icon="camera-outline" onPress={() => choosePhoto(true)} secondary />
      {photo ? <Image source={{ uri: photo.uri }} style={{ height: 160, borderRadius: 12 }} /> : null}
    </Card>
    <Button title={busy ? 'Đang gửi...' : 'Gửi phản ánh'} icon="send-outline" disabled={busy} onPress={submit} />
    <Button title="Lưu bản nháp" icon="save-outline" onPress={saveDraft} secondary />
    {message ? <Text style={{ color: colors.amber, lineHeight: 21 }}>{message}</Text> : null}
  </Page></Gate>;
}
