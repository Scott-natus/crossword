<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;
use App\Services\PuzzleGeneratorService;
use App\Models\PuzzleSessions;
use App\Models\PuzzleHintLimits;
use App\Models\PuzzleScoringRules;
use App\Models\PzWord;
use App\Models\PuzzleLevel;
use App\Models\UserPuzzleGame;
use Illuminate\Support\Facades\DB;

class PuzzleGameController extends Controller
{
    /**
     * React 앱용 퍼즐 템플릿 조회 (라라벨 웹의 getTemplate 참고)
     */
    public function getTemplate(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        $game = UserPuzzleGame::where('user_id', $user->id)
            ->where('is_active', true)
            ->first();

        if (!$game) {
            $game = UserPuzzleGame::create([
                'user_id' => $user->id,
                'current_level' => 1,
                'first_attempt_at' => now(),
                'is_active' => true,
            ]);
        }

        $level = PuzzleLevel::where('level', $game->current_level)->first();
        if (!$level) {
            return response()->json(['error' => '레벨을 찾을 수 없습니다.'], 404);
        }

        // 레벨에 해당하는 템플릿 중 랜덤으로 하나 선택
        $template = DB::table('puzzle_grid_templates')
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
        $wordsWithIds = [];
        $success = false;
        
        while ($retryCount < $maxRetries && !$success) {
            try {
                $extractData = $this->extractWordsFromTemplate($template, $level);
                $wordsWithIds = [];
                foreach ($extractData['extracted_words']['word_order'] as $wordInfo) {
                    // 단어 텍스트로 실제 pz_words ID 찾기
                    $pzWord = DB::table('pz_words')
                        ->where('word', $wordInfo['extracted_word'])
                        ->where('is_active', true)
                        ->first();
                    // 보안을 위해 단어 텍스트는 제거하고 ID만 전송
                    $secureWordInfo = [
                        'id' => $wordInfo['position']['id'], // 배지 번호 (puzzle_grid_templates.word_positions의 id)
                        'pz_word_id' => $pzWord ? $pzWord->id : null, // 실제 단어 ID (pz_words.id)
                        'category' => $pzWord ? $pzWord->category : '일반',
                        'position' => $wordInfo['position'],
                        'word_id' => $pzWord ? $pzWord->id : null, // 실제 단어 ID (pz_words.id) - 정답/힌트 조회용
                    ];
                    // 기본 힌트 정보만 추가 (정답 단어는 제외)
                    if ($pzWord) {
                        $baseHint = DB::table('pz_hints')
                            ->where('word_id', $pzWord->id)
                            ->where('is_primary', true)
                            ->where(function($q) {
                                $q->where('language_code', 'KR')
                                  ->orWhere('language_code', 'ko');
                            })
                            ->first();
                        $secureWordInfo['hint_id'] = $baseHint ? $baseHint->id : null;
                        $secureWordInfo['hint'] = $baseHint ? $baseHint->hint_text : null;
                    } else {
                        $secureWordInfo['hint_id'] = null;
                        $secureWordInfo['hint'] = null;
                    }
                    $wordsWithIds[] = $secureWordInfo;
                }
                // 하나라도 id나 hint가 null이면 실패로 간주
                $hasNull = false;
                foreach ($wordsWithIds as $w) {
                    if (is_null($w['id']) || is_null($w['hint'])) {
                        $hasNull = true;
                        break;
                    }
                }
                if (!$hasNull) {
                    $success = true;
                } else {
                    $extractData = null;
                    $wordsWithIds = [];
                    $retryCount++;
                    continue;
                }
            } catch (\Exception $e) {
                \Log::error("단어 추출 실패 (시도 {$retryCount}): " . $e->getMessage());
                $retryCount++;
                if ($retryCount >= $maxRetries) {
                    return response()->json(['error' => '퍼즐 생성에 실패했습니다. 다시 시도해주세요.'], 500);
                }
            }
        }

        if (!$success) {
            return response()->json(['error' => '퍼즐 생성에 실패했습니다.'], 500);
        }

        return response()->json([
            'success' => true,
            'data' => [
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
            ]
        ]);
    }

