@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row">
        <div class="col-md-8">
            <div class="card">
                <div class="card-header d-flex flex-column align-items-center">
                    <h3 class="text-center mb-2">K-CrossWord</h3>
                    <div class="w-100 d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">
                            레벨 {{ $game->current_level }}
                            @if($level && $level->level_name)
                                <span class="ms-2 text-secondary">({{ $level->level_name }})</span>
                            @endif
                        </h5>
                        <div>
                            <span class="badge bg-primary me-2">정답: {{ $game->current_level_correct_answers }}</span>
                            <span class="badge bg-danger me-2">오답: {{ $game->current_level_wrong_answers }}</span>
                            @if($level && $level->time_limit > 0)
                                <span class="badge bg-warning" id="timer">남은시간: <span id="time-left">{{ $level->time_limit }}</span>초</span>
                            @endif
                        </div>
                    </div>
                </div>
                @if($level && $level->clear_condition > 1)
                    <div class="card-body border-bottom">
                        <div class="alert alert-info mb-0 text-center">
                            <i class="fas fa-info-circle me-2"></i>
                            <strong>클리어 조건/횟수 :</strong> <span id="clear-condition-display">({{ $clearCount }}/{{ $level->clear_condition }})</span>
                        </div>
                    </div>
                @endif
                <div class="card-body">
                    <div id="puzzle-grid" class="text-center">
                        <!-- 퍼즐 그리드가 여기에 렌더링됩니다 -->
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card">
                <div class="card-header">
                    <h6 class="mb-0">단어 입력</h6>
                </div>
                <div class="card-body">
                    <div id="word-input-section" style="display: none;">
                        <div class="mb-3">
                            <label class="form-label">힌트:</label>
                            <div id="current-hint" class="alert alert-info"></div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">정답 입력:</label>
                            <input type="text" id="answer-input" class="form-control" placeholder="단어를 입력하세요" autocomplete="off">
                        </div>
                        <div class="d-flex gap-2">
                            <button type="button" id="check-answer-btn" class="btn btn-primary">확인</button>
                            <button type="button" id="show-hint-btn" class="btn btn-secondary">힌트보기</button>
                            @if(Auth::check() && Auth::user()->is_admin)
                                <button type="button" id="show-answer-btn" class="btn btn-warning">정답보기</button>
                            @endif
                        </div>
                        <div id="result-message" class="mt-3"></div>
                        <div id="additional-hints" class="mt-3" style="display: none;">
                            <h6>추가 힌트:</h6>
                            <div id="hints-list"></div>
                        </div>
                    </div>
                    <div id="game-info">
                        <p>번호를 클릭하여 단어를 입력하세요.</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- 게임 완료 모달 -->
<div class="modal fade" id="levelCompleteModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">레벨 완료</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p id="level-complete-message">모든 단어를 맞추셨습니다!</p>
                <p id="level-complete-sub-message">다음 레벨로 진행하시겠습니까?</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button type="button" id="next-level-btn" class="btn btn-primary">다음 레벨</button>
            </div>
        </div>
    </div>
</div>

<!-- 게임오버 모달 -->
<div class="modal fade" id="gameOverModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">게임오버</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>시간이 초과되었습니다.</p>
                <p>5분 후 재시도 가능합니다.</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="location.href='{{ route('main') }}'">메인으로</button>
            </div>
        </div>
    </div>
</div>

<!-- 오답 초과 모달 -->
<div class="modal fade" id="wrongCountExceededModal" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-danger text-white">
                <h5 class="modal-title">
                    <i class="fas fa-exclamation-triangle me-2"></i>오답 회수 초과
                </h5>
            </div>
            <div class="modal-body text-center">
                <div class="alert alert-danger">
                    <h4>오답회수가 초과했습니다!</h4>
                    <p class="mb-0">레벨을 다시 시작합니다.</p>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="location.reload()">레벨 재시작</button>
            </div>
        </div>
    </div>
</div>


@endsection

@push('styles')
<style>
.puzzle-cell {
    width: 40px;
    height: 40px;
    border: 1px solid #ccc;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin: 1px;
    cursor: pointer;
    font-weight: bold;
    position: relative;
}

