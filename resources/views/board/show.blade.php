@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row mb-4">
        <div class="col-12">
            <h2 class="mb-3">{{ $board->boardType->name }}</h2>
            <hr>
        </div>
    </div>
    <h2 class="fw-bold">{{ $board->title }}</h2>
    <div class="mb-2 text-muted">
        작성자: {{ $board->user->name ?? '알 수 없음' }} | 작성일: {{ $board->created_at->format('Y-m-d H:i') }} | 조회수: {{ $board->views }}
    </div>
    <hr>
    <div class="mb-3">
        <button class="btn btn-outline-primary" id="copyBtn">본문+첨부 복사</button>
    </div>
    <script>
    document.addEventListener('DOMContentLoaded', function() {
        document.getElementById('copyBtn').onclick = function() {
            let text = `제목: {{ $board->title }}\n\n본문: {!! strip_tags($board->content) !!}\n\n`;
            @foreach($board->attachments as $file)
                text += '첨부: /storage/{{ $file->file_path }}\n';
            @endforeach
            navigator.clipboard.writeText(text).then(function() {
                alert('복사되었습니다!');
            });
        };
    });
    </script>
    <div class="mb-4">
        <div class="content-wrapper" style="min-height: 200px;">
            {!! Purifier::clean($board->content) !!}
        </div>
    </div>

    @auth
    <!-- 답글 작성 버튼 및 폼 (게시글 본문 바로 아래) -->
    <div class="mb-4">
        <button class="btn btn-outline-secondary" type="button" data-bs-toggle="collapse" data-bs-target="#replyToPostForm">답글 작성</button>
        <div class="collapse mt-2" id="replyToPostForm">
            <form method="POST" action="{{ route('board.store', ['boardType' => $board->boardType->slug]) }}" enctype="multipart/form-data">
                @csrf
                <input type="hidden" name="parent_id" value="{{ $board->id }}">
                <div class="mb-2">
                    <input type="text" name="title" class="form-control mb-2" placeholder="답글 제목" required>
                    <textarea name="content" class="form-control mb-2" rows="5" placeholder="답글 내용을 입력하세요" required></textarea>
                </div>
                <div class="mb-2">
                    <label for="attachments" class="form-label">첨부파일</label>
                    <input type="file" class="form-control @error('attachments.*') is-invalid @enderror" 
                        id="attachments" name="attachments[]" multiple>
                                                <div class="form-text">이미지, 동영상, PDF, 문서 파일 등 최대 100MB까지 업로드 가능합니다.</div>
                    @error('attachments.*')
                        <div class="invalid-feedback">{{ $message }}</div>
                    @enderror
                </div>
                <input type="hidden" name="password" value="reply_dummy_pw">
                <button type="submit" class="btn btn-primary">답글 등록</button>
            </form>
        </div>
    </div>
    @endauth

    <!-- 복사 버튼 -->
    <div class="mb-4">
        <div class="btn-group">
            <button type="button" class="btn btn-outline-primary" onclick="copyToClipboard()">
                <i class="fas fa-copy"></i> 클립보드에 복사
            </button>
        </div>
    </div>

    <!-- 투표 섹션 -->
    <div class="mb-4">
        <h6>투표</h6>
        <div class="d-flex align-items-center">
            @auth
            <button class="btn btn-outline-success me-2 vote-btn" data-vote="1" onclick="vote({{ $board->id }}, true)">
                <i class="fas fa-thumbs-up"></i> 찬성
                <span class="badge bg-success ms-1">{{ $board->agreeVotes()->count() }}</span>
            </button>
            <button class="btn btn-outline-danger vote-btn" data-vote="0" onclick="vote({{ $board->id }}, false)">
                <i class="fas fa-thumbs-down"></i> 반대
                <span class="badge bg-danger ms-1">{{ $board->disagreeVotes()->count() }}</span>
            </button>
            @endauth
            @guest
            <button class="btn btn-outline-success me-2" data-bs-toggle="modal" data-bs-target="#guestModal">
                <i class="fas fa-thumbs-up"></i> 찬성
            </button>
            <button class="btn btn-outline-danger" data-bs-toggle="modal" data-bs-target="#guestModal">
                <i class="fas fa-thumbs-down"></i> 반대
            </button>
            @endguest
        </div>
    </div>

    @if($board->attachments->count())
        <div class="mb-4">
            <h5>첨부파일</h5>
            <div class="row g-3">
                @foreach($board->attachments as $file)
                    <div class="col-auto">
                        @if(Str::startsWith($file->file_type, 'image/'))
                            <img src="/storage/{{ $file->file_path }}" alt="첨부 이미지" style="max-width:200px; max-height:200px; cursor:pointer;" class="img-thumbnail mb-1"
                                 onclick="showImageModal('/storage/{{ $file->file_path }}')">
                            <br>
                            <a href="/storage/{{ $file->file_path }}" download class="btn btn-outline-secondary btn-sm">
                                다운로드
                            </a>
                        @else
                            <a href="/storage/{{ $file->file_path }}" download class="btn btn-outline-secondary">
                                {{ $file->original_name }}
                            </a>
                        @endif
                    </div>
                @endforeach
            </div>
        </div>
    @endif
    @if($comments->count())
        <div class="mb-4">
            <h5>댓글</h5>
            <ul class="list-group">
                @foreach($comments as $comment)
                    @include('board.partials.comment', ['comment' => $comment, 'depth' => 0])
                @endforeach
            </ul>
        </div>
    @endif

    @auth
    <!-- 댓글 작성 폼 -->
    <div class="mb-4">
        <h5>댓글 작성</h5>
        <form method="POST" action="{{ route('board-comments.store') }}">
            @csrf
            <input type="hidden" name="board_id" value="{{ $board->id }}">
            <div class="mb-3">
                <textarea class="form-control" name="content" rows="3" required placeholder="댓글을 입력하세요..."></textarea>
            </div>
            <button type="submit" class="btn btn-primary">댓글 등록</button>
        </form>
    </div>
    @endauth

    @guest
    <div class="mb-4">
        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#guestModal">댓글 작성</button>
    </div>
    @endguest

    <!-- 원글~답글 트리 구조 표시 -->
    <div class="mb-4">
        <h5>원글~답글 트리</h5>
        <ul class="list-group">
            @include('board.partials.thread', ['post' => $root, 'currentId' => $board->id, 'depth' => 0])
        </ul>
    </div>
    {{-- 첨부파일, 투표, 카피, 댓글 등은 추후 구현 --}}
    <a href="{{ route('board.index', ['boardType' => $board->boardType->slug]) }}" class="btn btn-secondary">목록으로</a>
    @auth
        @if($board->user_id == Auth::id() || (Auth::user()->email === 'rainynux@gmail.com'))
            <a href="{{ route('board.edit', ['boardType' => $board->boardType->slug, 'board' => $board->id]) }}" class="btn btn-primary ms-2">수정</a>
            <!-- 삭제 버튼 및 모달 -->
            <button type="button" class="btn btn-danger ms-2" data-bs-toggle="modal" data-bs-target="#deleteModal">삭제</button>
            <!-- 삭제 모달 -->
            <div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
              <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                  <form method="POST" action="{{ route('board.destroy', ['boardType' => $board->boardType->slug, 'board' => $board->id]) }}">
                    @csrf
                    @method('DELETE')
                    <div class="modal-header">
                      <h5 class="modal-title" id="deleteModalLabel">게시글 삭제</h5>
                      <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                      <div class="mb-3">
                        @if(Auth::user()->email === 'rainynux@gmail.com')
                          <label for="deletePassword" class="form-label">관리자 마스터 비밀번호를 입력하세요</label>
                          <input type="password" class="form-control" id="deletePassword" name="password" required autocomplete="off" placeholder="tngkrrhk">
                          <div class="form-text text-muted">관리자는 모든 글을 삭제할 수 있습니다.</div>
                        @else
                          <label for="deletePassword" class="form-label">게시글 비밀번호를 입력하세요</label>
                          <input type="password" class="form-control" id="deletePassword" name="password" required autocomplete="off">
                        @endif
                      </div>
                      <div class="text-danger small">삭제 시 복구할 수 없습니다.</div>
                    </div>
                    <div class="modal-footer">
                      <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                      <button type="submit" class="btn btn-danger">삭제</button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
        @endif
    @endauth