    /**
     * 템플릿에서 단어 추출 (교차점 음절 일치 로직 포함) - GridTemplateController와 동일한 재시도 로직
     */
    private function extractWordsFromTemplate($template, $level)
    {
        $gridPattern = json_decode($template->grid_pattern, true);
        $wordPositions = json_decode($template->word_positions, true);
        
        // 단어 위치 정보를 id 순서대로 정렬 (백엔드에서 처리)
        usort($wordPositions, function($a, $b) {
            return $a['id'] - $b['id'];
        });

        // wordPositions를 클래스 변수로 저장 (교차점 찾기에서 사용)
        $this->wordPositions = $wordPositions;

        // 최대 5회 재시도 (GridTemplateController와 동일)
        $maxRetries = 5;
        $retryCount = 0;
        $extractedWords = [];
        $confirmedWords = []; // 확정된 단어들을 저장 (word_id => word_text)

        while ($retryCount < $maxRetries) {
            $retryCount++;
            $extractedWords = [];
            $confirmedWords = []; // 확정된 단어들을 초기화
            $extractionFailed = false;

            \Log::info("단어 추출 시도 #{$retryCount} 시작");

            foreach ($wordPositions as $wordPos) {
                $wordId = $wordPos['id'];
                
                // 이미 확정된 단어는 건너뛰기
                if (isset($confirmedWords[$wordId])) {
                    continue;
                }

                // 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
                $intersections = $this->findIntersectionsWithConfirmedWords($wordPos, $confirmedWords);
                
                // 교차점을 가지고 있지 않다면 독립 단어로 처리
                if (empty($intersections)) {
                    $word = $this->findSuitableWord($wordPos, $level, $confirmedWords);
                    
                    if ($word) {
                        $extractedWords[] = $word->word;
                        $confirmedWords[$wordId] = $word->word;
                    } else {
                        $extractionFailed = true;
                        break;
                    }
                } else {
                    // 교차점을 가지고 있다면, 확정된 단어들의 교차점 음절을 추출한다.
                    $confirmedIntersectionSyllables = [];
                    
                    \Log::info("교차점 발견 - 단어 ID {$wordId}", [
                        'intersections_count' => count($intersections),
                        'intersections' => $intersections
                    ]);
                    
                    foreach ($intersections as $intersection) {
                        $connectedWordId = $intersection['connected_word_id'];
                        $connectedWord = $confirmedWords[$connectedWordId];
                        $connectedWordPosition = $this->findWordById($connectedWordId, $wordPositions);
                        $connectedSyllablePos = $this->getSyllablePosition($connectedWordPosition, $intersection['position']);
                        $connectedSyllable = mb_substr($connectedWord, $connectedSyllablePos - 1, 1, 'UTF-8');
                        
                        $currentSyllablePos = $this->getSyllablePosition($wordPos, $intersection['position']);
                        
                        \Log::info("교차점 음절 계산", [
                            'word_id' => $wordId,
                            'connected_word_id' => $connectedWordId,
                            'connected_word' => $connectedWord,
                            'connected_syllable_pos' => $connectedSyllablePos,
                            'connected_syllable' => $connectedSyllable,
                            'current_syllable_pos' => $currentSyllablePos,
                            'intersection_position' => $intersection['position']
                        ]);
                        
                        $confirmedIntersectionSyllables[] = [
                            'syllable' => $connectedSyllable,
                            'position' => $currentSyllablePos
                        ];
                    }
                    
                    // 확정된 음절들과 매칭되는 단어를 추출하여 확정한다.
                    $word = $this->findSuitableWordWithConfirmedSyllables($wordPos, $level, $confirmedIntersectionSyllables, $confirmedWords);
                    
                    if ($word) {
                        $extractedWords[] = $word->word;
                        $confirmedWords[$wordId] = $word->word;
                    } else {
                        $extractionFailed = true;
                        break;
                    }
                }
            }

            // 모든 단어가 성공적으로 추출되었으면 루프 종료
            if (!$extractionFailed) {
                \Log::info("단어 추출 성공 - 시도 #{$retryCount}에서 완료");
                break;
            }

            \Log::info("단어 추출 실패 - 시도 #{$retryCount}, 재시도 예정");
        }

        // 5회 시도 후에도 실패한 경우
        if ($extractionFailed) {
            $failedWordId = null;
            foreach ($wordPositions as $word) {
                if (!isset($confirmedWords[$word['id']])) {
                    $failedWordId = $word['id'];
                    break;
                }
            }

            \Log::error("단어 추출 최종 실패", [
                'failed_word_id' => $failedWordId,
                'retry_count' => $retryCount,
                'template_id' => $template->id,
                'level_id' => $level->id
            ]);

            // 실패 시 빈 결과 반환 (React Native 앱에서 처리)
            return [
                'extracted_words' => [
                    'words' => [],
                    'word_order' => [],
                    'grid_info' => [
                        'pattern' => $gridPattern,
                        'width' => $template->grid_width,
                        'height' => $template->grid_height
                    ]
                ]
            ];
        }

        // 성공 시 word_order 생성
        $wordOrder = [];
        foreach ($extractedWords as $index => $word) {
            $wordOrder[] = [
                'extracted_word' => $word,
                'position' => $wordPositions[$index],
                'word_id' => $wordPositions[$index]['id']
            ];
        }

        return [
            'extracted_words' => [
                'words' => $extractedWords,
                'word_order' => $wordOrder,
                'grid_info' => [
                    'pattern' => $gridPattern,
                    'width' => $template->grid_width,
                    'height' => $template->grid_height
                ]
            ]
        ];
    }

