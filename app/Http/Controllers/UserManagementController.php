<?php

namespace App\Http\Controllers;

use App\Models\User;
use App\Models\UserPuzzleGame;
use App\Models\UserPuzzleProfile;
use App\Models\PuzzleGameRecord;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;

class UserManagementController extends Controller
{
    public function index()
    {
        $users = User::select('id', 'name', 'email', 'created_at', 'last_login_at', 'is_admin')
            ->orderBy('created_at', 'desc')
            ->paginate(20);

        return view('admin.user-management.index', compact('users'));
    }

    public function getPuzzleGameInfo($userId)
    {
        try {
            Log::info("퍼즐게임 정보 조회 시작: user_id = {$userId}");
            
            $user = User::findOrFail($userId);
            Log::info("사용자 정보 조회 완료: {$user->email}");
            
            $puzzleGame = UserPuzzleGame::where('user_id', $userId)->orderByDesc('updated_at')->first();
            Log::info("퍼즐게임 데이터 조회 완료: " . ($puzzleGame ? '데이터 있음' : '데이터 없음'));
            
            $profile = UserPuzzleProfile::where('user_id', $userId)->first();
            Log::info("퍼즐 프로필 데이터 조회 완료: " . ($profile ? '데이터 있음' : '데이터 없음'));

            // 통계 계산
            $totalAttempts = $profile ? $profile->games_played : ($puzzleGame ? $puzzleGame->total_correct_answers + $puzzleGame->total_wrong_answers : 0);
            $completedGames = $profile ? $profile->games_completed : 0;
            $failedGames = $profile ? $profile->games_failed : 0;
            $accuracyRate = $puzzleGame ? $puzzleGame->accuracy_rate : 0;
            $totalPlayTime = $profile ? $profile->total_play_time : ($puzzleGame ? $puzzleGame->total_play_time : 0);
            
            Log::info("통계 계산 완료: attempts={$totalAttempts}, completed={$completedGames}, failed={$failedGames}, accuracy={$accuracyRate}");

            $recentGames = [];
            if ($puzzleGame) {
                $recentGames[] = [
                    'level_played' => $puzzleGame->current_level,
                    'game_status' => $puzzleGame->is_active ? '진행중' : '종료',
                    'score' => $puzzleGame->total_correct_answers,
                    'accuracy' => $puzzleGame->accuracy_rate,
                    'play_time' => $puzzleGame->total_play_time,
                    'created_at' => $puzzleGame->updated_at ? $puzzleGame->updated_at->format('Y-m-d H:i:s') : '-' 
                ];
            }
            
            Log::info("최근 게임 데이터 준비 완료: " . count($recentGames) . "개");

            $data = [
                'user' => [
                    'id' => $user->id,
                    'name' => $user->name,
                    'email' => $user->email
                ],
                'puzzle_game' => $puzzleGame ? [
                    'current_level' => $puzzleGame->current_level,
                    'accuracy_rate' => $puzzleGame->accuracy_rate,
                    'total_play_time' => $puzzleGame->total_play_time,
                    'last_played_at' => $puzzleGame->last_played_at,
                    'is_active' => $puzzleGame->is_active
                ] : null,
                'statistics' => [
                    'total_attempts' => $totalAttempts,
                    'accuracy_rate' => $accuracyRate,
                    'total_play_time' => $totalPlayTime,
                    'completed_games' => $completedGames,
                    'failed_games' => $failedGames
                ],
                'recent_games' => $recentGames
            ];
            
            Log::info("응답 데이터 준비 완료");

            return response()->json(['success' => true, 'data' => $data]);
        } catch (\Exception $e) {
            Log::error('퍼즐게임 정보 조회 실패: ' . $e->getMessage());
            Log::error('스택 트레이스: ' . $e->getTraceAsString());
            return response()->json(['success' => false, 'message' => '퍼즐게임 정보를 불러오는데 실패했습니다.'], 500);
        }
    }

    public function toggleAdmin($userId)
    {
        try {
            $user = User::findOrFail($userId);
            $user->is_admin = !$user->is_admin;
            $user->save();

            $message = $user->is_admin ? '관리자로 지정되었습니다.' : '일반 회원으로 변경되었습니다.';
            
            Log::info("회원 관리자 권한 변경: {$user->email} -> " . ($user->is_admin ? '관리자' : '일반회원'));

            return response()->json([
                'success' => true, 
                'message' => $message,
                'is_admin' => $user->is_admin
            ]);
        } catch (\Exception $e) {
            Log::error('관리자 권한 변경 실패: ' . $e->getMessage());
            return response()->json(['success' => false, 'message' => '권한 변경에 실패했습니다.'], 500);
        }
    }

    public function resetPassword($userId)
    {
        try {
            $user = User::findOrFail($userId);
            $user->password = Hash::make('puzzle123!@#');
            $user->save();

            Log::info("회원 비밀번호 초기화: {$user->email}");

            return response()->json([
                'success' => true, 
                'message' => '비밀번호가 초기화되었습니다. (puzzle123!@#)'
            ]);
        } catch (\Exception $e) {
            Log::error('비밀번호 초기화 실패: ' . $e->getMessage());
            return response()->json(['success' => false, 'message' => '비밀번호 초기화에 실패했습니다.'], 500);
        }
    }

    public function destroy($userId)
    {
        try {
            $user = User::findOrFail($userId);
            
            // 자기 자신은 삭제할 수 없음
            if ($user->id === auth()->id()) {
                return response()->json([
                    'success' => false, 
                    'message' => '자기 자신은 삭제할 수 없습니다.'
                ], 400);
            }
            
            // 관리자는 삭제할 수 없음
            if ($user->is_admin) {
                return response()->json([
                    'success' => false, 
                    'message' => '관리자는 삭제할 수 없습니다.'
                ], 400);
            }

            Log::info("회원 삭제 시작: {$user->email}");

            // 관련 데이터 삭제 (게시글, 댓글, 첨부파일 등)
            $this->deleteUserRelatedData($user);

            // 회원 삭제
            $user->delete();

            Log::info("회원 삭제 완료: {$user->email}");

            return response()->json([
                'success' => true, 
                'message' => '회원이 성공적으로 삭제되었습니다.'
            ]);
        } catch (\Exception $e) {
            Log::error('회원 삭제 실패: ' . $e->getMessage());
            return response()->json([
                'success' => false, 
                'message' => '회원 삭제에 실패했습니다: ' . $e->getMessage()
            ], 500);
        }
    }

    private function deleteUserRelatedData($user)
    {
        try {
            // 게시글 삭제 (첨부파일과 댓글도 함께 삭제됨)
            $boards = $user->boards;
            foreach ($boards as $board) {
                // 첨부파일 삭제
                foreach ($board->attachments as $attachment) {
                    \Storage::disk('public')->delete($attachment->file_path);
                    $attachment->delete();
                }
                
                // 댓글 삭제
                $board->comments()->delete();
                
                // 게시글 삭제
                $board->delete();
            }

            // 퍼즐게임 관련 데이터 삭제
            $user->puzzleGames()->delete();
            $user->puzzleProfiles()->delete();
            $user->puzzleGameRecords()->delete();

            Log::info("회원 관련 데이터 삭제 완료: {$user->email}");
        } catch (\Exception $e) {
            Log::error('회원 관련 데이터 삭제 실패: ' . $e->getMessage());
            throw $e;
        }
    }
} 