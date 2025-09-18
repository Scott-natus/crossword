<li class="list-group-item" style="margin-left: {{ $depth * 24 }}px; position: relative;">
    @if($depth > 0)
        <span style="position:absolute; left:-18px; top:10px; color:#888; font-size:18px;">&#x21B3;</span>
    @endif
    <div class="fw-bold">{{ $comment->user->name ?? '알 수 없음' }} <span class="text-muted small">{{ $comment->created_at->format('Y-m-d H:i') }}</span></div>
    <div>{{ $comment->content }}</div>
    @auth
        @if($depth < 19)
            <button class="btn btn-link btn-sm p-0" type="button" data-bs-toggle="collapse" data-bs-target="#replyForm{{ $comment->id }}">답글</button>
            <div class="collapse" id="replyForm{{ $comment->id }}">
                <form action="{{ route('board-comments.store') }}" method="POST" class="mt-2">
                    @csrf
                    <input type="hidden" name="board_id" value="{{ $comment->board_id }}">
                    <input type="hidden" name="parent_id" value="{{ $comment->id }}">
                    <textarea name="content" class="form-control mb-2" rows="2" placeholder="답글을 입력하세요" required></textarea>
                    <button type="submit" class="btn btn-primary btn-sm">답글 작성</button>
                </form>
            </div>
        @endif
        @if(auth()->user()->email === 'rainynux@gmail.com' || $comment->user_id == Auth::id())
            <form action="{{ route('board-comments.destroy', $comment->id) }}" method="POST" class="d-inline">
                @csrf
                @method('DELETE')
                <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('정말로 이 댓글을 삭제하시겠습니까?')">삭제</button>
            </form>
        @endif
    @endauth
    @if($comment->children->count())
        <ul class="list-group mt-2">
            @foreach($comment->children as $child)
                @include('board.partials.comment', ['comment' => $child, 'depth' => $depth + 1])
            @endforeach
        </ul>
    @endif
</li> 