    /**
     * 확정된 음절들과 매칭되는 단어 찾기 (GridTemplateController와 동일한 로직)
     */
    private function findSuitableWordWithConfirmedSyllables($wordPosition, $level, $confirmedSyllables, $confirmedWords = [])
    {
        $length = $wordPosition['length'];
        
        if (empty($confirmedSyllables)) {
            \Log::warning("확정된 음절 정보가 없습니다.", [
                'word_id' => $wordPosition['id']
            ]);
            return null;
        }
        
        // 레벨에 설정된 단어 난이도 사용
        $wordDifficulty = $level->word_difficulty;
        
        // 새로운 난이도 규칙 적용 (GridTemplateController와 동일)
        $allowedDifficulties = $this->getAllowedDifficulties($wordDifficulty);
        
        // 확정된 음절들을 기반으로 조건 생성
        $query = DB::table('pz_words as a')
            ->join('pz_hints as b', 'a.id', '=', 'b.word_id')
            ->select('a.word', 'b.hint_text as hint')
            ->where('a.length', $length)
            ->whereIn('a.difficulty', $allowedDifficulties)
            ->where('a.is_active', true);
        
        // 교차점을 공유하는 다른 단어들과 같은 단어 제외
        if (!empty($confirmedWords)) {
            $excludedWords = array_values($confirmedWords);
            $query->whereNotIn('a.word', $excludedWords);
        }
        
        // 각 확정된 음절에 대한 조건 추가
        \Log::info("교차점 음절 일치 조건 추가", [
            'word_id' => $wordPosition['id'],
            'confirmed_syllables_count' => count($confirmedSyllables),
            'confirmed_syllables' => $confirmedSyllables
        ]);
        
        foreach ($confirmedSyllables as $syllableInfo) {
            $syllable = $syllableInfo['syllable'];
            $position = $syllableInfo['position'];
            $query->whereRaw("SUBSTRING(a.word, {$position}, 1) = ?", [$syllable]);
            
            \Log::info("음절 일치 조건 추가", [
                'position' => $position,
                'syllable' => $syllable,
                'condition' => "SUBSTRING(a.word, {$position}, 1) = '{$syllable}'"
            ]);
        }
        
        $query->orderByRaw('RANDOM()');
        
        $result = $query->first();
        
        if (!$result) {
            \Log::warning("단어 추출 실패 - 확정된 음절들과 매칭되는 단어를 찾을 수 없습니다.", [
                'word_id' => $wordPosition['id'],
                'required_syllables' => $confirmedSyllables,
                'length' => $length,
                'allowed_difficulties' => $allowedDifficulties
            ]);
            return null;
        }
        
        return $result;
    }

