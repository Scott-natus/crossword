import React, { useEffect } from 'react';
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { useAuth } from '../contexts/AuthContext';

const LoadingScreen: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    // AuthContext에서 자동으로 인증 상태를 확인하므로
    // 여기서는 추가 작업이 필요하지 않습니다.
  }, []);

  if (isLoading) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>크로스워드 퍼즐</Text>
        <ActivityIndicator size="large" color="#007AFF" style={styles.spinner} />
        <Text style={styles.subtitle}>로딩 중...</Text>
      </View>
    );
  }

  return null; // 로딩이 완료되면 Navigation에서 자동으로 다음 화면으로 이동
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 20,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginTop: 10,
  },
  spinner: {
    marginVertical: 20,
  },
});

export default LoadingScreen;