.puzzle-cell.black {
    background-color: #000;
    color: white;
}

.puzzle-cell.answered {
    background-color: #6c757d;
    color: white;
}

.puzzle-cell:hover {
    background-color: #e9ecef;
}

.puzzle-cell.black:hover {
    background-color: #333;
}

.cell-number {
    position: absolute;
    top: 2px;
    left: 2px;
    font-size: 10px;
    color: #666;
}

.puzzle-cell.black .cell-number {
    color: #fff;
}

.puzzle-cell.answered .cell-number {
    color: #fff;
}

/* 템플릿 관리와 동일한 그리드 스타일 */
.grid-cell-number {
    position: relative;
    transition: all 0.2s ease;
}

.grid-cell-number:hover {
    transform: scale(1.05);
    z-index: 10;
}

.word-number {
    background: #ff6b6b;
    color: white;
    border-radius: 50%;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: bold;
    box-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.grid-container {
    background: white;
    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
    border-radius: 8px;
    overflow: hidden;
}

/* 자동완성 비활성화 */
#answer-input {
    -webkit-autocomplete: none;
    -moz-autocomplete: none;
    -ms-autocomplete: none;
    autocomplete: none;
}

/* 브라우저별 자동완성 스타일 숨기기 */
#answer-input:-webkit-autofill,
#answer-input:-webkit-autofill:hover,
#answer-input:-webkit-autofill:focus,
#answer-input:-webkit-autofill:active {
    -webkit-box-shadow: 0 0 0 30px white inset !important;
    -webkit-text-fill-color: #000 !important;
}

/* 모바일 최적화 스타일 */
@media (max-width: 768px) {
    .container {
        padding: 0 10px;
    }
    
    .card-header h3 {
        font-size: 1.5rem;
    }
    
    .card-header h5 {
        font-size: 1rem;
    }
    
    .badge {
        font-size: 0.75rem;
        padding: 0.25rem 0.5rem;
    }
    
    .puzzle-cell {
        width: 35px;
        height: 35px;
        font-size: 14px;
    }
    
    .cell-number {
        font-size: 8px;
    }
    
    .word-number {
        width: 16px;
        height: 16px;
        font-size: 8px;
    }
    
    .col-md-4 {
        margin-top: 1rem;
    }
    
    .btn {
        padding: 0.375rem 0.75rem;
        font-size: 0.875rem;
    }
    
    .form-control {
        font-size: 0.875rem;
    }
}

/* 매우 작은 모바일 화면 (320px 이하) */
@media (max-width: 320px) {
    .puzzle-cell {
        width: 30px;
        height: 30px;
        font-size: 12px;
    }
    
    .cell-number {
        font-size: 7px;
    }
    
    .word-number {
        width: 14px;
        height: 14px;
        font-size: 7px;
    }
}
</style>
@endpush

@push('scripts')
<script>
// 게스트 ID 관리
(function() {
    let guestId = localStorage.getItem('guest_id');
    const urlParams = new URLSearchParams(window.location.search);
    if (!guestId && urlParams.get('guest_id')) {
        guestId = urlParams.get('guest_id');
        localStorage.setItem('guest_id', guestId);
    }
    window.GUEST_ID = guestId || null;
})();
// 이후 window.GUEST_ID를 모든 API 요청에 포함
// 예시: $.post('/api/puzzle-level', { guest_id: window.GUEST_ID, ... })

let currentWordId = null;
let currentHintId = null;
let baseHintId = null;
let currentTemplateWords = [];
let currentTemplate = null; // 현재 템플릿 저장
let answeredWords = new Set();
let answeredWordsData = new Map(); // 정답 데이터 저장 (wordId -> answer) - 암호화된 형태로 저장
let gameTimer = null;
let timeLeft = {{ $level ? $level->time_limit : 0 }};

$(document).ready(function() {
    loadTemplate();
    
    if (timeLeft > 0) {
        startTimer();
    }
    
    // 페이지 로드 시 클리어 조건 업데이트
    updateClearCondition();
});