    /**
     * 새로운 난이도 규칙 적용 (GridTemplateController와 동일)
     */
    private function getAllowedDifficulties($levelDifficulty)
    {
        switch ($levelDifficulty) {
            case 1:
                return [1];
            case 2:
                return [1, 2];
            case 3:
                return [1, 2, 3];
            case 4:
                return [1, 2, 3, 4];
            case 5:
                return [1, 2, 3, 4, 5];
            default:
                return [1, 2, 3, 4, 5];
        }
    }

    /**
     * 적합한 단어 찾기 (교차점 음절 일치 로직 포함)
     */
    private function findSuitableWord($wordPosition, $level, $confirmedWords = [])
    {
        $length = $wordPosition['length'];
        $direction = $wordPosition['direction'];
        
        // 레벨 조건에 맞는 단어 검색
        $query = DB::table('pz_words')
            ->where('length', $length)
            ->where('is_active', true)
            ->where('difficulty', '<=', $level->word_difficulty);
        
        // 이미 사용된 단어 제외
        if (!empty($this->usedWords)) {
            $query->whereNotIn('word', $this->usedWords);
        }
        
        // 웹사이트와 동일한 순차적 단어 추출 로직
        if (!empty($confirmedWords)) {
            \Log::info("확정된 단어들 확인", [
                'current_word_id' => $wordPosition['id'],
                'confirmed_words_count' => count($confirmedWords),
                'confirmed_words' => $confirmedWords
            ]);
            
            // 현재 단어와 확정된 단어들의 교차점 확인
            foreach ($confirmedWords as $confirmedWordId => $confirmedWord) {
                $confirmedWordPosition = $this->findWordById($confirmedWordId, $this->wordPositions);
                if (!$confirmedWordPosition) {
                    continue;
                }
                
                $intersection = $this->findIntersection($wordPosition, $confirmedWordPosition);
                if ($intersection) {
                    \Log::info("교차점 발견", [
                        'current_word_id' => $wordPosition['id'],
                        'confirmed_word_id' => $confirmedWordId,
                        'intersection' => $intersection
                    ]);
                    
                    // 교차점 음절 계산
                    $connectedSyllablePos = $this->getSyllablePosition($confirmedWordPosition, $intersection);
                    $currentSyllablePos = $this->getSyllablePosition($wordPosition, $intersection);
                    
                    if ($connectedSyllablePos !== null && $currentSyllablePos !== null) {
                        $connectedSyllable = mb_substr($confirmedWord, $connectedSyllablePos - 1, 1, 'UTF-8');
                        
                        \Log::info("교차점 음절 조건 추가", [
                            'current_word_id' => $wordPosition['id'],
                            'confirmed_word_id' => $confirmedWordId,
                            'connected_word' => $confirmedWord,
                            'connected_syllable' => $connectedSyllable,
                            'current_syllable_pos' => $currentSyllablePos
                        ]);
                        
                        $query->whereRaw("SUBSTRING(word, {$currentSyllablePos}, 1) = ?", [$connectedSyllable]);
                    }
                }
            }
        }
        
        $word = $query->inRandomOrder()->first();
        
        if ($word) {
            $this->usedWords[] = $word->word;
            return $word;
        }
        
        // 단어 추출 실패 시 로그 기록
        \Log::warning("단어 추출 실패", [
            'word_position_id' => $wordPosition['id'],
            'word_position' => $wordPosition,
            'level' => $level,
            'confirmed_words_count' => count($confirmedWords),
            'used_words_count' => count($this->usedWords)
        ]);
        
        return null;
    }

    /**
     * 교차점 찾기
     */
    private function findIntersectionsWithConfirmedWords($currentWord, $confirmedWords)
    {
        $intersections = [];
        
        \Log::info("교차점 검색 시작", [
            'current_word' => $currentWord,
            'confirmed_words_count' => count($confirmedWords)
        ]);
        
        foreach ($confirmedWords as $confirmedWordId => $confirmedWord) {
            $confirmedWordPosition = $this->findWordById($confirmedWordId, $this->wordPositions);
            if (!$confirmedWordPosition) {
                \Log::info("확정된 단어 위치를 찾을 수 없음", [
                    'confirmed_word_id' => $confirmedWordId
                ]);
                continue;
            }
            
            \Log::info("교차점 계산 시도", [
                'current_word' => $currentWord,
                'confirmed_word_position' => $confirmedWordPosition
            ]);
            
            $intersection = $this->findIntersection($currentWord, $confirmedWordPosition);
            if ($intersection) {
                \Log::info("교차점 발견!", [
                    'current_word_id' => $currentWord['id'],
                    'confirmed_word_id' => $confirmedWordId,
                    'intersection' => $intersection
                ]);
                
                $intersections[] = [
                    'connected_word_id' => $confirmedWordId,
                    'position' => $intersection
                ];
            } else {
                \Log::info("교차점 없음", [
                    'current_word_id' => $currentWord['id'],
                    'confirmed_word_id' => $confirmedWordId
                ]);
            }
        }
        
        return $intersections;
    }

