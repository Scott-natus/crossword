@extends('layouts.app')

@section('title', '회원 관리')

@section('content')
<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">회원 관리</h3>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-bordered table-striped">
                            <thead>
                                <tr>
                                    <th>회원번호</th>
                                    <th>이름</th>
                                    <th>이메일</th>
                                    <th>가입일</th>
                                    <th>최종접속일</th>
                                    <th>관리자</th>
                                    <th>기능</th>
                                </tr>
                            </thead>
                            <tbody>
                                @foreach($users as $user)
                                <tr>
                                    <td>{{ $user->id }}</td>
                                    <td>{{ $user->name }}</td>
                                    <td>{{ $user->email }}</td>
                                    <td>{{ $user->created_at ? (is_string($user->created_at) ? $user->created_at : $user->created_at->format('Y-m-d H:i')) : '-' }}</td>
                                    <td>{{ $user->last_login_at ? (is_string($user->last_login_at) ? $user->last_login_at : $user->last_login_at->format('Y-m-d H:i')) : '-' }}</td>
                                    <td>
                                        <span class="badge badge-{{ $user->is_admin ? 'success' : 'secondary' }}" style="color: #000;">
                                            {{ $user->is_admin ? '관리자' : '일반회원' }}
                                        </span>
                                    </td>
                                    <td>
                                        <div class="btn-group" role="group">
                                            <button type="button" class="btn btn-info btn-sm puzzle-game-btn" 
                                                    data-user-id="{{ $user->id }}" 
                                                    data-user-name="{{ $user->name }}">
                                                퍼즐게임
                                            </button>
                                            <button type="button" class="btn btn-warning btn-sm toggle-admin-btn" 
                                                    data-user-id="{{ $user->id }}" 
                                                    data-is-admin="{{ $user->is_admin ? '1' : '0' }}">
                                                {{ $user->is_admin ? '관리자 해제' : '관리자 지정' }}
                                            </button>
                                            <button type="button" class="btn btn-danger btn-sm reset-password-btn" 
                                                    data-user-id="{{ $user->id }}" 
                                                    data-user-name="{{ $user->name }}">
                                                비밀번호 초기화
                                            </button>
                                            @if(!$user->is_admin && $user->id != Auth::id())
                                                <button type="button" class="btn btn-dark btn-sm delete-user-btn" 
                                                        data-user-id="{{ $user->id }}" 
                                                        data-user-name="{{ $user->name }}">
                                                    회원 삭제
                                                </button>
                                            @endif
                                        </div>
                                    </td>
                                </tr>
                                @endforeach
                            </tbody>
                        </table>
                    </div>
                    
                    <div class="d-flex justify-content-center">
                        {{ $users->links() }}
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- 퍼즐게임 정보 모달 -->
<div class="modal fade" id="puzzleGameModal" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">퍼즐게임 정보</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div id="puzzleGameInfo">
                    <div class="text-center">
                        <div class="spinner-border" role="status">
                            <span class="sr-only">Loading...</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
            </div>
        </div>
    </div>
</div>

<!-- 회원 삭제 확인 모달 -->
<div class="modal fade" id="deleteUserModal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">회원 삭제 확인</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-warning">
                    <strong>⚠️ 주의사항</strong>
                    <ul class="mb-0 mt-2">
                        <li>회원을 삭제하면 해당 회원의 모든 데이터가 영구적으로 삭제됩니다.</li>
                        <li>작성한 게시글, 댓글, 첨부파일 등이 모두 사라집니다.</li>
                        <li>삭제된 데이터는 복구할 수 없습니다.</li>
                    </ul>
                </div>
                <p class="mb-0">
                    <strong><span id="deleteUserName"></span></strong> 회원을 정말로 삭제하시겠습니까?
                </p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <form id="deleteUserForm" method="POST" style="display: inline;">
                    @csrf
                    @method('DELETE')
                    <button type="submit" class="btn btn-danger">회원 삭제</button>
                </form>
            </div>
        </div>
    </div>