function loadTemplate() {
    $.get('{{ route("puzzle-game.get-template") }}')
        .done(function(data) {
            currentTemplate = data.template; // 템플릿 저장
            currentTemplateWords = data.template.words;
            renderPuzzleGrid(data.template);
            
            // Wordle처럼 게임 상태 복원
            if (data.is_restored && data.game_state) {
                console.log('기존 게임 상태 복원:', data.game_state);
                restoreGameState(data.game_state, data.answered_words_with_answers);
            } else {
                console.log('새로운 퍼즐 시작');
            }
        })
        .fail(function(xhr) {
            alert('템플릿을 불러올 수 없습니다.');
        });
}

// 게임 상태 복원 함수
function restoreGameState(gameState, answeredWordsWithAnswers) {
    if (!gameState) return;
    
    // 정답 단어 복원
    if (gameState.answered_words && Array.isArray(gameState.answered_words)) {
        gameState.answered_words.forEach(function(wordId) {
            answeredWords.add(wordId);
            // 서버에서 받은 정답 단어 정보 사용
            if (answeredWordsWithAnswers && answeredWordsWithAnswers[wordId]) {
                answeredWordsData.set(wordId, answeredWordsWithAnswers[wordId]);
            } else {
                answeredWordsData.set(wordId, '***'); // 정답 정보가 없으면 별표로 표시
            }
        });
    }
    
    // 오답 기록 복원 (선택적)
    if (gameState.wrong_answers) {
        console.log('오답 기록 복원:', gameState.wrong_answers);
    }
    
    // 힌트 사용 기록 복원 (선택적)
    if (gameState.hints_used) {
        console.log('힌트 사용 기록 복원:', gameState.hints_used);
    }
    
    // 그리드에 정답 상태 표시
    restoreAnsweredCells();
}

// 동적 셀 크기 계산 함수
function calculateCellSize(gridSize) {
    const isMobile = window.innerWidth <= 480;
    const isPortrait = window.innerHeight > window.innerWidth;
    
    let availableWidth, availableHeight;
    
    if (isMobile && isPortrait) {
        // 모바일 세로 화면: 화면의 90% 사용
        availableWidth = window.innerWidth * 0.9;
        availableHeight = window.innerHeight * 0.9;
    } else if (isMobile) {
        // 모바일 가로 화면: 화면의 85% 사용
        availableWidth = window.innerWidth * 0.85;
        availableHeight = window.innerHeight * 0.8;
    } else {
        // 데스크톱/태블릿: 기본 크기 유지
        availableWidth = Math.min(600, window.innerWidth * 0.8);
        availableHeight = Math.min(600, window.innerHeight * 0.8);
    }

    // border와 gap 고려하여 실제 사용 가능한 크기 계산
    const borderWidth = 2; // 그리드 테두리
    const cellBorder = 1; // 셀 테두리
    const gap = 0; // 셀 간격 (현재는 0)
    
    const totalBorderWidth = borderWidth * 2 + (gridSize - 1) * cellBorder * 2;
    const totalGapWidth = (gridSize - 1) * gap;
    
    const maxCellWidth = (availableWidth - totalBorderWidth - totalGapWidth) / gridSize;
    const maxCellHeight = (availableHeight - totalBorderWidth - totalGapWidth) / gridSize;
    
    // 더 작은 값으로 통일 (정사각형 유지)
    const calculatedSize = Math.min(maxCellWidth, maxCellHeight);
    
    // 최소/최대 크기 제한
    const minSize = isMobile ? 12 : 20;
    const maxSize = isMobile ? 35 : 50;
    
    const finalSize = Math.max(minSize, Math.min(maxSize, calculatedSize));
    
    console.log('셀 크기 계산:', {
        gridSize,
        availableWidth,
        availableHeight,
        calculatedSize,
        finalSize,
        isMobile,
        isPortrait
    });
    
    return finalSize;
}

