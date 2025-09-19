<?php

use Illuminate\Support\Facades\Route;
use App\Models\BoardType;
use App\Http\Controllers\PzWordController;
use App\Http\Controllers\PzHintController;
use App\Http\Controllers\PzHintGeneratorController;
use App\Http\Controllers\PuzzleLevelController;
use App\Http\Controllers\PuzzleGridController;
use App\Http\Controllers\GridTemplateController;
use App\Http\Controllers\Auth\SocialLoginController;
use App\Http\Controllers\PuzzleGameController;
use App\Http\Controllers\UserManagementController;
use App\Http\Controllers\OcrController;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "web" middleware group. Make something great!
|
*/

// 루트 경로 - 인증 없이 접근 가능 (미들웨어 완전 제거)
Route::get('/', function () {
    $boardTypes = BoardType::where('is_active', true)
        ->orderBy('sort_order')
        ->orderBy('name')
        ->get();
    return view('welcome', compact('boardTypes'));
})->name('home')->withoutMiddleware(['web']);

Route::get('/main', function () {
    $boardTypes = BoardType::where('is_active', true)
        ->orderBy('sort_order')
        ->orderBy('name')
        ->get();
    return view('welcome', compact('boardTypes'));
})->name('main');

// 기본 인증 라우트 활성화
Auth::routes();

// SNS 로그인 라우트
Route::get('auth/google', [SocialLoginController::class, 'redirectToGoogle'])->name('auth.google');
Route::get('auth/google/callback', [SocialLoginController::class, 'handleGoogleCallback']);

Route::get('auth/kakao', [SocialLoginController::class, 'redirectToKakao'])->name('auth.kakao');
Route::get('auth/kakao/callback', [SocialLoginController::class, 'handleKakaoCallback']);

Route::get('auth/naver', [SocialLoginController::class, 'redirectToNaver'])->name('auth.naver');
Route::get('auth/naver/callback', [SocialLoginController::class, 'handleNaverCallback']);

Route::resource('posts', App\Http\Controllers\PostController::class);

Route::post('board/upload-image', [App\Http\Controllers\BoardController::class, 'uploadImage'])->name('board.upload-image.global');

Route::post('board-comments', [App\Http\Controllers\BoardCommentController::class, 'store'])->name('board-comments.store');
Route::get('board-comments/{boardComment}/edit', [App\Http\Controllers\BoardCommentController::class, 'edit'])->name('board-comments.edit');
Route::put('board-comments/{boardComment}', [App\Http\Controllers\BoardCommentController::class, 'update'])->name('board-comments.update');
Route::delete('board-comments/{boardComment}', [App\Http\Controllers\BoardCommentController::class, 'destroy'])->name('board-comments.destroy');

Route::post('board/{board}/vote', [App\Http\Controllers\BoardVoteController::class, 'vote'])->name('board.vote.global');
Route::post('board/{board}/copy', [App\Http\Controllers\BoardController::class, 'copy'])->name('board.copy.global');

Route::delete('board-attachments/{id}', [App\Http\Controllers\BoardAttachmentController::class, 'destroy'])->name('board-attachments.destroy.global');

// 알림 설정
Route::get('notification-settings', [App\Http\Controllers\NotificationSettingController::class, 'edit'])->name('notification-settings.edit');
Route::put('notification-settings', [App\Http\Controllers\NotificationSettingController::class, 'update'])->name('notification-settings.update');

// 게시판 타입별 라우트
Route::get('board-types', [App\Http\Controllers\BoardTypeController::class, 'index'])->name('board-types.index');

Route::prefix('board/{boardType}')->group(function () {
    Route::get('/', [App\Http\Controllers\BoardController::class, 'index'])->name('board.index');
    Route::get('/create', [App\Http\Controllers\BoardController::class, 'create'])->name('board.create');
    Route::post('/', [App\Http\Controllers\BoardController::class, 'store'])->name('board.store');
    Route::get('/{board}', [App\Http\Controllers\BoardController::class, 'show'])->name('board.show');
    Route::get('/{board}/edit', [App\Http\Controllers\BoardController::class, 'edit'])->name('board.edit');
    Route::put('/{board}', [App\Http\Controllers\BoardController::class, 'update'])->name('board.update');
    Route::delete('/{board}', [App\Http\Controllers\BoardController::class, 'destroy'])->name('board.destroy');
    
    Route::post('/{board}/vote', [App\Http\Controllers\BoardVoteController::class, 'vote'])->name('board.vote.type');
    Route::post('/{board}/copy', [App\Http\Controllers\BoardController::class, 'copy'])->name('board.copy.type');
    
    Route::post('/upload-image', [App\Http\Controllers\BoardController::class, 'uploadImage'])->name('board.upload-image.type');
    Route::delete('/attachments/{id}', [App\Http\Controllers\BoardAttachmentController::class, 'destroy'])->name('board-attachments.destroy.type');
});

