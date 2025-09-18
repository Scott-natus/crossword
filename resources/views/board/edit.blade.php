@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <h2 class="mb-3">{{ $boardType->name }} - 게시글 수정</h2>
            <div class="card">
                <div class="card-header">게시글 수정</div>

                <div class="card-body">
                    <form method="POST" action="{{ route('board.update', ['boardType' => $boardType->slug, 'board' => $board->id]) }}" enctype="multipart/form-data">
                        @csrf
                        @method('PUT')

                        <div class="mb-3">
                            <label for="title" class="form-label">제목</label>
                            <input type="text" class="form-control @error('title') is-invalid @enderror" 
                                id="title" name="title" value="{{ old('title', $board->title) }}" required>
                            @error('title')
                                <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>

                        <div class="mb-3">
                            <label for="content" class="form-label">내용</label>
                            <div id="editor" class="form-control @error('content') is-invalid @enderror" 
                                contenteditable="true" style="min-height: 300px; overflow-y: auto; padding: 10px;"
                                data-placeholder="내용을 입력하세요...">{!! old('content', $board->content) !!}</div>
                            <textarea id="content" name="content" style="display: none;"></textarea>
                            @error('content')
                                <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>

                        <div class="mb-3">
                            @if(Auth::user()->email === 'rainynux@gmail.com')
                                <label for="password" class="form-label">관리자 마스터 비밀번호</label>
                                <input type="password" class="form-control @error('password') is-invalid @enderror" 
                                    id="password" name="password" required placeholder="tngkrrhk">
                                <div class="form-text text-muted">관리자는 모든 글을 수정할 수 있습니다.</div>
                            @else
                                <label for="password" class="form-label">게시글 비밀번호</label>
                                <input type="password" class="form-control @error('password') is-invalid @enderror" 
                                    id="password" name="password" required>
                            @endif
                            @error('password')
                                <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>

                        <div class="mb-3">
                            <div class="form-check">
                                <input type="checkbox" class="form-check-input" id="comment_notify" 
                                    name="comment_notify" {{ $board->comment_notify ? 'checked' : '' }}>
                                <label class="form-check-label" for="comment_notify">댓글 알림 받기</label>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">현재 첨부파일</label>
                            <div class="list-group" id="existing-attachments">
                                @foreach($board->attachments as $attachment)
                                    <div class="list-group-item d-flex justify-content-between align-items-center" data-attachment-id="{{ $attachment->id }}">
                                        <span>{{ $attachment->original_name }}</span>
                                        <button type="button" class="btn btn-danger btn-sm" 
                                            onclick="deleteAttachment({{ $attachment->id }})">삭제</button>
                                    </div>
                                @endforeach
                            </div>
                            <input type="hidden" name="deleted_attachments" id="deleted_attachments" value="">
                        </div>

                        <div class="mb-3">
                            <label for="attachments" class="form-label">새 첨부파일</label>
                            <input type="file" class="form-control @error('attachments.*') is-invalid @enderror" 
                                id="attachments" name="attachments[]" multiple>
                            <div class="form-text">이미지, 동영상, PDF, 문서 파일 등 최대 100MB까지 업로드 가능합니다.</div>
                            @error('attachments.*')
                                <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>

                        <div class="d-flex justify-content-between">
                            <a href="{{ route('board.show', ['boardType' => $boardType, 'board' => $board->id]) }}" class="btn btn-secondary">취소</a>
                            <button type="submit" class="btn btn-primary">수정하기</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

@push('styles')
<style>
    #editor {
        border: 1px solid #ced4da;
        border-radius: 0.25rem;
        background-color: #fff;
    }
    #editor:empty:before {
        content: attr(data-placeholder);
        color: #6c757d;
        font-style: italic;
    }
    #editor:focus {
        border-color: #86b7fe;
        outline: 0;
        box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
    }
</style>
@endpush

@push('scripts')
<script>
// 전역 변수로 선언
let deletedAttachments = [];

