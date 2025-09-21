<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\UserPuzzleGame;
use App\Models\PuzzleGridTemplates;
use App\Models\PuzzleLevel;
use App\Models\PzWord;
use App\Http\Controllers\GridTemplateController;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Auth;

class PuzzleGameController extends Controller
{
    public function index(Request $request)
    {
        $user = Auth::user();
        $guestId = $request->query('guest_id') ?? session('guest_id');

        if (!$user && !$guestId) {
            // 로그인/게스트 ID 모두 없으면 안내
            return view('puzzle.game.guest');
        }

        if ($guestId && !$user) {
            // 게스트 계정 자동 생성 또는 조회
            $userModel = \App\Models\User::where('guest_id', $guestId)->first();
            if (!$userModel) {
                $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::create([
                        'name' => '게스트_' . substr($guestId, 0, 8),
                        'email' => $guestId . '@guest.local',
                        'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                        'guest_id' => $guestId,
                        'is_guest' => true,
                    ]);
                }
            }
            $userId = $userModel->id;
            session(['guest_id' => $guestId]);
        } else {
            $userId = $user->id;
        }

        // 게임 데이터 조회/생성 (user_id 기준)
        $game = \App\Models\UserPuzzleGame::where('user_id', $userId)
            ->where('is_active', true)
            ->first();
        if (!$game) {
            $game = \App\Models\UserPuzzleGame::create([
                'user_id' => $userId,
                'guest_id' => $guestId ?? null,
                'current_level' => 1,
                'first_attempt_at' => now(),
                'is_active' => true,
            ]);
        }
        $level = \App\Models\PuzzleLevel::where('level', $game->current_level)->first();
        // 현재 레벨 클리어 횟수 계산
        $clearCount = 0;
        if ($level && $level->clear_condition) {
            $clearCount = \DB::table('puzzle_game_records')
                ->where('user_id', $userId)
                ->where('level_played', $game->current_level)
                ->where('game_status', 'completed')
                ->count();
        }
        return view('puzzle.game.index', compact('game', 'level', 'clearCount'));
    }

    public function getTemplate(Request $request)
    {
        $user = Auth::user();
        $guestId = $request->query('guest_id') ?? session('guest_id');

        if (!$user && !$guestId) {
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        if ($guestId && !$user) {
            $userModel = \App\Models\User::where('guest_id', $guestId)->first();
            if (!$userModel) {
                $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::create([
                        'name' => '게스트_' . substr($guestId, 0, 8),
                        'email' => $guestId . '@guest.local',
                        'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                        'guest_id' => $guestId,
                        'is_guest' => true,
                    ]);
                }
            }
            $userId = $userModel->id;
            session(['guest_id' => $guestId]);
        } else {
            $userId = $user->id;
        }

        // 게임 데이터 조회/생성 (user_id 기준)
        $game = \App\Models\UserPuzzleGame::where('user_id', $userId)
            ->where('is_active', true)
            ->first();
        if (!$game) {
            $game = \App\Models\UserPuzzleGame::create([
                'user_id' => $userId,
                'guest_id' => $guestId ?? null,
                'current_level' => 1,
                'first_attempt_at' => now(),
                'is_active' => true,
            ]);
        }
        $level = \App\Models\PuzzleLevel::where('level', $game->current_level)->first();
        if (!$level) {
            return response()->json(['error' => '레벨을 찾을 수 없습니다.'], 404);
        }

        // Wordle처럼 현재 활성 퍼즐이 있는지 확인
        if ($game->hasActivePuzzle()) {
            \Log::info("기존 퍼즐 세션 복원", [
                'user_id' => $userId,
                'level' => $game->current_level,
                'puzzle_started_at' => $game->current_puzzle_started_at
            ]);
            
            // 저장된 퍼즐 데이터와 게임 상태 반환
            $puzzleData = $game->getCurrentPuzzleData();
            $gameState = $game->getCurrentGameState();
            
            // 기존 세션 복원 시 힌트 조회 로그 추가
            \Log::info("기존 세션 복원 - 퍼즐 데이터 확인", [
                'user_id' => $userId,
                'puzzleData' => $puzzleData,
                'has_words' => isset($puzzleData['words']) ? count($puzzleData['words']) : 0
            ]);
            
            // 정답 단어 정보 추가 (보안을 위해 맞춘 단어만)
            $answeredWordsWithAnswers = [];
            if (isset($gameState['answered_words']) && is_array($gameState['answered_words'])) {
                foreach ($gameState['answered_words'] as $wordId) {
                    $word = DB::table('pz_words')->where('id', $wordId)->first();
                    if ($word) {
                        $answeredWordsWithAnswers[$wordId] = $word->word;
                    }
                }
            }
            
            return response()->json([
                'template' => $puzzleData['template'],
                'level' => $level,
                'game' => $game,
                'game_state' => $gameState,
                'answered_words_with_answers' => $answeredWordsWithAnswers, // 맞춘 정답 단어들
                'is_restored' => true // 프론트엔드에서 복원된 퍼즐임을 알 수 있도록
            ]);
        }

        // 새로운 퍼즐 생성
        \Log::info("새로운 퍼즐 세션 시작", [
            'user_id' => $userId,
            'level' => $game->current_level
        ]);

        // 레벨에 해당하는 템플릿 중 랜덤으로 하나 선택
        $template = \DB::table('puzzle_grid_templates')
            ->where('level_id', $level->id)
            ->where('is_active', true)
            ->inRandomOrder()
            ->first();

        if (!$template) {
            return response()->json(['error' => '해당 레벨의 템플릿을 찾을 수 없습니다.'], 404);
        }

        // 5회 반복 로직으로 단어 추출 시도
        $maxRetries = 5;
        $retryCount = 0;
        $extractData = null;
        
        while ($retryCount < $maxRetries) {
            $retryCount++;
            
            // GridTemplateController의 extractWords 메서드 호출
            $templateService = app(\App\Services\PuzzleGridTemplateService::class);
            $gridTemplateController = new GridTemplateController($templateService);
            $extractRequest = new Request(['template_id' => $template->id]);
            $extractResult = $gridTemplateController->extractWords($extractRequest);
            
            $extractData = json_decode($extractResult->getContent(), true);
            
            // 성공하면 루프 종료
            if ($extractData['success']) {
                \Log::info("게임 단어 추출 성공 - 시도 #{$retryCount}에서 완료", [
                    'template_id' => $template->id,
                    'level_id' => $level->id,
                    'user_id' => $userId
                ]);
                break;
            }
            
            \Log::info("게임 단어 추출 실패 - 시도 #{$retryCount}", [
                'template_id' => $template->id,
                'level_id' => $level->id,
                'user_id' => $userId,
                'error_message' => $extractData['message'] ?? '알 수 없는 오류'
            ]);
            
            // 마지막 시도가 아니면 잠시 대기 후 재시도
            if ($retryCount < $maxRetries) {
                usleep(100000); // 0.1초 대기
            }
        }
        
        // 5회 시도 후에도 실패한 경우
        if (!$extractData['success']) {
            \Log::error("게임 단어 추출 최종 실패 - 5회 시도 후 실패", [
                'template_id' => $template->id,
                'level_id' => $level->id,
                'user_id' => $userId,
                'final_error' => $extractData['message'] ?? '알 수 없는 오류'
            ]);
            
            return response()->json([
                'error' => '단어 추출에 실패했습니다. 잠시 후 다시 시도해주세요.',
                'retry_count' => $retryCount,
                'message' => $extractData['message'] ?? '알 수 없는 오류'
            ], 500);
        }

        // 단어 정보에 실제 pz_words ID 추가 (보안을 위해 단어 텍스트는 제거)
        $wordsWithIds = [];
        foreach ($extractData['extracted_words']['word_order'] as $wordInfo) {
            // 단어 텍스트로 실제 pz_words ID 찾기
            $pzWord = DB::table('pz_words')
                ->where('word', $wordInfo['extracted_word'])
                ->where('is_active', true)
                ->first();
            
            // 보안을 위해 단어 텍스트는 제거하고 ID만 전송
            $secureWordInfo = [
                'pz_word_id' => $pzWord ? $pzWord->id : null,
                'category' => $pzWord ? $pzWord->category : '일반',
                'position' => $wordInfo['position'],
                'word_id' => $wordInfo['word_id'] // 그리드 내 단어 ID
            ];
            
            // 기본 힌트 정보만 추가 (정답 단어는 제외)
            if ($pzWord) {
                \Log::info("단어 힌트 조회 시작", [
                    'word_id' => $pzWord->id,
                    'word' => $pzWord->word
                ]);
                
                // 먼저 is_primary = 't'인 힌트를 찾고, 없으면 첫 번째 힌트를 사용
                $baseHint = DB::table('pz_hints')
                    ->where('word_id', $pzWord->id)
                    ->where('is_primary', 't')
                    ->orderBy('id', 'asc')
                    ->first();
                
                \Log::info("is_primary 힌트 조회 결과", [
                    'word_id' => $pzWord->id,
                    'baseHint' => $baseHint
                ]);
                
                // is_primary = true인 힌트가 없으면 첫 번째 힌트를 기본 힌트로 사용
                if (!$baseHint) {
                    $baseHint = DB::table('pz_hints')
                        ->where('word_id', $pzWord->id)
                        ->orderBy('id', 'asc')
                        ->first();
                    
                    \Log::info("fallback 힌트 조회 결과", [
                        'word_id' => $pzWord->id,
                        'fallbackHint' => $baseHint
                    ]);
                }
                
                $secureWordInfo['hint_id'] = $baseHint ? $baseHint->id : null;
                $secureWordInfo['hint'] = $baseHint ? $baseHint->hint_text : '힌트가 없습니다.';
                
                \Log::info("최종 힌트 정보", [
                    'word_id' => $pzWord->id,
                    'hint_id' => $secureWordInfo['hint_id'],
                    'hint' => $secureWordInfo['hint']
                ]);
            } else {
                $secureWordInfo['hint_id'] = null;
                $secureWordInfo['hint'] = '힌트가 없습니다.';
                \Log::info("단어를 찾을 수 없음", ['word' => $wordInfo['extracted_word']]);
            }
            
            $wordsWithIds[] = $secureWordInfo;
        }

        // 퍼즐 데이터 구성
        $puzzleData = [
            'template' => [
                'id' => $template->id,
                'template_name' => $template->template_name,
                'grid_pattern' => $extractData['extracted_words']['grid_info']['pattern'],
                'grid_width' => $extractData['extracted_words']['grid_info']['width'],
                'grid_height' => $extractData['extracted_words']['grid_info']['height'],
                'words' => $wordsWithIds
            ],
            'level' => $level,
            'game' => $game,
        ];

        // 새로운 퍼즐 세션 시작
        $game->startNewPuzzle($puzzleData);

        return response()->json([
            'template' => $puzzleData['template'],
            'level' => $level,
            'game' => $game,
            'is_restored' => false // 새로운 퍼즐임을 표시
        ]);
    }

    public function checkAnswer(Request $request)
    {
        \Log::info('checkAnswer 진입', $request->all());
        try {
            $request->validate([
                'word_id' => 'required|integer',
                'answer' => 'required|string',
            ]);

            \Log::info('checkAnswer validate 통과', $request->all());

            $user = Auth::user();
            $guestId = $request->input('guest_id') ?? session('guest_id');
            $userId = null;

            if (!$user && !$guestId) {
                \Log::warning('checkAnswer: 로그인/게스트 ID 모두 없음');
                return response()->json(['error' => '로그인이 필요합니다.'], 401);
            }

            if ($guestId && !$user) {
                $userModel = \App\Models\User::where('guest_id', $guestId)->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                    if (!$userModel) {
                        $userModel = \App\Models\User::create([
                            'name' => '게스트_' . substr($guestId, 0, 8),
                            'email' => $guestId . '@guest.local',
                            'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                            'guest_id' => $guestId,
                            'is_guest' => true,
                        ]);
                    }
                }
                $userId = $userModel->id;
                session(['guest_id' => $guestId]);
                \Log::info('checkAnswer: 게스트 계정 사용', ['user_id' => $userId, 'guest_id' => $guestId]);
            } else {
                $userId = $user->id;
                \Log::info('checkAnswer: 로그인 계정 사용', ['user_id' => $userId]);
            }

            $game = \App\Models\UserPuzzleGame::where('user_id', $userId)
                ->where('is_active', true)
                ->first();

            if (!$game) {
                \Log::warning('checkAnswer: 게임을 찾을 수 없음', ['user_id' => $userId]);
                return response()->json(['error' => '게임을 찾을 수 없습니다.'], 404);
            }

            $word = \DB::table('pz_words')
                ->where('id', $request->word_id)
                ->where('is_active', true)
                ->first();

            if (!$word) {
                \Log::warning('checkAnswer: 단어를 찾을 수 없음', ['word_id' => $request->word_id]);
                return response()->json(['error' => '단어를 찾을 수 없습니다.'], 404);
            }

            $userAnswer = trim(strtolower($request->answer));
            $correctAnswer = trim(strtolower($word->word));
            $isCorrect = ($userAnswer === $correctAnswer);

            \Log::info('checkAnswer 정답 비교', [
                'userAnswer' => $userAnswer,
                'correctAnswer' => $correctAnswer,
                'isCorrect' => $isCorrect
            ]);

            // 보안을 위해 정답 정보는 로그에 기록하지 않음
            \Log::info('Answer check', [
                'word_id' => $request->word_id,
                'is_correct' => $isCorrect
            ]);
            
            // 게임 상태 업데이트
            $gameState = $game->getCurrentGameState() ?? [
                'answered_words' => [],
                'wrong_answers' => [],
                'hints_used' => [],
                'additional_hints' => [],
                'started_at' => now()->toISOString()
            ];

            if ($isCorrect) {
                $game->incrementCorrectAnswer();
                $message = '정답입니다!';
                
                // 정답 단어를 게임 상태에 추가
                if (!in_array($word->id, $gameState['answered_words'])) {
                    $gameState['answered_words'][] = $word->id;
                }
            } else {
                $game->incrementWrongAnswer();
                $wrongCount = $game->current_level_wrong_answers;
                
                // 틀린 답변 기록
                $this->recordWrongAnswer($userId, $word->id, $request->answer, $word->word, $word->category, $game->current_level);
                
                // 오답 기록을 게임 상태에 추가
                if (!isset($gameState['wrong_answers'][$word->id])) {
                    $gameState['wrong_answers'][$word->id] = 0;
                }
                $gameState['wrong_answers'][$word->id]++;
                
                // 오답 4회일 때 특별한 메시지
                if ($wrongCount == 4) {
                    $message = '현재 오답이 4회 입니다, 5회 오답시 레벨을 재시작합니다';
                } else {
                    $message = '오답입니다. 누적 오답: ' . $wrongCount . '회';
                }
                
                // 오답 5회 초과 체크
                if ($wrongCount >= 5) {
                    // 게임 상태 초기화
                    $game->current_level_correct_answers = 0;
                    $game->current_level_wrong_answers = 0;
                    $game->endCurrentPuzzle(); // 현재 퍼즐 세션 종료
                    $game->save();
                    
                    return response()->json([
                        'is_correct' => false,
                        'message' => '오답회수가 초과했습니다, 레벨을 다시 시작합니다.',
                        'restart_level' => true,
                        'wrong_count_exceeded' => true
                    ]);
                }
            }

            // 게임 상태 저장
            $game->updateGameState($gameState);

            return response()->json([
                'is_correct' => $isCorrect,
                'message' => $message,
                'correct_answer' => $isCorrect ? $word->word : null, // 정답일 때만 정답 전송
                'correct_count' => $game->current_level_correct_answers,
                'wrong_count' => $game->current_level_wrong_answers
            ]);
        } catch (\Exception $e) {
            \Log::error('checkAnswer 예외 발생', [
                'msg' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
                'request' => $request->all()
            ]);
            return response()->json(['error' => '서버 오류'], 500);
        }
    }

    public function getHints(Request $request)
    {
        $request->validate([
            'word_id' => 'required|integer',
            'current_hint_id' => 'nullable|integer', // 현재 보여주고 있는 힌트 ID
            'base_hint_id' => 'nullable|integer', // 기본 힌트 ID (제외해야 함)
        ]);

        $user = Auth::user();
        $guestId = $request->input('guest_id') ?? session('guest_id');
        $userId = null;

        if (!$user && !$guestId) {
            \Log::warning('getHints: 로그인/게스트 ID 모두 없음');
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        if ($guestId && !$user) {
            $userModel = \App\Models\User::where('guest_id', $guestId)->first();
            if (!$userModel) {
                $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::create([
                        'name' => '게스트_' . substr($guestId, 0, 8),
                        'email' => $guestId . '@guest.local',
                        'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                        'guest_id' => $guestId,
                        'is_guest' => true,
                    ]);
                }
            }
            $userId = $userModel->id;
            session(['guest_id' => $guestId]);
            \Log::info('getHints: 게스트 계정 사용', ['user_id' => $userId, 'guest_id' => $guestId]);
        } else {
            $userId = $user->id;
            \Log::info('getHints: 로그인 계정 사용', ['user_id' => $userId]);
        }

        // 요구사항: 기본 힌트 제외하고 난이도 순서로 힌트 선택 (쉬운 것부터)
        $query = DB::table('pz_hints')
            ->where('word_id', $request->word_id)
            ->where('id', '<>', $request->base_hint_id) // 기본 힌트 제외
            ->select('hint_text as hint', 'id', 'difficulty')
            ->orderBy('difficulty', 'asc') // 난이도 낮은 것부터 (쉬운 것 우선)
            ->limit(1);

        // 실제 실행되는 SQL 쿼리를 로그에 출력
        \Log::info('SQL Query', [
            'sql' => $query->toSql(),
            'bindings' => $query->getBindings(),
            'base_hint_id' => $request->base_hint_id
        ]);

        // 추가 힌트 하나만 가져오기
        $additionalHint = $query->first();

        \Log::info('Query result', [
            'found_hint' => $additionalHint ? true : false,
            'hint_id' => $additionalHint ? $additionalHint->id : null,
            'hint_text' => $additionalHint ? $additionalHint->hint : null,
            'difficulty' => $additionalHint ? $additionalHint->difficulty : null
        ]);

        if (!$additionalHint) {
            return response()->json([
                'hint' => null,
                'message' => '더 이상 사용할 수 있는 힌트가 없습니다.'
            ]);
        }

        return response()->json([
            'hint' => $additionalHint,
            'message' => '추가 힌트를 제공합니다.'
        ]);
    }

    public function completeLevel(Request $request)
    {
        \Log::info('completeLevel 진입', $request->all());
        try {
            $user = Auth::user();
            $guestId = $request->input('guest_id') ?? session('guest_id');
            $userId = null;

            if (!$user && !$guestId) {
                \Log::warning('completeLevel: 로그인/게스트 ID 모두 없음');
                return response()->json(['error' => '로그인이 필요합니다.'], 401);
            }

            if ($guestId && !$user) {
                $userModel = \App\Models\User::where('guest_id', $guestId)->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                    if (!$userModel) {
                        $userModel = \App\Models\User::create([
                            'name' => '게스트_' . substr($guestId, 0, 8),
                            'email' => $guestId . '@guest.local',
                            'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                            'guest_id' => $guestId,
                            'is_guest' => true,
                        ]);
                    }
                }
                $userId = $userModel->id;
                session(['guest_id' => $guestId]);
                \Log::info('completeLevel: 게스트 계정 사용', ['user_id' => $userId, 'guest_id' => $guestId]);
            } else {
                $userId = $user->id;
                \Log::info('completeLevel: 로그인 계정 사용', ['user_id' => $userId]);
            }

            $game = \App\Models\UserPuzzleGame::where('user_id', $userId)
                ->where('is_active', true)
                ->first();

            if (!$game) {
                \Log::warning('completeLevel: 게임을 찾을 수 없음', ['user_id' => $userId]);
                return response()->json(['error' => '게임을 찾을 수 없습니다.'], 404);
            }

            // 레벨 클리어 기록 저장 (조건 충족 여부와 관계없이 항상 저장)
            $gameData = [
                'score' => $request->input('score', 0),
                'play_time' => $request->input('play_time', 0),
                'hints_used' => $request->input('hints_used', 0),
                'words_found' => $request->input('words_found', 0),
                'total_words' => $request->input('total_words', 0),
                'accuracy' => $request->input('accuracy', 0),
            ];
            $game->recordLevelClear($gameData);

            // 레벨 클리어 조건 확인 (기록 저장 후)
            if (!$game->checkLevelClearCondition()) {
                $level = \App\Models\PuzzleLevel::where('level', $game->current_level)->first();
                $clearCount = \DB::table('puzzle_game_records')
                    ->where('user_id', $userId)
                    ->where('level_played', $game->current_level)
                    ->where('game_status', 'completed')
                    ->count();
                $remaining = $level && $level->clear_condition ? max(0, $level->clear_condition - $clearCount) : 1;
                return response()->json([
                    'error' => '레벨 클리어 조건을 만족하지 않습니다.',
                    'message' => "클리어까지 {$remaining}회 남았습니다.",
                    'remaining' => $remaining,
                    'condition_not_met' => true
                ], 400);
            }

            // 다음 레벨로 진행 가능한지 확인
            if (!$game->canAdvanceToNextLevel()) {
                return response()->json([
                    'message' => '축하합니다! 레벨을 완료했습니다.',
                    'new_level' => $game->current_level,
                    'success' => true,
                    'no_next_level' => true
                ]);
            }

            // 현재 퍼즐 세션 종료
            $game->endCurrentPuzzle();
            
            // 다음 레벨로 진행
            $game->advanceToNextLevel();

            return response()->json([
                'message' => '축하합니다! 다음 레벨로 진행합니다.',
                'new_level' => $game->current_level,
                'success' => true
            ]);
        } catch (\Exception $e) {
            \Log::error('completeLevel 예외 발생', [
                'msg' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
                'request' => $request->all()
            ]);
            return response()->json(['error' => '서버 오류'], 500);
        }
    }

    public function gameOver(Request $request)
    {
        $user = Auth::user();
        $game = UserPuzzleGame::where('user_id', $user->id)
            ->where('is_active', true)
            ->first();

        if (!$game) {
            return response()->json(['error' => '게임을 찾을 수 없습니다.'], 404);
        }

        $game->last_played_at = now();
        $game->save();

        return response()->json([
            'message' => '게임오버! 5분 후 재시도 가능합니다.',
        ]);
    }

    public function showAnswer(Request $request)
    {
        if (!Auth::check() || !Auth::user()->is_admin) {
            return response()->json(['error' => '관리자만 접근 가능합니다.'], 403);
        }

        $wordId = $request->input('word_id');
        if (!$wordId) {
            return response()->json(['error' => '단어 ID가 필요합니다.'], 400);
        }

        $word = PzWord::find($wordId);
        if (!$word) {
            return response()->json(['error' => '단어를 찾을 수 없습니다.'], 404);
        }

        return response()->json([
            'success' => true,
            'message' => "정답: {$word->word}"
        ]);
    }

    /**
     * 현재 레벨의 클리어 조건 정보를 실시간으로 조회
     */
    public function getClearCondition(Request $request)
    {
        \Log::info('getClearCondition 진입', $request->all());
        
        $user = Auth::user();
        $guestId = $request->query('guest_id') ?? session('guest_id');

        if (!$user && !$guestId) {
            \Log::warning('getClearCondition: 로그인/게스트 ID 모두 없음');
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        if ($guestId && !$user) {
            $userModel = \App\Models\User::where('guest_id', $guestId)->first();
            if (!$userModel) {
                $userModel = \App\Models\User::where('email', $guestId . '@guest.local')->first();
                if (!$userModel) {
                    $userModel = \App\Models\User::create([
                        'name' => '게스트_' . substr($guestId, 0, 8),
                        'email' => $guestId . '@guest.local',
                        'password' => bcrypt(\Illuminate\Support\Str::random(16)),
                        'guest_id' => $guestId,
                        'is_guest' => true,
                    ]);
                }
            }
            $userId = $userModel->id;
            session(['guest_id' => $guestId]);
            \Log::info('getClearCondition: 게스트 계정 사용', ['user_id' => $userId, 'guest_id' => $guestId]);
        } else {
            $userId = $user->id;
            \Log::info('getClearCondition: 로그인 계정 사용', ['user_id' => $userId]);
        }

        // 게임 데이터 조회
        $game = \App\Models\UserPuzzleGame::where('user_id', $userId)
            ->where('is_active', true)
            ->first();
        
        if (!$game) {
            \Log::warning('getClearCondition: 게임 데이터를 찾을 수 없음', ['user_id' => $userId]);
            return response()->json(['error' => '게임 데이터를 찾을 수 없습니다.'], 404);
        }

        $level = \App\Models\PuzzleLevel::where('level', $game->current_level)->first();
        if (!$level) {
            \Log::warning('getClearCondition: 레벨을 찾을 수 없음', ['current_level' => $game->current_level]);
            return response()->json(['error' => '레벨을 찾을 수 없습니다.'], 404);
        }

        // 현재 레벨 클리어 횟수 계산
        $clearCount = 0;
        if ($level->clear_condition > 1) {
            $clearCount = \DB::table('puzzle_game_records')
                ->where('user_id', $userId)
                ->where('level_played', $game->current_level)
                ->where('game_status', 'completed')
                ->count();
        }

        \Log::info('getClearCondition 결과', [
            'user_id' => $userId,
            'current_level' => $game->current_level,
            'clear_count' => $clearCount,
            'clear_condition' => $level->clear_condition,
            'level_name' => $level->level_name
        ]);

        return response()->json([
            'success' => true,
            'clear_count' => $clearCount,
            'clear_condition' => $level->clear_condition,
            'current_level' => $game->current_level,
            'level_name' => $level->level_name
        ]);
    }

    /**
     * 틀린 답변 기록
     */
    private function recordWrongAnswer($userId, $wordId, $userAnswer, $correctAnswer, $category, $level)
    {
        try {
            // 매번 새로운 틀린 답변 기록 생성
            DB::table('user_wrong_answers')->insert([
                'user_id' => $userId,
                'word_id' => $wordId,
                'user_answer' => $userAnswer,
                'correct_answer' => $correctAnswer,
                'category' => $category,
                'level' => $level,
                'created_at' => now()
            ]);

            \Log::info('틀린 답변 기록', [
                'user_id' => $userId,
                'word_id' => $wordId,
                'user_answer' => $userAnswer,
                'category' => $category,
                'level' => $level
            ]);

        } catch (\Exception $e) {
            \Log::error('틀린 답변 기록 실패', [
                'error' => $e->getMessage(),
                'user_id' => $userId,
                'word_id' => $wordId
            ]);
        }
    }
}
