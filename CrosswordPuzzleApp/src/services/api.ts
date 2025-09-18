import axios, { AxiosInstance, AxiosResponse } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { 
  LoginRequest, 
  RegisterRequest, 
  User, 
  CrosswordPuzzle, 
  PuzzleLevel,
  ApiResponse,
  PushSettings,
  PushToken,
  LoginResponse
} from '../types';

const API_BASE_URL = 'http://222.100.103.227:8081/K-CrossWord/api';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });

    // 요청 인터셉터 - 토큰 추가
    this.api.interceptors.request.use(
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
    this.api.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          // 토큰이 만료된 경우 로그아웃
          await AsyncStorage.removeItem('auth_token');
          await AsyncStorage.removeItem('user');
        }
        return Promise.reject(error);
      }
    );
  }

  // 인증 관련 API
  async login(credentials: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    try {
      const response: AxiosResponse = await this.api.post('/login', credentials);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async register(userData: RegisterRequest): Promise<ApiResponse<{ user: User; authorization: { token: string; type: string } }>> {
    try {
      const response: AxiosResponse = await this.api.post('/register', userData);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async logout(): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/logout');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async me(): Promise<ApiResponse<User>> {
    try {
      const response: AxiosResponse = await this.api.get('/me');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 퍼즐 생성
  async generatePuzzle(level: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post(`/puzzles/generate/${level}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 퍼즐 레벨 목록 조회
  async getPuzzleLevels(): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get('/puzzles/levels');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 단어 조회
  async getWords(params?: { page?: number; size?: number; difficulty?: number; category?: string }): Promise<ApiResponse<any>> {
    try {
      const queryParams = new URLSearchParams();
      if (params?.page) queryParams.append('page', params.page.toString());
      if (params?.size) queryParams.append('size', params.size.toString());
      if (params?.difficulty) queryParams.append('difficulty', params.difficulty.toString());
      if (params?.category) queryParams.append('category', params.category);
      
      const response: AxiosResponse = await this.api.get(`/words?${queryParams.toString()}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 힌트 조회
  async getHintsByWordId(wordId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get(`/hints/word/${wordId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 게임 세션 생성
  async createGameSession(userId: number, wordId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/game/sessions', null, {
        params: { userId, wordId }
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 게임 세션 조회
  async getGameSession(sessionId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get(`/game/sessions/${sessionId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 게임 세션 완료
  async completeGameSession(sessionId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.put(`/game/sessions/${sessionId}/complete`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 정답 추가
  async addCorrectAnswer(sessionId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.put(`/game/sessions/${sessionId}/correct-answer`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 오답 추가
  async addWrongAnswer(sessionId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.put(`/game/sessions/${sessionId}/wrong-answer`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 힌트 사용 추가
  async addHintUsed(sessionId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.put(`/game/sessions/${sessionId}/hint-used`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 플레이 시간 업데이트
  async updatePlayTime(sessionId: number, playTimeSeconds: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.put(`/game/sessions/${sessionId}/play-time`, null, {
        params: { playTimeSeconds }
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 사용자 게임 통계
  async getUserGameStats(userId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get(`/game/stats/user/${userId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 힌트 생성
  async generateHintForWord(wordId: number, hintType: string): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post(`/hint-generation/word/${wordId}`, null, {
        params: { hintType }
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Spring Boot API - 힌트 생성 상태 확인
  async getHintGenerationStatus(): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get('/hint-generation/status');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // React 앱용 퍼즐게임 API (기존 호환성 유지)
  async getPuzzleTemplate(): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get('/puzzle/template');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async checkPuzzleAnswer(data: { word_id: number; answer: string }): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/submit-answer', data);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getPuzzleHints(data: { word_id: number; current_hint_id?: number | null; base_hint_id?: number | null }): Promise<ApiResponse<any>> {
    try {
      const params = new URLSearchParams();
      params.append('word_id', data.word_id.toString());
      if (data.current_hint_id) {
        params.append('current_hint_id', data.current_hint_id.toString());
      }
      if (data.base_hint_id) {
        params.append('base_hint_id', data.base_hint_id.toString());
      }
      
      const response: AxiosResponse = await this.api.get(`/puzzle/hints?${params.toString()}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async generatePuzzle(levelId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/generate', { level_id: levelId });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async checkCompletion(sessionToken: string): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/check-completion', { session_token: sessionToken });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async saveGameState(sessionToken: string, gameState: any): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/save-game-state', { 
        session_token: sessionToken, 
        game_state: gameState 
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async submitResult(data: {
    session_token: string;
    score: number;
    accuracy: number;
    hints_used: number;
    time_used: number;
  }): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/submit-result', data);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async completeLevel(): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/complete-level');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // 기존 퍼즐 게임 관련 API (호환성 유지)
  async getPuzzle(levelId: number): Promise<ApiResponse<CrosswordPuzzle>> {
    try {
      const response: AxiosResponse = await this.api.get(`/crossword/puzzle/${levelId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // PC 웹과 동일한 퍼즐게임 API (기존)
  async getPuzzleTemplateOld(): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get('/puzzle-game/template');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async checkPuzzleAnswerOld(data: { word_id: number; answer: string }): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle-game/check-answer', data);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getPuzzleHintsOld(wordId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get(`/puzzle-game/hints?word_id=${wordId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async completePuzzleLevel(levelId: number): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle-game/complete-level', { level_id: levelId });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async submitAnswer(data: { levelId: number; word: string; answer: string }): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/crossword/submit-answer', data);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getTemplate(): Promise<ApiResponse<any>> {
    try {
      const response: AxiosResponse = await this.api.get('/puzzle-game/template');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async checkAnswer(data: { word: string; answer: string }): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle-game/check-answer', data);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getHints(wordId: number): Promise<ApiResponse<any[]>> {
    try {
      const response: AxiosResponse = await this.api.get(`/puzzle-game/hints?word_id=${wordId}`);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async completeLevelOld(levelId: number): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle-game/complete-level', { level_id: levelId });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // 레벨 관련 API
  async getLevels(): Promise<ApiResponse<PuzzleLevel[]>> {
    try {
      const response: AxiosResponse = await this.api.get('/puzzle/levels');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // 힌트 생성 API
  async generateHints(words: string[]): Promise<ApiResponse<Record<string, string[]>>> {
    try {
      const response: AxiosResponse = await this.api.post('/puzzle/generate-hints', { words });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // 모바일 푸시 알림 관련 API
  async registerPushToken(token: string, platform: 'ios' | 'android'): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.post('/mobile/push-token', {
        token,
        platform,
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async unregisterPushToken(token: string): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.delete('/mobile/push-token', {
        data: { token },
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getPushSettings(): Promise<ApiResponse<PushSettings>> {
    try {
      const response: AxiosResponse = await this.api.get('/mobile/push-settings');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async updatePushSettings(settings: PushSettings): Promise<ApiResponse> {
    try {
      const response: AxiosResponse = await this.api.put('/mobile/push-settings', settings);
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // 사용자 통계 관련 API
  async getUserStats(): Promise<ApiResponse<{
    current_level: number;
    total_games: number;
    total_score: number;
    last_played: string | null;
    total_play_time: number;
    average_accuracy: number;
    total_hints_used: number;
  }>> {
    try {
      const response: AxiosResponse = await this.api.get('/user/stats');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getRecentGames(): Promise<ApiResponse<{
    id: number;
    level: number;
    score: number;
    completed_at: string;
    created_at: string;
  }[]>> {
    try {
      const response: AxiosResponse = await this.api.get('/user/recent-games');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  async getRecentGameSessions(): Promise<ApiResponse<{
    id: number;
    word_id: number;
    word: string;
    session_started_at: string;
    session_ended_at: string | null;
    total_play_time: number;
    accuracy_rate: number;
    total_correct_answers: number;
    total_wrong_answers: number;
    hints_used_count: number;
    is_completed: boolean;
    created_at: string;
  }[]>> {
    try {
      const response: AxiosResponse = await this.api.get('/user/recent-game-sessions');
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  private handleError(error: any): Error {
    if (error.response) {
      // 서버 응답이 있는 경우
      const message = error.response.data?.message || error.response.data?.error || '서버 오류가 발생했습니다.';
      return new Error(message);
    } else if (error.request) {
      // 요청은 보냈지만 응답을 받지 못한 경우
      return new Error('네트워크 연결을 확인해주세요.');
    } else {
      // 요청 자체를 보내지 못한 경우
      return new Error('알 수 없는 오류가 발생했습니다.');
    }
  }
}

export const apiService = new ApiService(); 