document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form');
    const editor = document.getElementById('editor');
    const contentTextarea = document.getElementById('content');
    const deletedAttachmentsInput = document.getElementById('deleted_attachments');

    // 폼 제출 시 contenteditable div의 내용을 textarea로 복사
    form.addEventListener('submit', function(e) {
        // 디버깅을 위한 콘솔 로그 추가
        console.log('Editor content before submit:', editor.innerHTML);
        
        // contenteditable div의 내용을 textarea로 복사
        contentTextarea.value = editor.innerHTML;
        
        // 삭제된 첨부파일 ID들을 hidden input에 설정
        deletedAttachmentsInput.value = JSON.stringify(deletedAttachments);
        
        // 디버깅을 위한 콘솔 로그 추가
        console.log('Textarea content after copy:', contentTextarea.value);
        console.log('Deleted attachments array:', deletedAttachments);
        console.log('Deleted attachments JSON:', deletedAttachmentsInput.value);
        
        if (!contentTextarea.value.trim()) {
            e.preventDefault();
            alert('내용을 입력해주세요.');
            return;
        }

        // 폼 데이터 확인
        const formData = new FormData(form);
        console.log('Form data content:', formData.get('content'));
        console.log('Form data deleted_attachments:', formData.get('deleted_attachments'));
        
        // 폼 제출 전 최종 확인
        console.log('Form is being submitted with deleted attachments:', deletedAttachmentsInput.value);
    });

    // 붙여넣기 이벤트 처리 (StartFragment/EndFragment 제거)
    editor.addEventListener('paste', function(e) {
        e.preventDefault();
        let html = (e.clipboardData || window.clipboardData).getData('text/html');
        let text = (e.clipboardData || window.clipboardData).getData('text/plain');

        if (html) {
            // StartFragment/EndFragment 사이만 추출
            const start = html.indexOf('<!--StartFragment-->');
            const end = html.indexOf('<!--EndFragment-->');
            if (start !== -1 && end !== -1) {
                html = html.substring(start + 20, end);
            }
            document.execCommand('insertHTML', false, html);
        } else {
            document.execCommand('insertText', false, text);
        }
        
        // 붙여넣기 후 textarea 업데이트
        contentTextarea.value = editor.innerHTML;
    });

    // 에디터 내용 변경 시 textarea 업데이트
    editor.addEventListener('input', function() {
        contentTextarea.value = editor.innerHTML;
    });

    // 초기 로드 시 textarea 업데이트
    contentTextarea.value = editor.innerHTML;
});

// 첨부파일 삭제 함수 (전역 함수로 정의)
function deleteAttachment(attachmentId) {
    console.log('deleteAttachment called with ID:', attachmentId);
    
    if (confirm('이 첨부파일을 삭제하시겠습니까?')) {
        // 삭제된 첨부파일 ID를 배열에 추가
        deletedAttachments.push(attachmentId);
        console.log('Added to deletedAttachments array:', deletedAttachments);
        
        // 화면에서 해당 첨부파일 요소 제거
        const attachmentElement = document.querySelector(`[data-attachment-id="${attachmentId}"]`);
        if (attachmentElement) {
            attachmentElement.remove();
            console.log('Removed attachment element from DOM');
        }
        
        // 삭제된 첨부파일이 없으면 메시지 표시
        const existingAttachments = document.getElementById('existing-attachments');
        if (existingAttachments && existingAttachments.children.length === 0) {
            existingAttachments.innerHTML = '<div class="list-group-item text-muted">삭제된 첨부파일이 없습니다.</div>';
        }
        
        // hidden input 업데이트
        const deletedAttachmentsInput = document.getElementById('deleted_attachments');
        if (deletedAttachmentsInput) {
            deletedAttachmentsInput.value = JSON.stringify(deletedAttachments);
            console.log('Updated hidden input value:', deletedAttachmentsInput.value);
        }
    }
}
</script>
@endpush
@endsection 