</div>

@endsection

@push('scripts')
<script>
$(document).ready(function() {
    // CSRF 토큰 설정
    $.ajaxSetup({
        headers: {
            'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')
        }
    });
    // 퍼즐게임 정보 조회
    $('.puzzle-game-btn').click(function() {
        const userId = $(this).data('user-id');
        const userName = $(this).data('user-name');
        
        $('#puzzleGameModal .modal-title').text(`${userName}님의 퍼즐게임 정보`);
        const modal = new bootstrap.Modal(document.getElementById('puzzleGameModal'));
        modal.show();
        
        $.get(`/admin/users/${userId}/puzzle-info`)
            .done(function(response) {
                if (response.success) {
                    displayPuzzleGameInfo(response.data);
                } else {
                    $('#puzzleGameInfo').html('<div class="alert alert-danger">정보를 불러오는데 실패했습니다.</div>');
                }
            })
            .fail(function() {
                $('#puzzleGameInfo').html('<div class="alert alert-danger">서버 오류가 발생했습니다.</div>');
            });
    });
    
    // 관리자 권한 토글
    $('.toggle-admin-btn').click(function() {
        const userId = $(this).data('user-id');
        const isAdmin = $(this).data('is-admin') === '1';
        const button = $(this);
        
        if (!confirm(isAdmin ? '관리자 권한을 해제하시겠습니까?' : '관리자로 지정하시겠습니까?')) {
            return;
        }
        
        $.post(`/admin/users/${userId}/toggle-admin`)
            .done(function(response) {
                if (response.success) {
                    alert(response.message);
                    // 버튼 텍스트와 상태 업데이트
                    button.text(response.is_admin ? '관리자 해제' : '관리자 지정');
                    button.data('is-admin', response.is_admin ? '1' : '0');
                    // 페이지 새로고침
                    location.reload();
                } else {
                    alert(response.message || '권한 변경에 실패했습니다.');
                }
            })
            .fail(function() {
                alert('서버 오류가 발생했습니다.');
            });
    });
    
    // 비밀번호 초기화
    $('.reset-password-btn').click(function() {
        const userId = $(this).data('user-id');
        const userName = $(this).data('user-name');
        
        if (!confirm(`${userName}님의 비밀번호를 초기화하시겠습니까?\n초기화된 비밀번호: puzzle123!@#`)) {
            return;
        }
        
        $.post(`/admin/users/${userId}/reset-password`)
            .done(function(response) {
                if (response.success) {
                    alert(response.message);
                } else {
                    alert(response.message || '비밀번호 초기화에 실패했습니다.');
                }
            })
            .fail(function() {
                alert('서버 오류가 발생했습니다.');
            });
    });

    // 회원 삭제 버튼 클릭 이벤트
    $('.delete-user-btn').click(function() {
        const userId = $(this).data('user-id');
        const userName = $(this).data('user-name');
        
        // 모달에 회원 이름 설정
        $('#deleteUserName').text(userName);
        
        // 폼 액션 URL 설정
        $('#deleteUserForm').attr('action', `/admin/users/${userId}`);
        
        // 모달 표시
        $('#deleteUserModal').modal('show');
    });

    // 회원 삭제 폼 제출 이벤트
    $('#deleteUserForm').submit(function(e) {
        e.preventDefault();
        
        const form = $(this);
        const submitBtn = form.find('button[type="submit"]');
        const originalText = submitBtn.text();
        
        // 버튼 비활성화 및 로딩 표시
        submitBtn.prop('disabled', true).text('삭제 중...');
        
        $.ajax({
            url: form.attr('action'),
            method: 'DELETE',
            headers: {
                'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')
            }
        })
        .done(function(response) {
            if (response.success) {
                alert('회원이 성공적으로 삭제되었습니다.');
                location.reload(); // 페이지 새로고침
            } else {
                alert(response.message || '회원 삭제에 실패했습니다.');
            }
        })
        .fail(function(xhr) {
            let errorMessage = '서버 오류가 발생했습니다.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            }
            alert(errorMessage);
        })
        .always(function() {
            // 버튼 복원
            submitBtn.prop('disabled', false).text(originalText);
            // 모달 닫기
            $('#deleteUserModal').modal('hide');
        });
    });
    
    function displayPuzzleGameInfo(data) {
        let html = `
            <div class="row">
                <div class="col-md-6">
                    <h6>기본 정보</h6>
                    <table class="table table-sm">
                        <tr><td>이름:</td><td>${data.user.name}</td></tr>
                        <tr><td>이메일:</td><td>${data.user.email}</td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <h6>퍼즐게임 통계</h6>
                    <table class="table table-sm">
                        <tr><td>총 시도 횟수:</td><td>${data.statistics.total_attempts}회</td></tr>
                        <tr><td>정답률:</td><td>${data.statistics.accuracy_rate}%</td></tr>
                        <tr><td>완료한 게임:</td><td>${data.statistics.completed_games}회</td></tr>
                        <tr><td>실패한 게임:</td><td>${data.statistics.failed_games}회</td></tr>
                    </table>
                </div>
            </div>
        `;
        
        if (data.puzzle_game) {
            html += `
                <div class="row mt-3">
                    <div class="col-md-6">
                        <h6>현재 게임 상태</h6>
                        <table class="table table-sm">
                            <tr><td>현재 레벨:</td><td>${data.puzzle_game.current_level}</td></tr>
                            <tr><td>정답률:</td><td>${data.puzzle_game.accuracy_rate}%</td></tr>
                            <tr><td>총 플레이 시간:</td><td>${formatPlayTime(data.puzzle_game.total_play_time)}</td></tr>
                            <tr><td>마지막 플레이:</td><td>${data.puzzle_game.last_played_at || '-'}</td></tr>
                            <tr><td>활성화 상태:</td><td>${data.puzzle_game.is_active ? '활성' : '비활성'}</td></tr>
                        </table>
                    </div>
                </div>
            `;
        }
        
        if (data.recent_games && data.recent_games.length > 0) {
            html += `
                <div class="row mt-3">
                    <div class="col-12">
                        <h6>최근 게임 기록</h6>
                        <div class="table-responsive">
                            <table class="table table-sm table-striped">
                                <thead>
                                    <tr>
                                        <th>레벨</th>
                                        <th>상태</th>
                                        <th>점수</th>
                                        <th>정확도</th>
                                        <th>플레이 시간</th>
                                        <th>날짜</th>
                                    </tr>
                                </thead>
                                <tbody>
            `;
            
            data.recent_games.forEach(function(game) {
                const statusClass = game.game_status === 'completed' ? 'success' : 
                                  game.game_status === 'failed' ? 'danger' : 'warning';
                const statusText = game.game_status === 'completed' ? '완료' : 
                                 game.game_status === 'failed' ? '실패' : '중단';
                
                html += `
                    <tr>
                        <td>${game.level_played}</td>
                        <td><span class="badge badge-${statusClass}">${statusText}</span></td>
                        <td>${game.score}</td>
                        <td>${game.accuracy}%</td>
                        <td>${formatPlayTime(game.play_time)}</td>
                        <td>${game.created_at}</td>
                    </tr>
                `;
            });
            
            html += `
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            `;
        }
        
        if (!data.puzzle_game && data.statistics.total_attempts === 0) {
            html = '<div class="alert alert-info">아직 퍼즐게임을 플레이하지 않았습니다.</div>';
        }
        
        $('#puzzleGameInfo').html(html);
    }
    
    function formatPlayTime(seconds) {
        if (!seconds) return '0초';
        
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;
        
        let result = '';
        if (hours > 0) result += `${hours}시간 `;
        if (minutes > 0) result += `${minutes}분 `;
        if (secs > 0 || result === '') result += `${secs}초`;
        
        return result.trim();
    }
});
</script>
@endpush 