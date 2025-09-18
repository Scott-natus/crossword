import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const API_BASE_URL = 'http://222.100.103.227:8080/api';

// axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터 - 토큰 자동 추가
api.interceptors.request.use(
  async (config) => {
    const token = await AsyncStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터 - 에러 처리
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 토큰이 만료되었거나 유효하지 않음
      await AsyncStorage.removeItem('auth_token');
    }
    return Promise.reject(error);
  }
);

export const apiService = {
  // 인증 관련
  async login(email: string, password: string) {
    try {
      const response = await api.post('/login', { email, password });
      return {
        success: true,
        token: response.data.token,
        user: response.data.user,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '로그인에 실패했습니다.',
      };
    }
  },

  async register(email: string, password: string, passwordConfirmation: string) {
    try {
      const response = await api.post('/register', {
        email,
        password,
        password_confirmation: passwordConfirmation,
      });
      return {
        success: true,
        token: response.data.token,
        user: response.data.user,
      };
    } catch (error: any) {
      return {
        success: false,
        message: error.response?.data?.message || '회원가입에 실패했습니다.',
      };
    }
  },

  async getUserInfo() {
    try {
      const response = await api.get('/user');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  // 퍼즐 게임 관련
  async getPuzzleTemplate() {
    try {
      const response = await api.get('/puzzle/template');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  async checkAnswer(answer: string) {
    try {
      const response = await api.post('/puzzle/check-answer', { answer });
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  async completeLevel() {
    try {
      const response = await api.post('/puzzle/complete-level');
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  async getHint() {
    try {
      const response = await api.post('/puzzle/hint');
      return response.data;
    } catch (error) {
      throw error;
    }
  },
};