    /**
     * 두 단어의 교차점 찾기
     */
    private function findIntersection($word1, $word2)
    {
        $start1 = $word1['start_x'];
        $end1 = $word1['end_x'];
        $y1 = $word1['start_y'];
        $direction1 = $word1['direction'];
        
        $start2 = $word2['start_x'];
        $end2 = $word2['end_x'];
        $y2 = $word2['start_y'];
        $direction2 = $word2['direction'];
        
        // 서로 다른 방향의 단어만 교차 가능
        if ($direction1 === $direction2) {
            return null;
        }
        
        // 가로 단어와 세로 단어의 교차점 찾기
        if ($direction1 === 'horizontal' && $direction2 === 'vertical') {
            // 가로 단어: start_x ~ end_x, 세로 단어: start_y ~ end_y
            // 세로 단어의 x 좌표가 가로 단어의 x 범위에 있고, 가로 단어의 y 좌표가 세로 단어의 y 범위에 있어야 함
            $verticalEndY = $word2['end_y'];
            if ($start2 >= $start1 && $start2 <= $end1 && $y1 >= $y2 && $y1 <= $verticalEndY) {
                return ['x' => $start2, 'y' => $y1];
            }
        } elseif ($direction1 === 'vertical' && $direction2 === 'horizontal') {
            // 세로 단어: start_y ~ end_y, 가로 단어: start_x ~ end_x
            // 가로 단어의 x 좌표가 세로 단어의 x 범위에 있고, 세로 단어의 y 좌표가 가로 단어의 y 범위에 있어야 함
            $verticalEndY = $word1['end_y'];
            if ($start1 >= $start2 && $start1 <= $end2 && $y2 >= $y1 && $y2 <= $verticalEndY) {
                return ['x' => $start1, 'y' => $y2];
            }
        }
        
        return null;
    }

    /**
     * 단어 위치에서 음절 위치 계산
     */
    private function getSyllablePosition($wordPosition, $intersection)
    {
        $direction = $wordPosition['direction'];
        $startX = $wordPosition['start_x'];
        $startY = $wordPosition['start_y'];
        
        if ($direction === 'horizontal') {
            $position = $intersection['x'] - $startX;
        } else {
            $position = $intersection['y'] - $startY;
        }
        
        // 위치가 유효한 범위 내에 있는지 확인
        if ($position < 0 || $position >= $wordPosition['length']) {
            \Log::warning("음절 위치가 유효하지 않음", [
                'word_position' => $wordPosition,
                'intersection' => $intersection,
                'calculated_position' => $position
            ]);
            return null;
        }
        
        return $position + 1; // 1-based index
    }

    /**
     * ID로 단어 위치 찾기
     */
    private function findWordById($id, $wordPositions)
    {
        foreach ($wordPositions as $position) {
            if ($position['id'] == $id) {
                return $position;
            }
        }
        return null;
    }