function renderPuzzleGrid(template) {
    const grid = $('#puzzle-grid');
    grid.empty();
    
    const gridData = template.grid_pattern;
    const words = template.words;
    const gridSize = gridData.length;
    
    // 동적 셀 크기 계산
    const cellSize = calculateCellSize(gridSize);
    
    let html = `<div class="grid-container" style="display: inline-block; border: 2px solid #333; max-width: 90vw; max-height: 90vh; margin: 0 auto;">`;
    
    for (let y = 0; y < gridData.length; y++) {
        html += '<div class="grid-row" style="display: flex;">';
        for (let x = 0; x < gridData[y].length; x++) {
            const cellValue = gridData[y][x];
            const wordInfo = getWordInfoAtPosition(x, y, words);
            const hasWords = getWordsAtPosition(x, y, words).length > 0;
            
            let cellClass = 'grid-cell-number';
            let cellContent = '';
            let clickEvent = '';
            
            if (cellValue === 2) { // 검은색칸 (단어가 있는 칸)
                cellClass += ' bg-dark text-white';
                
                // 번호는 시작 위치에만 표시
                if (wordInfo) {
                    if (wordInfo.isIntersection) {
                        // 교차점인 경우: 가로 단어는 큰 번호, 세로 단어는 작은 번호로 표시
                        const badgeSize = Math.max(8, Math.min(16, cellSize * 0.3));
                        const fontSize = Math.max(6, Math.min(10, cellSize * 0.2));
                        cellContent = `
                            <div style="position: relative; width: 100%; height: 100%;">
                                <span class="word-number" style="position: absolute; top: 2px; left: 2px; width: ${badgeSize}px; height: ${badgeSize}px; font-size: ${fontSize}px; background: #ff6b6b; border-radius: 2px; display: flex; align-items: center; justify-content: center;">${wordInfo.horizontalWord.word_id}</span>
                                <span class="word-number" style="position: absolute; bottom: 2px; right: 2px; width: ${badgeSize}px; height: ${badgeSize}px; font-size: ${fontSize}px; background: #4ecdc4; border-radius: 2px; display: flex; align-items: center; justify-content: center;">${wordInfo.verticalWord.word_id}</span>
                            </div>
                        `;
                    } else {
                        // 단일 단어인 경우
                        cellContent = `<span class="word-number">${wordInfo.word_id}</span>`;
                    }
                }
                
                // 클릭 이벤트는 모든 검은색 칸에 추가
                if (hasWords) {
                    clickEvent = ` onclick="handleCellClick(${y}, ${x}, ${JSON.stringify(words).replace(/"/g, '&quot;')})" style="cursor: pointer;"`;
                }
            } else { // 흰색칸 (빈 칸)
                cellClass += ' bg-light';
                cellContent = '□';
            }
            
            const fontSize = Math.max(8, Math.min(14, cellSize * 0.3));
            // data-word-id 속성 추가 (보안을 위해 단어 ID만 저장)
            const dataWordId = wordInfo ? (wordInfo.pz_word_id || wordInfo.word_id) : '';
            const dataAttr = dataWordId ? ` data-word-id="${dataWordId}"` : '';
            html += `<div class="${cellClass}" style="width: ${cellSize}px; height: ${cellSize}px; border: 1px solid #ccc; display: flex; align-items: center; justify-content: center; font-size: ${fontSize}px; font-weight: bold; box-sizing: border-box;"${clickEvent}${dataAttr}>${cellContent}</div>`;
        }
        html += '</div>';
    }
    
    html += '</div>';
    grid.html(html);
    
    // 그리드 렌더링 후 정답 상태 복원
    restoreAnsweredCells();
}

// 정답 상태 복원 함수
function restoreAnsweredCells() {
    if (!answeredWords || answeredWords.size === 0) return;
    
    // answeredWords에서 정답 정보 복원
    answeredWords.forEach(function(wordId) {
        const correctWord = answeredWordsData.get(wordId);
        if (correctWord) {
            // 임시로 currentWordId 설정
            const originalWordId = currentWordId;
            currentWordId = wordId;
            
            // 정답 표시
            updateGridWithAnswer(correctWord);
            
            // currentWordId 복원
            currentWordId = originalWordId;
        }
    });
}