</div>

<!-- 비회원 안내 모달 -->
<div class="modal fade" id="guestModal" tabindex="-1" aria-labelledby="guestModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="guestModalLabel">회원가입 안내</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        회원가입 또는 로그인 후 이용하실 수 있습니다.<br>
        <a href="{{ route('register') }}?redirect={{ urlencode(request()->url()) }}" class="btn btn-primary mt-3">회원가입</a>
        <a href="{{ route('login') }}?redirect={{ urlencode(request()->url()) }}" class="btn btn-outline-primary mt-3 ms-2">로그인</a>
      </div>
    </div>
  </div>
</div>

<!-- 이미지 미리보기 모달 -->
<div class="modal fade" id="imagePreviewModal" tabindex="-1" aria-labelledby="imagePreviewModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="imagePreviewModalLabel">이미지 미리보기</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body text-center">
        <img id="imagePreviewModalImg" src="" alt="미리보기" class="img-fluid">
      </div>
    </div>
  </div>
</div>
@endsection

@push('scripts')
<script>
function vote(boardId, isAgree) {
    fetch(`/board/${boardId}/vote`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': '{{ csrf_token() }}',
            'Accept': 'application/json'
        },
        body: JSON.stringify({ is_agree: isAgree })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // 투표 버튼 UI 업데이트
            document.querySelectorAll('.vote-btn').forEach(btn => {
                btn.classList.remove('active', 'btn-success', 'btn-danger');
                btn.classList.add(btn.dataset.vote === '1' ? 'btn-outline-success' : 'btn-outline-danger');
            });
            if (data.user_vote) {
                // 찬성
                const agreeBtn = document.querySelector('[data-vote="1"]');
                agreeBtn.classList.remove('btn-outline-success');
                agreeBtn.classList.add('active', 'btn-success');
            } else {
                // 반대
                const disagreeBtn = document.querySelector('[data-vote="0"]');
                disagreeBtn.classList.remove('btn-outline-danger');
                disagreeBtn.classList.add('active', 'btn-danger');
            }
            // 투표 수 업데이트
            document.querySelector('[data-vote="1"] .badge').textContent = data.agree_count;
            document.querySelector('[data-vote="0"] .badge').textContent = data.disagree_count;
        } else {
            alert('투표 처리에 실패했습니다.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('투표 처리 중 오류가 발생했습니다.');
    });
}