// 크로스워드 퍼즐 관리
Route::prefix('puzzle')->name('puzzle.')->middleware(['auth'])->group(function () {
    Route::get('words/data', [PzWordController::class, 'getData'])->name('words.data');
    Route::post('words/batch-update', [PzWordController::class, 'batchUpdate'])->name('words.batch-update');
    Route::resource('words', PzWordController::class);
    Route::put('words/{id}/toggle-active', [PzWordController::class, 'toggleActive'])->name('words.toggle-active');
    
    Route::prefix('words/{wordId}/hints')->name('hints.')->group(function () {
        Route::get('/', [PzHintController::class, 'index'])->name('index');
        Route::post('/', [PzHintController::class, 'store'])->name('store');
        Route::put('/{id}', [PzHintController::class, 'update'])->name('update');
        Route::delete('/{id}', [PzHintController::class, 'destroy'])->name('destroy');
        Route::put('/{id}/primary', [PzHintController::class, 'setPrimary'])->name('set-primary');
        Route::put('/reorder', [PzHintController::class, 'reorder'])->name('reorder');
    });

    // 힌트 생성 관리
    Route::prefix('hint-generator')->name('hint-generator.')->group(function () {
        Route::get('/', [PzHintGeneratorController::class, 'index'])->name('index');
        Route::get('/words-ajax', [PzHintGeneratorController::class, 'getWordsAjax'])->name('words-ajax');
        Route::post('/word/{wordId}', [PzHintGeneratorController::class, 'generateForWord'])->name('generate-word');
        Route::get('/word/{wordId}/hints', [PzHintGeneratorController::class, 'getWordHints'])->name('get-word-hints');
        Route::post('/batch', [PzHintGeneratorController::class, 'generateBatch'])->name('generate-batch');
        Route::post('/category', [PzHintGeneratorController::class, 'generateByCategory'])->name('generate-category');
        Route::get('/test-connection', [PzHintGeneratorController::class, 'testConnection'])->name('test-connection');
        Route::get('/stats', [PzHintGeneratorController::class, 'getStats'])->name('stats');
        
        // 힌트 수정 관련
        Route::post('/regenerate-hints', [PzHintGeneratorController::class, 'regenerateHints'])->name('regenerate-hints');
        Route::get('/hints-for-correction', [PzHintGeneratorController::class, 'getHintsForCorrection'])->name('hints-for-correction');
    });

    // 퍼즐 레벨 관리
    Route::prefix('levels')->name('levels.')->group(function () {
        Route::get('/', [PuzzleLevelController::class, 'index'])->name('index');
        Route::get('/create', [PuzzleLevelController::class, 'create'])->name('create');
        Route::post('/', [PuzzleLevelController::class, 'store'])->name('store');
        Route::get('/{id}', [PuzzleLevelController::class, 'show'])->name('show');
        Route::get('/{id}/edit', [PuzzleLevelController::class, 'edit'])->name('edit');
        Route::put('/{id}', [PuzzleLevelController::class, 'update'])->name('update');
        Route::delete('/{id}', [PuzzleLevelController::class, 'destroy'])->name('destroy');
        Route::post('/restore-origin', [PuzzleLevelController::class, 'restoreFromOrigin'])->name('restore-origin');
    });

    // 레벨 목록 API (샘플보기용)
    Route::get('/puzzle/levels', [PuzzleLevelController::class, 'getLevelsForSamples'])->name('puzzle.levels.for-samples');

    // 퍼즐 그리드 관리
    Route::prefix('grids')->name('grids.')->group(function () {
        Route::get('/', [PuzzleGridController::class, 'index'])->name('index');
        Route::get('/create', [PuzzleGridController::class, 'create'])->name('create');
        Route::post('/', [PuzzleGridController::class, 'store'])->name('store');
        Route::get('/{level}', [PuzzleGridController::class, 'show'])->name('show');
    });

    // 그리드 템플릿 관리
    Route::prefix('grid-templates')->name('grid-templates.')->group(function () {
        Route::get('/', [GridTemplateController::class, 'index'])->name('index');
        Route::get('/create', [GridTemplateController::class, 'create'])->name('create');
        Route::post('/', [GridTemplateController::class, 'store'])->name('store');
        Route::get('/{id}', [GridTemplateController::class, 'show'])->name('show');
        Route::put('/{id}', [GridTemplateController::class, 'update'])->name('update');
        Route::delete('/{id}', [GridTemplateController::class, 'destroy'])->name('destroy');
        Route::post('/level-conditions', [GridTemplateController::class, 'getLevelConditions'])->name('level-conditions');
        Route::post('/extract-words', [GridTemplateController::class, 'extractWords'])->name('extract-words');
        Route::post('/update-numbering', [GridTemplateController::class, 'updateTemplateNumbering'])->name('update-numbering');
        Route::post('/sample-templates', [GridTemplateController::class, 'getSampleTemplates'])->name('sample-templates');
        Route::post('/create-from-sample', [GridTemplateController::class, 'createFromSample'])->name('create-from-sample');
        Route::get('/level/{levelId}', [GridTemplateController::class, 'getTemplatesByLevel'])->name('level-templates');
    });

    Route::get('/grid-templates/{id}/json', [GridTemplateController::class, 'showJson']);
});