// 특정 위치의 단어 정보 찾기 (교차점 우선순위 적용 - 템플릿 관리와 동일)
function getWordInfoAtPosition(x, y, words) {
    let horizontalWord = null;
    let verticalWord = null;
    
    for (const wordInfo of words) {
        const word = wordInfo.position;
        
        // 가로 방향 단어
        if (word.direction === 'horizontal') {
            if (y === word.start_y && x >= word.start_x && x <= word.end_x) {
                // 단어의 시작 위치인지 확인
                if (x === word.start_x) {
                    horizontalWord = wordInfo;
                }
            }
        }
        // 세로 방향 단어
        else if (word.direction === 'vertical') {
            if (x === word.start_x && y >= word.start_y && y <= word.end_y) {
                // 단어의 시작 위치인지 확인
                if (y === word.start_y) {
                    verticalWord = wordInfo;
                }
            }
        }
    }
    
    // 교차점인 경우: 가로 단어 우선, 세로 단어는 작은 번호로 표시
    if (horizontalWord && verticalWord) {
        // 가로 단어를 메인으로 반환하되, 교차점 정보 포함
        return {
            ...horizontalWord,
            isIntersection: true,
            horizontalWord: horizontalWord,
            verticalWord: verticalWord
        };
    }
    
    // 단일 단어인 경우
    return horizontalWord || verticalWord;
}

// 클릭 처리용 - 해당 위치의 모든 단어 찾기 (교차점 처리)
function getWordsAtPosition(x, y, words) {
    let foundWords = [];
    
    for (const wordInfo of words) {
        const word = wordInfo.position;
        
        // 가로 방향 단어
        if (word.direction === 'horizontal') {
            if (y === word.start_y && x >= word.start_x && x <= word.end_x) {
                foundWords.push(wordInfo);
            }
        }
        // 세로 방향 단어
        else if (word.direction === 'vertical') {
            if (x === word.start_x && y >= word.start_y && y <= word.end_y) {
                foundWords.push(wordInfo);
            }
        }
    }
    
    return foundWords;
}

// 검은색 칸 클릭 처리 함수
function handleCellClick(row, col, words) {
    // 해당 위치의 모든 단어 찾기 (교차점 처리)
    let foundWords = getWordsAtPosition(col, row, words);
    
    if (foundWords.length === 0) {
        return;
    }
    
    let selectedWord = foundWords[0]; // 기본적으로 첫 번째 단어 선택
    
    // 교차점인 경우 (여러 단어가 있는 경우) 배지가 있는 단어 우선 선택
    if (foundWords.length > 1) {
        // 현재 클릭한 위치에 배지가 있는지 확인
        const wordInfo = getWordInfoAtPosition(col, row, words);
        if (wordInfo) {
            // 배지가 있는 단어를 우선 선택
            selectedWord = wordInfo;
        } else {
            // 배지가 없으면 첫 번째 단어 선택
            selectedWord = foundWords[0];
        }
    }
    
    // 선택된 단어 처리
    currentWordId = selectedWord.pz_word_id || selectedWord.word_id;
    currentHintId = null; // 새로운 단어 선택 시 힌트 ID 초기화
    baseHintId = selectedWord.hint_id || null; // 기본 힌트 ID 설정
    
    // 힌트 영역 완전 초기화
    $('#hints-list').empty(); // 기존 힌트 목록 삭제
    $('#additional-hints').hide(); // 힌트 영역 숨기기
    
    // 힌트보기 버튼 재활성화 (새로운 단어 선택 시)
    $('#show-hint-btn').prop('disabled', false).text('힌트보기');
    
    // 디버깅: selectedWord 객체 확인
    console.log('selectedWord 객체:', selectedWord);
    console.log('selectedWord.hint:', selectedWord.hint);
    
    // 우측 힌트 영역에 기본 힌트만 표시 (힌트 내용만)
    $('#current-hint').html(`
        <strong>힌트:</strong> ${selectedWord.hint || '힌트가 없습니다.'}
    `);
    
    $('#answer-input').val('').focus();
    $('#word-input-section').show();
    $('#result-message').empty();
}

