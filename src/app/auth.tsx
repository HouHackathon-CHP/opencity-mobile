import { Redirect } from 'expo-router';

// The OAuth callback is also an Android deep link handled by Expo Router.
// AuthSession processes the code; this route only keeps it out of the UI/history.
export default function AuthCallback() {
  return <Redirect href="/(tabs)/account" />;
}