// 크로스워드 퍼즐 테스트 페이지 (제미나이 알고리즘)
Route::get('/crossword-test', function () {
    return view('crossword-test');
})->name('crossword.test');

// 크로스워드 퍼즐 게임 (모든 사용자)
Route::prefix('puzzle-game')->name('puzzle-game.')->group(function () {
    Route::get('/', [PuzzleGameController::class, 'index'])->name('index');
    Route::get('/template', [PuzzleGameController::class, 'getTemplate'])->name('get-template');
    Route::post('/check-answer', [PuzzleGameController::class, 'checkAnswer'])->name('check-answer');
    Route::get('/hints', [PuzzleGameController::class, 'getHints'])->name('get-hints');
    Route::get('/show-answer', [PuzzleGameController::class, 'showAnswer'])->name('show-answer');
    Route::post('/complete-level', [PuzzleGameController::class, 'completeLevel'])->name('complete-level');
    Route::post('/game-over', [PuzzleGameController::class, 'gameOver'])->name('game-over');
    Route::get('/clear-condition', [PuzzleGameController::class, 'getClearCondition'])->name('get-clear-condition');
});

// 회원 관리 (관리자만 접근)
Route::prefix('admin')->name('admin.')->middleware(['auth', 'admin'])->group(function () {
    Route::get('/users', [UserManagementController::class, 'index'])->name('users.index');
    Route::get('/users/{userId}/puzzle-info', [UserManagementController::class, 'getPuzzleGameInfo'])->name('users.puzzle-info');
    Route::post('/users/{userId}/toggle-admin', [UserManagementController::class, 'toggleAdmin'])->name('users.toggle-admin');
    Route::post('/users/{userId}/reset-password', [UserManagementController::class, 'resetPassword'])->name('users.reset-password');
Route::delete('/users/{userId}', [UserManagementController::class, 'destroy'])->name('users.destroy');
});

// OCR 기능 라우트
Route::prefix('ocr')->name('ocr.')->group(function () {
    Route::get('/', [OcrController::class, 'index'])->name('index');
    Route::post('/process-server', [OcrController::class, 'processServer'])->name('process-server');
    Route::post('/process-google-vision', [OcrController::class, 'processGoogleVision'])->name('process-google-vision');
    Route::post('/save-result', [OcrController::class, 'saveResult'])->name('save-result');
    Route::get('/history', [OcrController::class, 'history'])->name('history');
});

// 똥손 로또 시스템
Route::prefix('lotto')->name('lotto.')->middleware(['auth'])->group(function () {
    Route::get('/', [App\Http\Controllers\LottoController::class, 'index'])->name('index');
    Route::get('/upload', [App\Http\Controllers\LottoController::class, 'upload'])->name('upload');
    Route::post('/upload', [App\Http\Controllers\LottoController::class, 'store'])->name('store');
    Route::get('/ticket/{id}', [App\Http\Controllers\LottoController::class, 'show'])->name('show');
    Route::get('/rankings', [App\Http\Controllers\LottoController::class, 'rankings'])->name('rankings');
    Route::get('/statistics', [App\Http\Controllers\LottoController::class, 'statistics'])->name('statistics');
});