$('#check-answer-btn').click(function() {
    if (!currentWordId) return;
    
    const answer = $('#answer-input').val().trim();
    if (!answer) {
        alert('정답을 입력해주세요.');
        return;
    }
    
    $.post('{{ route("puzzle-game.check-answer") }}', {
        word_id: currentWordId,
        answer: answer,
        _token: '{{ csrf_token() }}'
    })
            .done(function(data) {
            // 실시간 카운트 업데이트
            if (data.correct_count !== undefined && data.wrong_count !== undefined) {
                updateGameCounts(data.correct_count, data.wrong_count);
            }
            
            if (data.is_correct) {
                // 정답인 경우
                console.log('정답 확인됨');
                $('#result-message').html(`<div class="alert alert-success">${data.message}</div>`);
                
                // 서버에서 받은 정답을 그리드에 표시
                updateGridWithAnswer(data.correct_answer);
                
                // 입력 영역 숨기기
                $('#word-input-section').hide();
                
                // 정답 메시지 표시 후 2초 뒤 숨기기
                setTimeout(function() {
                    $('#result-message').empty();
                }, 2000);
                
                // 클리어 조건 실시간 업데이트
                updateClearCondition();
                
                // 레벨 완료 체크 (모든 단어를 맞췄는지 확인)
                checkLevelCompletion();
            } else {
            // 오답인 경우
            if (data.wrong_count_exceeded) {
                // 오답 5회 초과 시 메시지 표시
                $('#result-message').html(`<div class="alert alert-danger">${data.message}</div>`);
                // 2초 후 모달 표시
                setTimeout(function() {
                    console.log('오답 초과 모달 표시 시도');
                    const modalElement = document.getElementById('wrongCountExceededModal');
                    if (modalElement) {
                        const modal = new bootstrap.Modal(modalElement);
                        modal.show();
                        console.log('오답 초과 모달 표시됨');
                    } else {
                        console.error('오답 초과 모달을 찾을 수 없음');
                    }
                }, 2000);
            } else {
                // 일반 오답 시 메시지 표시 (4회일 때는 특별 메시지)
                $('#result-message').html(`<div class="alert alert-danger">${data.message}</div>`);
            }
            
            // 입력 필드 초기화
            $('#answer-input').val('').focus();
        }
    })
    .fail(function(xhr) {
        alert('오류가 발생했습니다.');
    });
});

function updateGridWithAnswer(correctWord) {
    console.log('updateGridWithAnswer 호출됨');
    
    // 현재 선택된 단어의 위치 정보를 사용하여 그리드에 정답 표시
    if (!currentWordId) {
        console.log('currentWordId가 없음');
        return;
    }
    
    // 현재 선택된 단어 정보 찾기
    let selectedWord = null;
    for (let word of currentTemplateWords) {
        if ((word.pz_word_id || word.word_id) == currentWordId) {
            selectedWord = word;
            break;
        }
    }
    
    if (!selectedWord) {
        console.log('selectedWord를 찾을 수 없음');
        return;
    }
    
    // 정답을 맞춘 단어를 추적에 추가
    answeredWords.add(currentWordId);
    // 정답 데이터 저장 (정답일 때만 저장하므로 보안 문제 없음)
    answeredWordsData.set(currentWordId, correctWord);
    
    // Wordle처럼 게임 상태를 서버에 저장 (선택적)
    // 실제 구현에서는 서버에서 게임 상태를 관리하므로 클라이언트에서는 표시만 담당
    console.log('정답 단어 추가됨:', currentWordId, correctWord);
    
    const position = selectedWord.position;
    const direction = position.direction;
    const startX = position.start_x;
    const startY = position.start_y;
    const endX = position.end_x;
    const endY = position.end_y;
    
    // 현재 그리드 크기에 따른 동적 크기 계산
    const gridSize = currentTemplate.grid_pattern.length;
    const cellSize = calculateCellSize(gridSize);
    const answerFontSize = Math.max(10, Math.min(18, cellSize * 0.4));
    const answerPadding = Math.max(2, Math.min(6, cellSize * 0.1));
    const badgeSize = Math.max(8, Math.min(16, cellSize * 0.3));
    const badgeFontSize = Math.max(6, Math.min(10, cellSize * 0.2));
    
    // 그리드에서 해당 단어 위치에 정답 표시
    for (let i = 0; i < correctWord.length; i++) {
        let x, y;
        
        if (direction === 'horizontal') {
            x = startX + i;
            y = startY;
        } else {
            x = startX;
            y = startY + i;
        }
        
        // 그리드 셀 찾기 (0부터 시작하는 인덱스)
        const cell = $(`.grid-row:eq(${y}) .grid-cell-number:eq(${x})`);
        if (cell.length > 0) {
            const isStartPosition = (direction === 'horizontal' && i === 0) || (direction === 'vertical' && i === 0);
            
            if (isStartPosition) {
                // 시작 위치: 배지를 정답 뒤로 숨기고 정답만 표시
                cell.html(`
                    <div style="background-color: #6c757d; color: white; padding: ${answerPadding}px ${answerPadding * 2}px; border-radius: 2px; font-size: ${answerFontSize}px; font-weight: bold; z-index: 10;">${correctWord[i]}</div>
                    <span class="word-number" style="position: absolute; top: 2px; left: 2px; z-index: 5; opacity: 0.3; width: ${badgeSize}px; height: ${badgeSize}px; font-size: ${badgeFontSize}px; display: flex; align-items: center; justify-content: center;">${selectedWord.word_id}</span>
                `);
            } else {
                // 중간 위치: 정답 문자만 표시
                cell.html(`
                    <div style="background-color: #6c757d; color: white; padding: ${answerPadding}px ${answerPadding * 2}px; border-radius: 2px; font-size: ${answerFontSize}px; font-weight: bold;">${correctWord[i]}</div>
                `);
            }
            
            // 칸 자체를 회색으로 변경
            cell.css('background-color', '#6c757d');
            cell.addClass('answered');
            cell.css('position', 'relative'); // 절대 위치를 위한 relative 설정
        }
    }
}