// 페이지 로드 시 사용자의 투표 상태 표시
document.addEventListener('DOMContentLoaded', function() {
    const userVote = {{ $board->userVote() ? ($board->userVote()->is_agree ? 'true' : 'false') : 'null' }};
    document.querySelectorAll('.vote-btn').forEach(btn => {
        btn.classList.remove('active', 'btn-success', 'btn-danger');
        btn.classList.add(btn.dataset.vote === '1' ? 'btn-outline-success' : 'btn-outline-danger');
    });
    if (userVote !== null) {
        if (userVote) {
            // 찬성
            const agreeBtn = document.querySelector('[data-vote="1"]');
            agreeBtn.classList.remove('btn-outline-success');
            agreeBtn.classList.add('active', 'btn-success');
        } else {
            // 반대
            const disagreeBtn = document.querySelector('[data-vote="0"]');
            disagreeBtn.classList.remove('btn-outline-danger');
            disagreeBtn.classList.add('active', 'btn-danger');
        }
    }
});

function copyToClipboard() {
    const text = `제목: {{ $board->title }}\n\n내용:\n{{ $board->content }}\n\n작성자: {{ $board->user->name }}\n작성일: {{ $board->created_at->format('Y-m-d H:i') }}`;

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
            alert('클립보드에 복사되었습니다.');
        }).catch(err => {
            fallbackCopyTextToClipboard(text);
        });
    } else {
        fallbackCopyTextToClipboard(text);
    }
}

function fallbackCopyTextToClipboard(text) {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.style.position = "fixed";
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
        document.execCommand('copy');
        alert('클립보드에 복사되었습니다.');
    } catch (err) {
        alert('복사에 실패했습니다.');
    }
    document.body.removeChild(textarea);
}

function showImageModal(url) {
    document.getElementById('imagePreviewModalImg').src = url;
    var modal = new bootstrap.Modal(document.getElementById('imagePreviewModal'));
    modal.show();
}
</script>
@endpush 