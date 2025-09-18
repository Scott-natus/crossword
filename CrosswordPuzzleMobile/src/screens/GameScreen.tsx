import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ScrollView,
  ActivityIndicator,
  TextInput,
} from 'react-native';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/apiService';

const GameScreen: React.FC = () => {
  const { logout, user } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const [puzzleData, setPuzzleData] = useState<any>(null);
  const [currentAnswer, setCurrentAnswer] = useState('');
  const [wrongAttempts, setWrongAttempts] = useState(0);
  const [hintsUsed, setHintsUsed] = useState(0);

  useEffect(() => {
    loadPuzzle();
  }, []);

  const loadPuzzle = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getPuzzleTemplate();
      setPuzzleData(data);
    } catch (error) {
      Alert.alert('오류', '퍼즐을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAnswerSubmit = async () => {
    if (!currentAnswer.trim()) {
      Alert.alert('오류', '답을 입력해주세요.');
      return;
    }

    try {
      const result = await apiService.checkAnswer(currentAnswer);
      if (result.correct) {
        Alert.alert('정답!', '축하합니다! 정답입니다.');
        setCurrentAnswer('');
        // 다음 단계로 진행하거나 새로운 퍼즐 로드
      } else {
        setWrongAttempts(prev => prev + 1);
        Alert.alert('틀렸습니다', '다시 시도해주세요.');
        setCurrentAnswer('');
      }
    } catch (error) {
      Alert.alert('오류', '답을 확인하는 중 오류가 발생했습니다.');
    }
  };

  const handleGetHint = async () => {
    try {
      const hint = await apiService.getHint();
      setHintsUsed(prev => prev + 1);
      Alert.alert('힌트', hint.hint);
    } catch (error) {
      Alert.alert('오류', '힌트를 가져오는데 실패했습니다.');
    }
  };

  const handleLogout = async () => {
    Alert.alert(
      '로그아웃',
      '정말 로그아웃하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        { text: '로그아웃', onPress: logout, style: 'destructive' },
      ]
    );
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.loadingText}>퍼즐을 불러오는 중...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>크로스워드 퍼즐</Text>
        <TouchableOpacity onPress={handleLogout} style={styles.logoutButton}>
          <Text style={styles.logoutText}>로그아웃</Text>
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.content}>
        <View style={styles.gameInfo}>
          <Text style={styles.infoText}>오답: {wrongAttempts}회</Text>
          <Text style={styles.infoText}>힌트 사용: {hintsUsed}회</Text>
        </View>

        <View style={styles.puzzleContainer}>
          <Text style={styles.puzzleTitle}>퍼즐 #{puzzleData?.id || '로딩 중'}</Text>
          <Text style={styles.puzzleDescription}>
            {puzzleData?.description || '퍼즐을 풀어보세요!'}
          </Text>
        </View>

        <View style={styles.answerSection}>
          <Text style={styles.answerLabel}>답을 입력하세요:</Text>
          <TextInput
            style={styles.answerInput}
            value={currentAnswer}
            onChangeText={setCurrentAnswer}
            placeholder="답을 입력하세요"
            autoCapitalize="none"
            autoCorrect={false}
          />
          <TouchableOpacity style={styles.submitButton} onPress={handleAnswerSubmit}>
            <Text style={styles.submitButtonText}>정답 확인</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.actionButtons}>
          <TouchableOpacity style={styles.hintButton} onPress={handleGetHint}>
            <Text style={styles.hintButtonText}>힌트 보기</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.newPuzzleButton} onPress={loadPuzzle}>
            <Text style={styles.newPuzzleButtonText}>새 퍼즐</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
  },
  logoutButton: {
    padding: 8,
  },
  logoutText: {
    color: '#FF3B30',
    fontSize: 16,
  },
  content: {
    flex: 1,
    padding: 20,
  },
  gameInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  infoText: {
    fontSize: 14,
    color: '#666',
  },
  puzzleContainer: {
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  puzzleTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  puzzleDescription: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
  },
  answerSection: {
    backgroundColor: 'white',
    padding: 20,
    borderRadius: 10,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  answerLabel: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  answerInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 15,
    fontSize: 16,
    marginBottom: 15,
  },
  submitButton: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
  },
  submitButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  hintButton: {
    backgroundColor: '#FF9500',
    padding: 15,
    borderRadius: 8,
    flex: 1,
    marginRight: 10,
    alignItems: 'center',
  },
  hintButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  newPuzzleButton: {
    backgroundColor: '#34C759',
    padding: 15,
    borderRadius: 8,
    flex: 1,
    marginLeft: 10,
    alignItems: 'center',
  },
  newPuzzleButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default GameScreen;