function checkLevelCompletion() {
    // 모든 단어가 정답인지 확인
    
    // 모든 단어의 정답을 맞췄는지 확인
    if (answeredWords.size >= currentTemplateWords.length) {
        // 클리어 조건 미달/충족 여부 확인
        $.post('{{ route("puzzle-game.complete-level") }}', {
            _token: '{{ csrf_token() }}',
            score: 0,
            play_time: 0,
            hints_used: 0,
            words_found: answeredWords.size,
            total_words: currentTemplateWords.length,
            accuracy: 100
        })
        .done(function(data) {
            // 클리어 조건 충족: 다음 레벨로 이동 안내
            $('#level-complete-message').text('축하드립니다! 다음 레벨로 이동하겠습니다.');
            $('#level-complete-sub-message').text('확인 버튼을 누르면 다음 레벨로 이동합니다.');
            $('#next-level-btn').text('확인').prop('disabled', false).data('condition', 'met').show();
            $('#levelCompleteModal').modal('show');
        })
        .fail(function(xhr) {
            const response = xhr.responseJSON;
            if (response && response.condition_not_met) {
                // 클리어 조건 미달: 남은 횟수 안내 및 새 퍼즐 재도전
                $('#level-complete-message').text(response.message || '아직 클리어 조건을 충족하지 못했습니다.');
                $('#level-complete-sub-message').text('확인 버튼을 누르면 새 퍼즐로 재도전합니다.');
                $('#next-level-btn').text('확인').prop('disabled', false).data('condition', 'not_met').show();
                $('#levelCompleteModal').modal('show');
            } else {
                $('#level-complete-message').text('오류가 발생했습니다.');
                $('#level-complete-sub-message').text('잠시 후 다시 시도해 주세요.');
                $('#next-level-btn').text('확인').prop('disabled', true).data('condition', '').show();
                $('#levelCompleteModal').modal('show');
            }
        });
    }
}

// 실시간 게임 카운트 업데이트
function updateGameCounts(correctCount, wrongCount) {
    // 정답/오답 카운트 업데이트
    $('.badge.bg-primary').text('정답: ' + correctCount);
    $('.badge.bg-danger').text('오답: ' + wrongCount);
}