    /**
     * 퍼즐 생성
     */
    public function generate(Request $request, PuzzleGeneratorService $generator)
    {
        $request->validate([
            'level_id' => 'sometimes|required|integer|min:1|max:100',
            'level' => 'sometimes|required|integer|min:1|max:100',
        ]);
        
        $levelId = $request->input('level_id', $request->input('level'));

        if (!$levelId) {
            return response()->json(['success' => false, 'message' => 'level_id is required.'], 400);
        }

        $userId = $request->user()?->id;

        try {
            $puzzle = $generator->generatePuzzle($levelId, $userId);
        } catch (\Exception $e) {
            // 퍼즐 생성 중 어떤 오류가 발생하더라도, 서버가 다운되는 대신
            // 정상적인 JSON 오류 메시지를 반환하도록 처리합니다.
            return response()->json([
                'success' => false,
                'message' => $e->getMessage(),
            ], 500);
        }

        // 세션 저장
        $session = PuzzleSessions::create([
            'user_id' => $userId,
            'level_id' => $levelId,
            'session_token' => Str::uuid(),
            'grid_data' => $puzzle['grid'],
            'selected_words' => $puzzle['words'],
            'game_state' => null,
            'user_progress' => null,
            'started_at' => now(),
            'expires_at' => now()->addHours(2),
            'is_active' => true,
            'status' => 'active',
        ]);

        return response()->json([
            'success' => true,
            'data' => [
                'session_token' => $session->session_token,
                'level' => $puzzle['level'],
                'grid' => $puzzle['grid'],
                'words' => $puzzle['words'],
                'word_positions' => $puzzle['word_positions'],
                'grid_rules' => $puzzle['grid_rules'],
            ]
        ]);
    }

    /**
     * 답안 제출 (라라벨 웹과 동일한 로직)
     */
    public function submitAnswer(Request $request)
    {
        $request->validate([
            'word_id' => 'required|integer',
            'answer' => 'required|string',
        ]);

        $user = $request->user();
        $game = UserPuzzleGame::where('user_id', $user->id)
            ->where('is_active', true)
            ->first();

        if (!$game) {
            return response()->json(['error' => '게임을 찾을 수 없습니다.'], 404);
        }

        // 단어 ID로 실제 단어 정보 가져오기
        $word = DB::table('pz_words')
            ->where('id', $request->word_id)
            ->where('is_active', true)
            ->first();

        // 디버깅 로그 추가
        \Log::info('Word lookup', [
            'requested_word_id' => $request->word_id,
            'found_word' => $word ? $word->word : 'NOT_FOUND',
            'word_id_exists' => $word ? true : false
        ]);

        if (!$word) {
            return response()->json(['error' => '단어를 찾을 수 없습니다.'], 404);
        }

        // 정답 확인 (대소문자 구분 없이, 공백 제거)
        $userAnswer = trim(strtolower($request->answer));
        $correctAnswer = trim(strtolower($word->word));
        
        $isCorrect = ($userAnswer === $correctAnswer);
        
        // 디버깅 로그 추가
        \Log::info('Answer check', [
            'word_id' => $request->word_id,
            'user_answer' => $userAnswer,
            'correct_answer' => $correctAnswer,
            'is_correct' => $isCorrect
        ]);
        
        if ($isCorrect) {
            $game->incrementCorrectAnswer();
            $message = '정답입니다!';
        } else {
            $game->incrementWrongAnswer();
            $wrongCount = $game->current_level_wrong_answers;
            
            // 틀린 답변 기록
            $this->recordWrongAnswer($user->id, $word->id, $request->answer, $word->word, $word->category, $game->current_level);
            
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
                $game->save();
                
                return response()->json([
                    'is_correct' => false,
                    'message' => '오답회수가 초과했습니다, 레벨을 다시 시작합니다.',
                    'restart_level' => true,
                    'wrong_count_exceeded' => true
                ]);
            }
        }

