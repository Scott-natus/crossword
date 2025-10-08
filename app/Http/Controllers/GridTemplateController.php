<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\PuzzleLevel;
use App\Services\PuzzleGridTemplateService;
use Illuminate\Support\Facades\Auth;

class GridTemplateController extends Controller
{
    protected $templateService;

    public function __construct(PuzzleGridTemplateService $templateService)
    {
        $this->templateService = $templateService;
    }

    /**
     * 그리드 템플릿 생성 페이지
     */
    public function create()
    {
        // 모든 레벨 정보 가져오기
        $levels = DB::table('puzzle_levels')
            ->orderBy('level')
            ->get();

        return view('puzzle.grid-templates.create', compact('levels'));
    }

    /**
     * 레벨 선택 시 조건 정보 반환 (AJAX)
     */
    public function getLevelConditions(Request $request)
    {
        $levelId = $request->input('level_id');
        
        $level = DB::table('puzzle_levels')
            ->where('id', $levelId)
            ->first();

        if (!$level) {
            return response()->json([
                'success' => false,
                'message' => '레벨을 찾을 수 없습니다.'
            ]);
        }

        // 해당 레벨의 기존 템플릿들 가져오기
        $existingTemplates = DB::table('puzzle_grid_templates')
            ->where('level_id', $levelId)
            ->where('is_active', true)
            ->orderBy('created_at', 'desc')
            ->get()
            ->map(function ($template) {
                // 날짜 필드를 문자열로 변환
                $template->created_at = (string) $template->created_at;
                $template->updated_at = (string) $template->updated_at;
                return $template;
            });

        return response()->json([
            'success' => true,
            'level' => $level,
            'existing_templates' => $existingTemplates
        ]);
    }