// 클리어 조건 실시간 업데이트
function updateClearCondition() {
    console.log('클리어 조건 업데이트 시작');
    $.get('{{ route("puzzle-game.get-clear-condition") }}')
        .done(function(data) {
            console.log('클리어 조건 업데이트 성공:', data);
            if (data.success && data.clear_condition > 1) {
                // 클리어 조건 표시 업데이트
                const clearConditionText = `(${data.clear_count}/${data.clear_condition})`;
                $('#clear-condition-display').text(clearConditionText);
                console.log('클리어 조건 표시 업데이트:', clearConditionText);
            }
        })
        .fail(function(xhr) {
            console.log('클리어 조건 업데이트 실패:', xhr);
        });
}

$('#show-hint-btn').click(function() {
    if (!currentWordId) return;
    
    // 버튼 비활성화 (한 번만 클릭 가능)
    $(this).prop('disabled', true).text('힌트 사용됨');
    
    $.get('{{ route("puzzle-game.get-hints") }}', {
        word_id: currentWordId,
        current_hint_id: currentHintId, // 현재 힌트 ID 전달
        base_hint_id: baseHintId // 기본 힌트 ID 전달
    })
    .done(function(data) {
        if (data.hint) {
            // 힌트 목록을 초기화하고 새로운 힌트만 표시
            const hintsList = $('#hints-list');
            hintsList.empty(); // 기존 힌트 목록 삭제
            hintsList.append(`<div class="alert alert-info">${data.hint.hint}</div>`);
            
            // 현재 힌트 ID를 새로 받은 힌트의 ID로 업데이트
            currentHintId = data.hint.id;
            
            $('#additional-hints').show();
        } else {
            // 더 이상 힌트가 없는 경우
            alert(data.message || '더 이상 사용할 수 있는 힌트가 없습니다.');
            // 힌트가 없으면 버튼 다시 활성화
            $('#show-hint-btn').prop('disabled', false).text('힌트보기');
        }
    })
    .fail(function(xhr) {
        alert('힌트를 불러올 수 없습니다.');
        // 오류 발생 시 버튼 다시 활성화
        $('#show-hint-btn').prop('disabled', false).text('힌트보기');
    });
});

// 정답보기 버튼 (관리자만)
$('#show-answer-btn').click(function() {
    if (!currentWordId) return;
    
    $.get('{{ route("puzzle-game.show-answer") }}', {
        word_id: currentWordId
    })
    .done(function(data) {
        if (data.success) {
            $('#result-message').html(`<div class="alert alert-info"><i class="fas fa-eye me-2"></i>${data.message}</div>`);
        } else {
            alert(data.message || '정답을 불러올 수 없습니다.');
        }
    })
    .fail(function(xhr) {
        if (xhr.status === 403) {
            alert('관리자만 접근 가능합니다.');
        } else {
            alert('정답을 불러올 수 없습니다.');
        }
    });
});

$('#next-level-btn').off('click').on('click', function() {
    if ($(this).data('condition') === 'not_met') {
        // 클리어 조건 미달: 새 퍼즐로 재도전
        $('#levelCompleteModal').modal('hide');
        updateClearCondition();
        setTimeout(function() {
            location.reload();
        }, 500);
    } else {
        // 클리어 조건 충족: 다음 레벨로 이동 (API 호출 없이 새로고침)
        $('#levelCompleteModal').modal('hide');
        location.reload();
    }
});

function startTimer() {
    gameTimer = setInterval(function() {
        timeLeft--;
        $('#time-left').text(timeLeft);
        
        if (timeLeft <= 0) {
            clearInterval(gameTimer);
            gameOver();
        }
    }, 1000);
}

function gameOver() {
    $.post('{{ route("puzzle-game.game-over") }}', {
        _token: '{{ csrf_token() }}'
    })
    .done(function(data) {
        $('#gameOverModal').modal('show');
    })
    .fail(function(xhr) {
        alert('오류가 발생했습니다.');
    });
}

// 화면 크기 변경 시 그리드 재렌더링 (정답 표시 유지)
$(window).on('resize', function() {
    if (currentTemplate) {
        // 그리드 재렌더링 (정답 상태는 restoreAnsweredCells에서 복원됨)
        renderPuzzleGrid(currentTemplate);
    }
});

$('#answer-input').on('keydown', function(e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        $('#check-answer-btn').click();
    }
});
</script>
@endpush 