        return response()->json([
            'is_correct' => $isCorrect,
            'message' => $message,
            'correct_answer' => $isCorrect ? $word->word : null, // 정답일 때만 정답 전송
            'correct_count' => $game->current_level_correct_answers,
            'wrong_count' => $game->current_level_wrong_answers,
            'show_answer' => $user->is_admin ? $word->word : null // 관리자일 때만 정답 전송
        ]);
    }

    /**
     * 힌트 요청 (라라벨 웹과 동일한 로직)
     */
    public function getPuzzleHints(Request $request)
    {
        $request->validate([
            'word_id' => 'required|integer',
            'current_hint_id' => 'nullable|integer', // 현재 보여주고 있는 힌트 ID
            'base_hint_id' => 'nullable|integer', // 기본 힌트 ID (제외해야 함)
        ]);

        $user = $request->user();
        if (!$user) {
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        // 디버깅 로그 추가
        \Log::info('getHints called', [
            'word_id' => $request->word_id,
            'current_hint_id' => $request->current_hint_id,
            'base_hint_id' => $request->base_hint_id,
            'has_current_hint_id' => $request->has('current_hint_id'),
            'has_base_hint_id' => $request->has('base_hint_id')
        ]);

        // 요구사항: 기본 힌트(is_primary=true) 제외하고 난이도 순서로 힌트 선택 (쉬운 것부터)
        // 한국어 힌트만 조회 (language_code='KR' 또는 'ko')
        $query = DB::table('pz_hints')
            ->where('word_id', $request->word_id)
            ->where('is_primary', false) // 기본 힌트 제외
            ->where(function($q) {
                $q->where('language_code', 'KR')
                  ->orWhere('language_code', 'ko');
            })
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
                'success' => false,
                'hint' => null,
                'message' => '더 이상 사용할 수 있는 힌트가 없습니다.'
            ]);
        }

        return response()->json([
            'success' => true,
            'hints' => [$additionalHint->hint], // 배열 형태로 반환
            'message' => '추가 힌트를 제공합니다.'
        ]);
    }

    /**
     * 레벨 완료
     */
    public function completeLevel(Request $request)
    {
        $user = $request->user();
        $game = UserPuzzleGame::where('user_id', $user->id)
            ->where('is_active', true)
            ->first();

        if (!$game) {
            return response()->json(['error' => '게임을 찾을 수 없습니다.'], 404);
        }

        // 레벨 클리어 조건 확인
        if (!$game->checkLevelClearCondition()) {
            $conditionMessage = $game->getLevelClearConditionMessage();
            return response()->json([
                'error' => '레벨 클리어 조건을 만족하지 않습니다.',
                'message' => $conditionMessage,
                'condition_not_met' => true
            ], 400);
        }

        // 레벨 클리어 기록 저장
        $gameData = [
            'score' => $request->input('score', 0),
            'play_time' => $request->input('play_time', 0),
            'hints_used' => $request->input('hints_used', 0),
            'words_found' => $request->input('words_found', 0),
            'total_words' => $request->input('total_words', 0),
            'accuracy' => $request->input('accuracy', 0),
        ];
        $game->recordLevelClear($gameData);

        // 다음 레벨로 진행 가능한지 확인
        if (!$game->canAdvanceToNextLevel()) {
            return response()->json([
                'message' => '축하합니다! 레벨을 완료했습니다.',
                'new_level' => $game->current_level,
                'success' => true,
                'no_next_level' => true
            ]);
        }

        // 다음 레벨로 진행
        $game->advanceToNextLevel();

        return response()->json([
            'message' => '축하합니다! 다음 레벨로 진행합니다.',
            'new_level' => $game->current_level,
            'success' => true
        ]);
    }

    /**
     * 틀린 답변 기록 (라라벨 웹과 동일)
     */
    private function recordWrongAnswer($userId, $wordId, $userAnswer, $correctAnswer, $category, $level)
    {
        try {
            DB::table('puzzle_wrong_answers')->insert([
                'user_id' => $userId,
                'word_id' => $wordId,
                'user_answer' => $userAnswer,
                'correct_answer' => $correctAnswer,
                'category' => $category,
                'level' => $level,
                'created_at' => now(),
                'updated_at' => now()
            ]);
        } catch (\Exception $e) {
            \Log::error('틀린 답변 기록 실패', [
                'error' => $e->getMessage(),
                'user_id' => $userId,
                'word_id' => $wordId
            ]);
        }
    }

    /**
     * 힌트 요청 (기존 메서드)
     */
    public function hint(Request $request)
    {
        $request->validate([
            'session_token' => 'required|string',
            'word_id' => 'required|integer',
            'hint_type' => 'nullable|string',
        ]);
        $session = PuzzleSessions::where('session_token', $request->input('session_token'))->firstOrFail();
        $word = PzWord::findOrFail($request->input('word_id'));
        $hintType = $request->input('hint_type', 'text');

        // 힌트 제한 체크
        $hintLimit = PuzzleHintLimits::where('level_id', $session->level_id)
            ->where('hint_type', $hintType)
            ->first();
        if (!$hintLimit || !$hintLimit->is_active) {
            return response()->json(['success' => false, 'message' => '힌트 사용 불가'], 403);
        }
        // TODO: 힌트 사용 횟수, 쿨다운 체크 필요

        // 힌트 제공 (예시: 단어의 첫 글자)
        $hint = mb_substr($word->word, 0, 1) . str_repeat(' _', mb_strlen($word->word) - 1);

        return response()->json([
            'success' => true,
            'data' => [
                'hint' => $hint,
            ]
        ]);
    }

    /**
     * 퍼즐 완료 여부 확인
     */
    public function checkCompletion(Request $request)
    {
        $request->validate([
            'session_token' => 'required|string',
        ]);
        $session = PuzzleSessions::where('session_token', $request->input('session_token'))->firstOrFail();
        $words = $session->selected_words ?? [];
        $progress = $session->user_progress ?? [];
        $completed = true;
        foreach ($words as $word) {
            if (empty($progress[$word['id']]['is_correct']) || !$progress[$word['id']]['is_correct']) {
                $completed = false;
                break;
            }
        }
        return response()->json([
            'success' => true,
            'data' => [
                'completed' => $completed,
            ]
        ]);
    }

    /**
     * 게임 상태 저장
     */
    public function saveGameState(Request $request)
    {
        $request->validate([
            'session_token' => 'required|string',
            'game_state' => 'required|array',
        ]);
        $session = PuzzleSessions::where('session_token', $request->input('session_token'))->firstOrFail();
        $session->game_state = $request->input('game_state');
        $session->save();
        return response()->json(['success' => true]);
    }

    /**
     * 게임 결과 제출
     */
    public function submitResult(Request $request)
    {
        $request->validate([
            'session_token' => 'required|string',
            'score' => 'required|integer',
            'accuracy' => 'required|numeric',
            'hints_used' => 'required|integer',
            'time_used' => 'required|integer',
        ]);
        $session = PuzzleSessions::where('session_token', $request->input('session_token'))->firstOrFail();
        $session->status = 'completed';
        $session->is_active = false;
        $session->save();
        // TODO: 기록 저장, 업적/레벨업 처리 등
        return response()->json(['success' => true]);
    }

    /**
     * 정답보기 (관리자 전용)
     */
    public function showAnswer(Request $request)
    {
        $request->validate([
            'word_id' => 'required|integer',
        ]);

        $user = $request->user();
        if (!$user) {
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        // 관리자 권한 확인
        if (!$user->is_admin) {
            return response()->json(['error' => '관리자만 접근 가능합니다.'], 403);
        }

        try {
            // word_id로 단어 조회
            $word = DB::table('pz_words')
                ->where('id', $request->word_id)
                ->where('is_active', true)
                ->first();

            if (!$word) {
                return response()->json(['error' => '단어를 찾을 수 없습니다.'], 404);
            }

            return response()->json([
                'success' => true,
                'answer' => $word->word,
            ]);
        } catch (\Exception $e) {
            \Log::error('정답보기 오류: ' . $e->getMessage());
            return response()->json(['error' => '정답을 불러오는데 실패했습니다.'], 500);
        }
    }

    /**
     * 오답 초과 시 정답보기 (모든 사용자 접근 가능)
     */
    public function showAnswerOnWrongCount(Request $request)
    {
        $request->validate([
            'word_id' => 'required|integer',
        ]);

        $user = $request->user();
        if (!$user) {
            return response()->json(['error' => '로그인이 필요합니다.'], 401);
        }

        try {
            // word_id로 단어 조회
            $word = DB::table('pz_words')
                ->where('id', $request->word_id)
                ->where('is_active', true)
                ->first();

            if (!$word) {
                return response()->json(['error' => '단어를 찾을 수 없습니다.'], 404);
            }

            return response()->json([
                'success' => true,
                'answer' => $word->word,
            ]);
        } catch (\Exception $e) {
            \Log::error('오답 초과 정답보기 오류: ' . $e->getMessage());
            return response()->json(['error' => '정답을 불러오는데 실패했습니다.'], 500);
        }
    }

    /**
     * 레벨 재시작
     */
}