    /**
     * 그리드 템플릿 저장
     */
    public function store(Request $request)
    {
        $request->validate([
            'level_id' => 'required|integer|exists:puzzle_levels,id',
            'grid_size' => 'required|integer|min:3|max:20',
            'grid_pattern' => 'required|array',
            'word_positions' => 'required|array',
            'word_numbering' => 'array',
            'word_count' => 'required|integer|min:1',
            'intersection_count' => 'required|integer|min:0'
        ]);

        try {
            // 레벨 정보 가져오기
            $level = DB::table('puzzle_levels')
                ->where('id', $request->level_id)
                ->first();

            if (!$level) {
                return response()->json([
                    'success' => false,
                    'message' => '레벨을 찾을 수 없습니다.'
                ]);
            }

            // 조건 검증
            if ($request->word_count != $level->word_count) {
                return response()->json([
                    'success' => false,
                    'message' => "단어 개수가 일치하지 않습니다. 레벨 {$level->level}은 {$level->word_count}개 단어가 필요합니다."
                ]);
            }

            if ($request->intersection_count < $level->intersection_count) {
                return response()->json([
                    'success' => false,
                    'message' => "교차점 개수가 부족합니다. 레벨 {$level->level}은 최소 {$level->intersection_count}개 교차점이 필요합니다. (현재: {$request->intersection_count}개)"
                ]);
            }

            // 동일한 그리드 패턴의 활성화된 템플릿이 있는지 체크
            $existingTemplates = DB::table('puzzle_grid_templates')
                ->where('level_id', $request->level_id)
                ->where('is_active', true)
                ->get();

            foreach ($existingTemplates as $existingTemplate) {
                $existingGridPattern = json_decode($existingTemplate->grid_pattern, true);
                $newGridPattern = $request->grid_pattern;
                
                // 그리드 크기가 다르면 건너뛰기
                if (count($existingGridPattern) !== count($newGridPattern)) {
                    continue;
                }
                
                // 그리드 패턴 비교
                $isSame = true;
                for ($i = 0; $i < count($existingGridPattern) && $isSame; $i++) {
                    for ($j = 0; $j < count($existingGridPattern[$i]); $j++) {
                        if ($existingGridPattern[$i][$j] !== $newGridPattern[$i][$j]) {
                            $isSame = false;
                            break;
                        }
                    }
                }
                
                if ($isSame) {
                    return response()->json([
                        'success' => false,
                        'message' => "동일한 그리드 패턴의 템플릿이 이미 존재합니다.\n\n템플릿명: {$existingTemplate->template_name}\n템플릿 ID: {$existingTemplate->id}\n\n다른 그리드 패턴을 사용하거나 기존 템플릿을 수정해주세요."
                    ]);
                }
            }

            // 템플릿 이름 자동 생성 - 같은 레벨 내에서 가장 큰 번호 + 1
            $existingTemplates = DB::table('puzzle_grid_templates')
                ->where('level_id', $request->level_id)
                ->where('template_name', 'LIKE', "레벨 {$level->level} 템플릿 #%")
                ->get();
            
            $maxNumber = 0;
            foreach ($existingTemplates as $template) {
                // "레벨 n 템플릿 #숫자" 패턴에서 숫자 추출
                if (preg_match('/레벨 \d+ 템플릿 #(\d+)/', $template->template_name, $matches)) {
                    $number = (int)$matches[1];
                    if ($number > $maxNumber) {
                        $maxNumber = $number;
                    }
                }
            }
            
            $templateName = "레벨 {$level->level} 템플릿 #" . ($maxNumber + 1);

            // word_positions의 id 값을 사용자가 선택한 번호로 업데이트
            $wordPositions = $request->word_positions;
            $wordNumbering = $request->word_numbering;
            
            if ($wordNumbering) {
                // 번호 매핑 생성
                $numberMapping = [];
                foreach ($wordNumbering as $item) {
                    $numberMapping[$item['word_id']] = $item['order'];
                }
                
                // word_positions의 id 값을 선택된 번호로 변경
                foreach ($wordPositions as &$word) {
                    if (isset($numberMapping[$word['id']])) {
                        $word['id'] = $numberMapping[$word['id']];
                    }
                }
            }

            // 템플릿 데이터 준비
            $template = [
                'level_id' => $request->level_id,
                'template_name' => $templateName,
                'grid_pattern' => $request->grid_pattern,
                'word_positions' => $wordPositions,
                'grid_width' => $request->grid_size,
                'grid_height' => $request->grid_size,
                'difficulty_rating' => $level->word_difficulty,
                'word_count' => $request->word_count,
                'intersection_count' => $request->intersection_count,
                'category' => 'custom',
                'description' => null,
                'is_active' => true
            ];

            // 템플릿 저장
            $templateId = $this->templateService->saveTemplate($template);

            return response()->json([
                'success' => true,
                'message' => '그리드 템플릿이 성공적으로 저장되었습니다.',
                'template_id' => $templateId
            ]);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '템플릿 저장 중 오류가 발생했습니다: ' . $e->getMessage()
            ]);
        }
    }

    /**
     * 그리드 템플릿 목록
     */
    public function index()
    {
        $templates = DB::table('puzzle_grid_templates')
            ->join('puzzle_levels', 'puzzle_grid_templates.level_id', '=', 'puzzle_levels.id')
            ->select(
                'puzzle_grid_templates.*',
                'puzzle_levels.level',
                'puzzle_levels.level_name'
            )
            ->where('puzzle_grid_templates.is_active', true)
            ->orderBy('puzzle_levels.level')
            ->orderBy('puzzle_grid_templates.created_at', 'desc')
            ->get()
            ->map(function ($template) {
                // created_at을 Carbon 객체로 변환
                $template->created_at = \Carbon\Carbon::parse($template->created_at);
                $template->updated_at = \Carbon\Carbon::parse($template->updated_at);
                return $template;
            });

        return view('puzzle.grid-templates.index', compact('templates'));
    }

    /**
     * 레벨별 템플릿 목록 조회 (API)
     */
    public function getTemplatesByLevel($levelId)
    {
        try {
            $templates = DB::table('puzzle_grid_templates')
                ->where('level_id', $levelId)
                ->where('is_active', true)
                ->orderBy('created_at', 'desc')
                ->get()
                ->map(function ($template) {
                    // 날짜 필드를 문자열로 변환
                    $template->created_at = (string) $template->created_at;
                    $template->updated_at = (string) $template->updated_at;
                    return $template;
                });

            return response()->json([
                'success' => true,
                'templates' => $templates
            ]);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '템플릿 목록 조회 중 오류가 발생했습니다: ' . $e->getMessage()
            ], 500);
        }
    }

    /**
     * 그리드 템플릿 상세 보기
     */
    public function show($id)
    {
        $template = DB::table('puzzle_grid_templates')
            ->join('puzzle_levels', 'puzzle_grid_templates.level_id', '=', 'puzzle_levels.id')
            ->select(
                'puzzle_grid_templates.*',
                'puzzle_levels.level',
                'puzzle_levels.level_name'
            )
            ->where('puzzle_grid_templates.id', $id)
            ->where('puzzle_grid_templates.is_active', true)
            ->first();

        if (!$template) {
            return redirect()->route('puzzle.grid-templates.index')
                ->with('error', '템플릿을 찾을 수 없습니다.');
        }

        // created_at을 Carbon 객체로 변환
        $template->created_at = \Carbon\Carbon::parse($template->created_at);
        $template->updated_at = \Carbon\Carbon::parse($template->updated_at);

        // JSON 데이터 파싱
        $gridPattern = json_decode($template->grid_pattern, true);
        $wordPositions = json_decode($template->word_positions, true);
        
        // 사용자가 설정한 번호 순서를 유지 (정렬하지 않음)
        \Log::info("템플릿 상세 조회", [
            'template_id' => $id,
            'word_positions' => $wordPositions
        ]);

        return view('puzzle.grid-templates.show', compact('template', 'gridPattern', 'wordPositions'));
    }

    /**
     * 그리드 템플릿 삭제
     */
    public function destroy($id)
    {
        try {
            $template = DB::table('puzzle_grid_templates')
                ->where('id', $id)
                ->where('is_active', true)
                ->first();

            if (!$template) {
                return response()->json([
                    'success' => false,
                    'message' => '템플릿을 찾을 수 없습니다.'
                ]);
            }

            // 논리적 삭제 (is_active = false)
            DB::table('puzzle_grid_templates')
                ->where('id', $id)
                ->update(['is_active' => false]);

            return response()->json([
                'success' => true,
                'message' => '템플릿이 성공적으로 삭제되었습니다.'
            ]);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '템플릿 삭제 중 오류가 발생했습니다: ' . $e->getMessage()
            ]);
        }
    }

    /**
     * 템플릿에서 단어 추출
     */
    public function extractWords(Request $request)
    {
        $templateId = $request->input('template_id');
        
        // 쿼리 로그 수집을 위한 배열 초기화
        $queryLog = [];
        
        try {
            // 1. 넘겨받은 템플릿 번호를 기반으로 템플릿 정보를 가져온다.
            $template = DB::table('puzzle_grid_templates')
                ->join('puzzle_levels', 'puzzle_grid_templates.level_id', '=', 'puzzle_levels.id')
                ->select(
                    'puzzle_grid_templates.*',
                    'puzzle_levels.word_difficulty',
                    'puzzle_levels.hint_difficulty',
                    'puzzle_levels.word_count',
                    'puzzle_levels.intersection_count'
                )
                ->where('puzzle_grid_templates.id', $templateId);
            
            // 쿼리 로그 수집
            $queryLog[] = [
                'type' => 'template_info',
                'sql' => $template->toSql(),
                'bindings' => $template->getBindings(),
                'description' => '템플릿 정보 조회'
            ];
            
            $template = $template->first();

            if (!$template) {
                return response()->json([
                    'success' => false,
                    'message' => '템플릿을 찾을 수 없습니다.',
                    'query_log' => $queryLog
                ]);
            }

            $wordPositions = json_decode($template->word_positions, true);
            $gridPattern = json_decode($template->grid_pattern, true);
            
            // 디버깅: 정렬 전 순서 출력
            \Log::info("정렬 전 word_positions 순서:", array_map(function($wp) { return $wp['id']; }, $wordPositions));
            
            // 2. 단어의 순서대로 정렬후 단어번호대로(루프) 처리한다.
            usort($wordPositions, function($a, $b) {
                return $a['id'] - $b['id'];
            });
            
            // 디버깅: 정렬 후 순서 출력
            \Log::info("정렬 후 word_positions 순서:", array_map(function($wp) { return $wp['id']; }, $wordPositions));

            // 최대 5회 재시도
            $maxRetries = 5;
            $retryCount = 0;
            $extractedWords = [];
            $confirmedWords = []; // 확정된 단어들을 저장 (word_id => word_info)

            while ($retryCount < $maxRetries) {
                $retryCount++;
                $extractedWords = [];
                $confirmedWords = []; // 확정된 단어들을 초기화
                $extractionFailed = false;

                \Log::info("단어 추출 시도 #{$retryCount} 시작");

                foreach ($wordPositions as $word) {
                    $wordId = $word['id'];
                    
                    // 이미 확정된 단어는 건너뛰기
                    if (isset($confirmedWords[$wordId])) {
                        continue;
                    }

                    // 3. 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
                    $intersections = $this->findIntersectionsWithConfirmedWords($word, $confirmedWords, $wordPositions);
                    
                    // 3-1. 교차점을 가지고 있지 않다면 조건에 따라 데이터베이스에서 쿼리를 해와서 결정한다.
                    if (empty($intersections)) {
                        $extractedWord = $this->extractIndependentWord($word, $template, $queryLog, $confirmedWords);
                        if ($extractedWord['word'] === '추출 실패') {
                            $extractionFailed = true;
                            break;
                        }
                        $extractedWords[] = [
                            'word_id' => $wordId,
                            'position' => $word,
                            'type' => 'no_intersection',
                            'extracted_word' => $extractedWord['word'],
                            'hint' => $extractedWord['hint']
                        ];
                        $confirmedWords[$wordId] = $extractedWord['word'];
                    } else {
                        // 3-2. 단어가 교차점을 가지고 있다면, 확정된 단어들의 교차점 음절을 추출한다.
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
                            
                            $currentSyllablePos = $this->getSyllablePosition($word, $intersection['position']);
                            
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
                        
                        // 3-3. 확정된 음절들과 매칭되는 단어를 추출하여 확정한다.
                        $extractedWord = $this->extractWordWithConfirmedSyllables($word, $template, $confirmedIntersectionSyllables, $queryLog, $confirmedWords);
                        
                        if ($extractedWord['success']) {
                            $extractedWords[] = [
                                'word_id' => $wordId,
                                'position' => $word,
                                'type' => 'intersection_connected',
                                'extracted_word' => $extractedWord['word'],
                                'hint' => $extractedWord['hint']
                            ];
                            $confirmedWords[$wordId] = $extractedWord['word'];
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

                return response()->json([
                    'success' => false,
                    'message' => "단어 ID {$failedWordId}에서 확정된 음절들과 매칭되는 단어를 찾을 수 없습니다.",
                    'query_log' => $queryLog,
                    'retry_count' => $retryCount
                ]);
            }

            // 4. 1번에서 추출한 조건(단어개수, 교차점)이 완료되면 루프를 마무리 한다.
            return response()->json([
                'success' => true,
                'template' => [
                    'id' => $template->id,
                    'template_name' => $template->template_name,
                    'level_id' => $template->level_id
                ],
                'extracted_words' => [
                    'grid_info' => [
                        'width' => $template->grid_width,
                        'height' => $template->grid_height,
                        'pattern' => $gridPattern
                    ],
                    'word_order' => $extractedWords
                ],
                'word_analysis' => [
                    'total_words' => count($wordPositions),
                    'extracted_words' => count($extractedWords),
                    'required_word_count' => $template->word_count,
                    'required_intersection_count' => $template->intersection_count,
                    'retry_count' => $retryCount
                ],
                'query_log' => $queryLog
            ]);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '단어 추출 중 오류가 발생했습니다: ' . $e->getMessage(),
                'query_log' => $queryLog
            ]);
        }
    }
    
    /**
     * 기존 템플릿들의 넘버링을 새로운 로직으로 업데이트
     */
    public function updateTemplateNumbering(Request $request)
    {
        try {
            $templateIds = $request->input('template_ids', [11, 12, 13, 14]);
            $results = [];
            
            foreach ($templateIds as $templateId) {
                $template = DB::table('puzzle_grid_templates')
                    ->where('id', $templateId)
                    ->where('is_active', true)
                    ->first();
                
                if (!$template) {
                    $results[] = [
                        'template_id' => $templateId,
                        'status' => 'not_found',
                        'message' => '템플릿을 찾을 수 없습니다.'
                    ];
                    continue;
                }
                
                $gridPattern = json_decode($template->grid_pattern, true);
                $wordPositions = json_decode($template->word_positions, true);
                
                if (!$gridPattern || !$wordPositions) {
                    $results[] = [
                        'template_id' => $templateId,
                        'status' => 'invalid_data',
                        'message' => '그리드 패턴 또는 단어 위치 데이터가 유효하지 않습니다.'
                    ];
                    continue;
                }
                
                // 새로운 넘버링 로직 적용
                $newWordOrder = $this->determineWordOrder($wordPositions, $gridPattern);
                
                // 새로운 넘버링 정보를 JSON으로 저장
                $newNumberingData = json_encode($newWordOrder, JSON_UNESCAPED_UNICODE);
                
                // 데이터베이스 업데이트
                DB::table('puzzle_grid_templates')
                    ->where('id', $templateId)
                    ->update([
                        'word_numbering' => $newNumberingData,
                        'updated_at' => now()
                    ]);
                
                $results[] = [
                    'template_id' => $templateId,
                    'template_name' => $template->template_name,
                    'status' => 'updated',
                    'message' => '넘버링이 성공적으로 업데이트되었습니다.',
                    'old_word_count' => count($wordPositions),
                    'new_word_count' => count($newWordOrder),
                    'new_numbering' => $newWordOrder
                ];
                
                \Log::info("템플릿 넘버링 업데이트 완료", [
                    'template_id' => $templateId,
                    'template_name' => $template->template_name,
                    'word_count' => count($newWordOrder)
                ]);
            }
            
            return response()->json([
                'success' => true,
                'message' => '템플릿 넘버링 업데이트가 완료되었습니다.',
                'results' => $results
            ], 200, [], JSON_INVALID_UTF8_SUBSTITUTE);
            
        } catch (\Exception $e) {
            \Log::error("템플릿 넘버링 업데이트 오류", [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ]);
            
            return response()->json([
                'success' => false,
                'message' => '템플릿 넘버링 업데이트 중 오류가 발생했습니다: ' . $e->getMessage(),
                'debug_info' => [
                    'error' => $e->getMessage(),
                    'trace' => $e->getTraceAsString()
                ]
            ], 500, [], JSON_INVALID_UTF8_SUBSTITUTE);
        }
    }

    /**
     * 그리드 템플릿 수정 (주로 번호 정보 수정)
     */
    public function update(Request $request, $id)
    {
        $request->validate([
            'level_id' => 'required|integer|exists:puzzle_levels,id',
            'grid_size' => 'required|integer|min:3|max:20',
            'grid_pattern' => 'required|array',
            'word_positions' => 'required|array',
            'word_numbering' => 'array',
            'word_count' => 'required|integer|min:1',
            'intersection_count' => 'required|integer|min:0'
        ]);

        try {
            // 기존 템플릿 확인
            $existingTemplate = DB::table('puzzle_grid_templates')
                ->where('id', $id)
                ->where('is_active', true)
                ->first();

            if (!$existingTemplate) {
                return response()->json([
                    'success' => false,
                    'message' => '수정할 템플릿을 찾을 수 없습니다.'
                ]);
            }

            // 레벨 정보 가져오기
            $level = DB::table('puzzle_levels')
                ->where('id', $request->level_id)
                ->first();

            if (!$level) {
                return response()->json([
                    'success' => false,
                    'message' => '레벨을 찾을 수 없습니다.'
                ]);
            }

            // 조건 검증
            if ($request->word_count != $level->word_count) {
                return response()->json([
                    'success' => false,
                    'message' => "단어 개수가 일치하지 않습니다. 레벨 {$level->level}은 {$level->word_count}개 단어가 필요합니다."
                ]);
            }

            if ($request->intersection_count < $level->intersection_count) {
                return response()->json([
                    'success' => false,
                    'message' => "교차점 개수가 부족합니다. 레벨 {$level->level}은 최소 {$level->intersection_count}개 교차점이 필요합니다. (현재: {$request->intersection_count}개)"
                ]);
            }

            // word_positions의 id 값을 사용자가 선택한 번호로 업데이트
            $wordPositions = $request->word_positions;
            $wordNumbering = $request->word_numbering;
            
            if ($wordNumbering) {
                // 번호 매핑 생성
                $numberMapping = [];
                foreach ($wordNumbering as $item) {
                    $numberMapping[$item['word_id']] = $item['order'];
                }
                
                // word_positions의 id 값을 선택된 번호로 변경
                foreach ($wordPositions as &$word) {
                    if (isset($numberMapping[$word['id']])) {
                        $word['id'] = $numberMapping[$word['id']];
                    }
                }
            }

            // 템플릿 업데이트
            $updateData = [
                'word_positions' => json_encode($wordPositions),
                'updated_at' => now()
            ];

            DB::table('puzzle_grid_templates')
                ->where('id', $id)
                ->update($updateData);

            return response()->json([
                'success' => true,
                'message' => '템플릿 번호 정보가 성공적으로 수정되었습니다.',
                'template_id' => $id
            ]);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '템플릿 수정 중 오류가 발생했습니다: ' . $e->getMessage()
            ]);
        }
    }

    /**
     * 현재 단어와 이미 확정된 단어들 사이의 교차점 찾기
     */
    private function findIntersectionsWithConfirmedWords($currentWord, $confirmedWords, $allWordPositions)
    {
        $intersections = [];
        
        // 이미 확정된 단어들과만 교차점 확인
        foreach ($confirmedWords as $confirmedWordId => $confirmedWordText) {
            $confirmedWordPosition = $this->findWordById($confirmedWordId, $allWordPositions);
            if (!$confirmedWordPosition) continue;
            
            $intersection = $this->findIntersection($currentWord, $confirmedWordPosition);
            if ($intersection) {
                $intersections[] = [
                    'position' => $intersection,
                    'connected_word_id' => $confirmedWordId,
                    'connected_word' => $confirmedWordPosition
                ];
            }
        }
        
        return $intersections;
    }

    /**
     * 두 단어 사이의 교차점 찾기
     */
    private function findIntersection($word1, $word2)
    {
        // 같은 방향인 경우 연결점 확인
        if ($word1['direction'] == $word2['direction']) {
            return $this->findConnectionPoint($word1, $word2);
        }
        
        // 가로-세로 교차 확인
        $horizontal = $word1['direction'] == 'horizontal' ? $word1 : $word2;
        $vertical = $word1['direction'] == 'vertical' ? $word1 : $word2;
        
        // 교차점 좌표 계산
        $intersectX = $vertical['start_x'];
        $intersectY = $horizontal['start_y'];
        
        // 교차점이 두 단어 범위 내에 있는지 확인
        if ($intersectX >= $horizontal['start_x'] && $intersectX <= $horizontal['end_x'] &&
            $intersectY >= $vertical['start_y'] && $intersectY <= $vertical['end_y']) {
            
            return [
                'x' => $intersectX,
                'y' => $intersectY,
                'horizontal_word_id' => $horizontal['id'],
                'vertical_word_id' => $vertical['id']
            ];
        }
        
        return null;
    }

    /**
     * 같은 방향의 두 단어 사이의 연결점 찾기
     */
    private function findConnectionPoint($word1, $word2)
    {
        // 가로 방향 연결점 확인
        if ($word1['direction'] == 'horizontal' && $word2['direction'] == 'horizontal') {
            // 같은 y좌표에서 연결되는지 확인
            if ($word1['start_y'] == $word2['start_y']) {
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if ($word1['end_x'] + 1 == $word2['start_x']) {
                    return [
                        'x' => $word1['end_x'],
                        'y' => $word1['start_y'],
                        'word1_end' => true,
                        'word2_start' => true
                    ];
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if ($word2['end_x'] + 1 == $word1['start_x']) {
                    return [
                        'x' => $word2['end_x'],
                        'y' => $word2['start_y'],
                        'word1_start' => true,
                        'word2_end' => true
                    ];
                }
            }
        }
        
        // 세로 방향 연결점 확인
        if ($word1['direction'] == 'vertical' && $word2['direction'] == 'vertical') {
            // 같은 x좌표에서 연결되는지 확인
            if ($word1['start_x'] == $word2['start_x']) {
                // word1의 끝점과 word2의 시작점이 연결되는지 확인
                if ($word1['end_y'] + 1 == $word2['start_y']) {
                    return [
                        'x' => $word1['start_x'],
                        'y' => $word1['end_y'],
                        'word1_end' => true,
                        'word2_start' => true
                    ];
                }
                // word2의 끝점과 word1의 시작점이 연결되는지 확인
                if ($word2['end_y'] + 1 == $word1['start_y']) {
                    return [
                        'x' => $word2['start_x'],
                        'y' => $word2['end_y'],
                        'word1_start' => true,
                        'word2_end' => true
                    ];
                }
            }
        }
        
        return null;
    }

    /**
     * 독립 단어 추출 (교차점이 없는 단어)
     */
    private function extractIndependentWord($word, $template, &$queryLog, $confirmedWords = [])
    {
        $length = $word['length'];
        
        // 레벨에 설정된 단어 난이도 사용
        $wordDifficulty = $template->word_difficulty;
        
        // 새로운 난이도 규칙 적용
        $allowedDifficulties = $this->getAllowedDifficulties($wordDifficulty);
        
        // 2.5 단어 추출의 쿼리는 랜덤을 기본으로 한다.
        $query = DB::table('pz_words as a')
            ->join('pz_hints as b', 'a.id', '=', 'b.word_id')
            ->select('a.word', 'b.hint_text as hint')
            ->where('a.length', $length)
            ->whereIn('a.difficulty', $allowedDifficulties) // 새로운 난이도 규칙 적용
            ->where('a.is_active', true);
        
        // 이미 사용된 단어들 제외
        if (!empty($confirmedWords)) {
            $excludedWords = array_values($confirmedWords);
            $query->whereNotIn('a.word', $excludedWords);
        }
        
        $query->orderByRaw('RANDOM()');
        
        // 쿼리 로그 수집
        $allowedDifficultiesText = implode(',', $allowedDifficulties);
        $queryLog[] = [
            'type' => 'independent_word',
            'sql' => $query->toSql(),
            'bindings' => $query->getBindings(),
            'description' => "독립 단어 추출 (길이: {$length}, 허용난이도: {$allowedDifficultiesText})"
        ];
        
        $result = $query->first();
        
        if (!$result) {
            return [
                'word' => '추출 실패',
                'hint' => '조건에 맞는 단어를 찾을 수 없습니다.',
                'query_log' => $queryLog
            ];
        }
        
        return [
            'word' => $result->word,
            'hint' => $result->hint,
            'query_log' => $queryLog
        ];
    }

    /**
     * 새로운 난이도 규칙에 따른 허용 난이도 반환
     */
    private function getAllowedDifficulties($levelDifficulty)
    {
        switch ($levelDifficulty) {
            case 1:
                return [1, 2]; // 레벨 1: 난이도 1,2
            case 2:
                return [1, 2, 3]; // 레벨 2: 난이도 1,2,3
            case 3:
                return [2, 3, 4]; // 레벨 3: 난이도 2,3,4
            case 4:
                return [3, 4, 5]; // 레벨 4: 난이도 3,4,5
            case 5:
                return [4, 5]; // 레벨 5: 난이도 4,5
            default:
                return [1, 2, 3, 4, 5]; // 기본값: 모든 난이도
        }
    }

    /**
     * 난이도 규칙 텍스트 생성
     */
    private function getDifficultyRuleText($levelDifficulty)
    {
        switch ($levelDifficulty) {
            case 1:
                return "난이도 1,2";
            case 2:
                return "난이도 1,2,3";
            case 3:
                return "난이도 2,3,4";
            case 4:
                return "난이도 3,4,5";
            case 5:
                return "난이도 4,5";
            default:
                return "모든 난이도";
        }
    }

    /**
     * 음절 위치 계산
     */
    private function getSyllablePosition($word, $intersection)
    {
        if ($word['direction'] === 'horizontal') {
            return $intersection['x'] - $word['start_x'] + 1;
        } else {
            return $intersection['y'] - $word['start_y'] + 1;
        }
    }

    /**
     * ID로 단어 찾기
     */
    private function findWordById($id, $wordPositions)
    {
        foreach ($wordPositions as $word) {
            if ($word['id'] == $id) {
                return $word;
            }
        }
        return null;
    }

    /**
     * 단어 위치 정보를 분석하여 교차점 정보를 실시간으로 계산
     */
    private function analyzeWordPositions($wordPositions)
    {
        $analysis = [];
        
        foreach ($wordPositions as $word) {
            $wordId = $word['id'];
            $intersections = [];
            
            // 다른 모든 단어와의 교차점 찾기
            foreach ($wordPositions as $otherWord) {
                if ($word['id'] === $otherWord['id']) continue;
                
                $intersection = $this->findIntersection($word, $otherWord);
                if ($intersection) {
                    $intersections[] = [
                        'position' => $intersection,
                        'connected_word_id' => $otherWord['id'],
                        'connected_word' => $otherWord
                    ];
                }
            }
            
            // 교차점을 좌에서 우로, 위에서 아래로 순서 정렬
            usort($intersections, function($a, $b) {
                $posA = $a['position'];
                $posB = $b['position'];
                
                if ($posA['y'] !== $posB['y']) {
                    return $posA['y'] - $posB['y']; // y 좌표 우선
                }
                return $posA['x'] - $posB['x']; // x 좌표 차선
            });
            
            $analysis[$wordId] = [
                'word' => $word,
                'intersections' => $intersections
            ];
        }
        
        return $analysis;
    }

    /**
     * 확정된 음절 정보를 기반으로 단어 추출
     */
    private function extractWordWithConfirmedSyllables($word, $template, $confirmedSyllables, &$queryLog, $confirmedWords = [])
    {
        $length = $word['length'];
        
        if (empty($confirmedSyllables)) {
            return [
                'success' => false,
                'message' => "확정된 음절 정보가 없습니다.",
                'word_id' => $word['id'],
                'query_log' => $queryLog
            ];
        }
        
        // 레벨에 설정된 단어 난이도 사용
        $wordDifficulty = $template->word_difficulty;
        
        // 새로운 난이도 규칙 적용
        $allowedDifficulties = $this->getAllowedDifficulties($wordDifficulty);
        
        // 확정된 음절들을 기반으로 조건 생성
        $query = DB::table('pz_words as a')
            ->join('pz_hints as b', 'a.id', '=', 'b.word_id')
            ->select('a.word', 'b.hint_text as hint')
            ->where('a.length', $length)
            ->whereIn('a.difficulty', $allowedDifficulties) // 새로운 난이도 규칙 적용
            ->where('a.is_active', true);
        
        // 교차점을 공유하는 다른 단어들과 같은 단어 제외
        if (!empty($confirmedWords)) {
            $excludedWords = array_values($confirmedWords);
            $query->whereNotIn('a.word', $excludedWords);
        }
        
        // 각 확정된 음절에 대한 조건 추가
        \Log::info("교차점 음절 일치 조건 추가", [
            'word_id' => $word['id'],
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
        
        // 쿼리 로그 수집
        $syllableConditions = [];
        foreach ($confirmedSyllables as $syllableInfo) {
            $syllableConditions[] = "{$syllableInfo['position']}번째='{$syllableInfo['syllable']}'";
        }
        $syllableDesc = implode(', ', $syllableConditions);
        
        $excludedWordsDesc = '';
        if (!empty($confirmedWords)) {
            $excludedWordsDesc = ', 제외단어: ' . implode(', ', array_values($confirmedWords));
        }
        
        $allowedDifficultiesText = implode(',', $allowedDifficulties);
        $queryLog[] = [
            'type' => 'intersection_word',
            'sql' => $query->toSql(),
            'bindings' => $query->getBindings(),
            'description' => "교차점 단어 추출 (길이: {$length}, 허용난이도: {$allowedDifficultiesText}, 확정음절: {$syllableDesc}{$excludedWordsDesc})"
        ];
        
        $result = $query->first();
        
        if (!$result) {
            return [
                'success' => false,
                'message' => "단어 ID {$word['id']}에서 확정된 음절들과 매칭되는 단어를 찾을 수 없습니다.",
                'word_id' => $word['id'],
                'required_syllables' => $confirmedSyllables,
                'query_log' => $queryLog
            ];
        }
        
        return [
            'success' => true,
            'word' => $result->word,
            'hint' => $result->hint,
            'query_log' => $queryLog
        ];
    }

    /**
     * 템플릿 상세 정보를 JSON으로 반환
     */
    public function showJson($id)
    {
        $template = DB::table('puzzle_grid_templates')
            ->where('id', $id)
            ->where('is_active', true)
            ->first();

        if (!$template) {
            return response()->json(['success' => false, 'message' => '템플릿을 찾을 수 없습니다.']);
        }

        $template->created_at = (string) $template->created_at;
        $template->updated_at = (string) $template->updated_at;

        return response()->json(['success' => true, 'template' => $template]);
    }

    /**
     * 레벨별 샘플 템플릿 데이터 생성
     */
    private function generateSampleTemplates($level)
    {
        $samples = [];
        
        // 레벨별 기본 조건
        $wordCount = $level->word_count;
        $intersectionCount = $level->intersection_count;
        
        // 레벨별 최적화된 샘플들 (단어 수, 교차점, 그리드 크기 계산 기반)
        if ($level->level == 1) {
            // 레벨 1: 5단어, 2교차점, 5x5 그리드 (검은색 13-15칸)
            // 샘플 1: 기본 십자형 (보강된 버전)
            $crossGrid = $this->generateCrossPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '기본 십자형',
                'description' => '가로와 세로가 교차하는 기본적인 형태 (검은색 13칸, 흰색 12칸)',
                'grid' => $crossGrid,
                'word_positions' => $this->extractWordPositions($crossGrid),
                'pattern' => 'cross_basic',
                'black_cells' => $this->countBlackCells($crossGrid),
                'white_cells' => $this->countWhiteCells($crossGrid)
            ];
            
            // 샘플 2: L자형 패턴 (보강된 버전)
            $lGrid = $this->generateLPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => 'L자형 패턴',
                'description' => 'L자 모양으로 배치된 단어들 (검은색 14칸, 흰색 11칸)',
                'grid' => $lGrid,
                'word_positions' => $this->extractWordPositions($lGrid),
                'pattern' => 'l_shape',
                'black_cells' => $this->countBlackCells($lGrid),
                'white_cells' => $this->countWhiteCells($lGrid)
            ];
            
            // 샘플 3: 그물형 패턴 (보강된 버전)
            $netGrid = $this->generateNetPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '그물형 패턴',
                'description' => '여러 교차점을 가진 복합형 패턴 (검은색 15칸, 흰색 10칸)',
                'grid' => $netGrid,
                'word_positions' => $this->extractWordPositions($netGrid),
                'pattern' => 'net',
                'black_cells' => $this->countBlackCells($netGrid),
                'white_cells' => $this->countWhiteCells($netGrid)
            ];
            
        } elseif ($level->level == 2) {
            // 레벨 2: 6단어, 2교차점, 6x6 그리드 (검은색 16-18칸)
            $crossGrid = $this->generateCrossPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '레벨 2 기본형',
                'description' => '6단어 2교차점 기본 패턴 (검은색 16칸, 흰색 20칸)',
                'grid' => $crossGrid,
                'word_positions' => $this->extractWordPositions($crossGrid),
                'pattern' => 'level2_basic',
                'black_cells' => $this->countBlackCells($crossGrid),
                'white_cells' => $this->countWhiteCells($crossGrid)
            ];
            
        } elseif ($level->level == 3) {
            // 레벨 3: 8단어, 3교차점, 6x6 그리드 (검은색 21-25칸)
            $netGrid = $this->generateNetPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '레벨 3 복합형',
                'description' => '8단어 3교차점 복합 패턴 (검은색 22칸, 흰색 14칸)',
                'grid' => $netGrid,
                'word_positions' => $this->extractWordPositions($netGrid),
                'pattern' => 'level3_complex',
                'black_cells' => $this->countBlackCells($netGrid),
                'white_cells' => $this->countWhiteCells($netGrid)
            ];
            
        } else {
            // 기타 레벨용 기본 샘플들 (보강된 버전)
            $crossGrid = $this->generateCrossPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '기본 십자형',
                'description' => '가로와 세로가 교차하는 기본적인 형태',
                'grid' => $crossGrid,
                'word_positions' => $this->extractWordPositions($crossGrid),
                'pattern' => 'cross',
                'black_cells' => $this->countBlackCells($crossGrid),
                'white_cells' => $this->countWhiteCells($crossGrid)
            ];
            
            $lGrid = $this->generateLPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => 'L자형 패턴',
                'description' => 'L자 모양으로 배치된 단어들',
                'grid' => $lGrid,
                'word_positions' => $this->extractWordPositions($lGrid),
                'pattern' => 'l_shape',
                'black_cells' => $this->countBlackCells($lGrid),
                'white_cells' => $this->countWhiteCells($lGrid)
            ];
            
            $netGrid = $this->generateNetPattern($wordCount, $intersectionCount);
            $samples[] = [
                'name' => '그물형 패턴',
                'description' => '여러 교차점을 가진 복합형 패턴',
                'grid' => $netGrid,
                'word_positions' => $this->extractWordPositions($netGrid),
                'pattern' => 'net',
                'black_cells' => $this->countBlackCells($netGrid),
                'white_cells' => $this->countWhiteCells($netGrid)
            ];
        }
        
        return $samples;
    }
    
    /**
     * 그리드에서 단어 위치 정보 추출
     */
    private function extractWordPositions($grid)
    {
        $size = count($grid);
        $wordPositions = [];
        $wordId = 1;
        $visited = array_fill(0, $size, array_fill(0, $size, false));
        
        // 가로 단어 찾기
        for ($i = 0; $i < $size; $i++) {
            for ($j = 0; $j < $size; $j++) {
                if ($grid[$i][$j] == 2 && !$visited[$i][$j]) {
                    // 가로 방향으로 연속된 검은색 칸 확인
                    $startJ = $j;
                    $endJ = $j;
                    $k = $j;
                    while ($k < $size && $grid[$i][$k] == 2) {
                        $visited[$i][$k] = true;
                        $endJ = $k;
                        $k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if ($endJ - $startJ + 1 >= 2) {
                        $wordPositions[] = [
                            'id' => $wordId++,
                            'start_x' => $startJ,
                            'start_y' => $i,
                            'end_x' => $endJ,
                            'end_y' => $i,
                            'direction' => 'horizontal',
                            'length' => $endJ - $startJ + 1
                        ];
                    }
                }
            }
        }
        
        // 세로 단어 찾기
        for ($j = 0; $j < $size; $j++) {
            for ($i = 0; $i < $size; $i++) {
                if ($grid[$i][$j] == 2 && !$visited[$i][$j]) {
                    // 세로 방향으로 연속된 검은색 칸 확인
                    $startI = $i;
                    $endI = $i;
                    $k = $i;
                    while ($k < $size && $grid[$k][$j] == 2) {
                        $visited[$k][$j] = true;
                        $endI = $k;
                        $k++;
                    }
                    
                    // 2칸 이상이면 단어로 인정
                    if ($endI - $startI + 1 >= 2) {
                        $wordPositions[] = [
                            'id' => $wordId++,
                            'start_x' => $j,
                            'start_y' => $startI,
                            'end_x' => $j,
                            'end_y' => $endI,
                            'direction' => 'vertical',
                            'length' => $endI - $startI + 1
                        ];
                    }
                }
            }
        }
        
        return $wordPositions;
    }
    
    /**
     * 검은색 칸 개수 계산
     */
    private function countBlackCells($grid)
    {
        $count = 0;
        $size = count($grid);
        
        for ($i = 0; $i < $size; $i++) {
            for ($j = 0; $j < $size; $j++) {
                if ($grid[$i][$j] == 2) {
                    $count++;
                }
            }
        }
        
        return $count;
    }
    
    /**
     * 흰색 칸 개수 계산
     */
    private function countWhiteCells($grid)
    {
        $count = 0;
        $size = count($grid);
        
        for ($i = 0; $i < $size; $i++) {
            for ($j = 0; $j < $size; $j++) {
                if ($grid[$i][$j] == 1) {
                    $count++;
                }
            }
        }
        
        return $count;
    }

    /**
     * 레벨별 샘플 템플릿 조회
     */
    public function getSampleTemplates(Request $request)
    {
        try {
            $levelId = $request->input('level_id');
            
            if (!$levelId) {
                return response()->json([
                    'success' => false,
                    'message' => '레벨 ID가 필요합니다.'
                ], 400);
            }
            
            $level = PuzzleLevel::find($levelId);
            
            if (!$level) {
                return response()->json([
                    'success' => false,
                    'message' => '해당 레벨을 찾을 수 없습니다.'
                ], 404);
            }
            
            $samples = $this->generateSampleTemplates($level);
            
            return response()->json([
                'success' => true,
                'level' => $level,
                'samples' => $samples
            ]);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '샘플 템플릿 조회 중 오류가 발생했습니다: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * 샘플 템플릿을 기반으로 새 템플릿 생성
     */
    public function createFromSample(Request $request)
    {
        try {
            $levelId = $request->input('level_id');
            $sampleIndex = $request->input('sample_index');
            $templateName = $request->input('template_name');
            
            if (!$levelId || !isset($sampleIndex) || !$templateName) {
                return response()->json([
                    'success' => false,
                    'message' => '필수 파라미터가 누락되었습니다.'
                ], 400);
            }
            
            $level = PuzzleLevel::find($levelId);
            
            if (!$level) {
                return response()->json([
                    'success' => false,
                    'message' => '해당 레벨을 찾을 수 없습니다.'
                ], 404);
            }
            
            $samples = $this->generateSampleTemplates($level);
            
            if (!isset($samples[$sampleIndex])) {
                return response()->json([
                    'success' => false,
                    'message' => '유효하지 않은 샘플 인덱스입니다.'
                ], 400);
            }
            
            $sample = $samples[$sampleIndex];
            $grid = $sample['grid'];
            $size = count($grid);
            
            // 그리드 템플릿 생성
            $template = new GridTemplate();
            $template->level_id = $levelId;
            $template->template_name = $templateName;
            $template->grid_width = $size;
            $template->grid_height = $size;
            $template->grid_data = json_encode($grid);
            $template->word_count = $this->countWords($grid);
            $template->intersection_count = $this->countIntersections($grid);
            $template->category = 'sample_' . $sample['pattern'];
            $template->created_by = Auth::user()->email;
            $template->save();
            
            return response()->json([
                'success' => true,
                'message' => '샘플 템플릿이 성공적으로 생성되었습니다.',
                'template' => $template
            ]);
            
        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => '템플릿 생성 중 오류가 발생했습니다: ' . $e->getMessage()
            ], 500);
        }
    }
    
    /**
     * 그리드에서 교차점 수 계산
     */
    private function countIntersections($grid)
    {
        $count = 0;
        $size = count($grid);
        
        for ($i = 0; $i < $size; $i++) {
            for ($j = 0; $j < $size; $j++) {
                if ($grid[$i][$j] == 1) {
                    // 가로와 세로 방향 모두에 단어가 있는지 확인
                    $hasHorizontal = false;
                    $hasVertical = false;
                    
                    // 가로 방향 확인
                    if ($j > 0 && $grid[$i][$j-1] == 1) $hasHorizontal = true;
                    if ($j < $size-1 && $grid[$i][$j+1] == 1) $hasHorizontal = true;
                    
                    // 세로 방향 확인
                    if ($i > 0 && $grid[$i-1][$j] == 1) $hasVertical = true;
                    if ($i < $size-1 && $grid[$i+1][$j] == 1) $hasVertical = true;
                    
                    if ($hasHorizontal && $hasVertical) {
                        $count++;
                    }
                }
            }
        }
        
        return $count;
    }
} 