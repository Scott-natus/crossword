@extends('layouts.app')

@section('title', '그리드 템플릿 목록')

@section('content')
<!-- 퍼즐 관리 바로가기 네비게이션 -->
<div class="container-fluid mb-4">
    <div class="card">
        <div class="card-body py-2">
            <div class="d-flex justify-content-between align-items-center">
                <h6 class="mb-0 text-primary">
                    <i class="fas fa-puzzle-piece me-2"></i>퍼즐 관리 바로가기
                </h6>
                <div class="d-flex gap-2">
                    <a href="{{ route('puzzle.words.index') }}" class="btn btn-primary btn-sm">
                        <i class="fas fa-list me-1"></i>단어 관리
                    </a>
                    <a href="{{ route('puzzle.hint-generator.index') }}" class="btn btn-info btn-sm">
                        <i class="fas fa-magic me-1"></i>AI 힌트 생성
                    </a>
                    <a href="{{ route('puzzle.levels.index') }}" class="btn btn-success btn-sm">
                        <i class="fas fa-layer-group me-1"></i>레벨 관리
                    </a>
                    <a href="{{ route('puzzle.grid-templates.index') }}" class="btn btn-warning btn-sm">
                        <i class="fas fa-th me-1"></i>그리드 템플릿관리
                    </a>
                    <div class="dropdown">
                        <button class="btn btn-outline-secondary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown">
                            <i class="fas fa-ellipsis-v"></i>
                        </button>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="{{ route('puzzle.words.index') }}">
                                <i class="fas fa-list me-2"></i>단어 관리
                            </a></li>
                            <li><a class="dropdown-item" href="{{ route('puzzle.hint-generator.index') }}">
                                <i class="fas fa-magic me-2"></i>AI 힌트 생성
                            </a></li>
                            <li><a class="dropdown-item" href="{{ route('puzzle.levels.index') }}">
                                <i class="fas fa-layer-group me-2"></i>레벨 관리
                            </a></li>
                            <li><a class="dropdown-item" href="{{ route('puzzle.grid-templates.index') }}">
                                <i class="fas fa-th me-2"></i>그리드 템플릿관리
                            </a></li>
                            <li><a class="dropdown-item" href="{{ route('admin.users.index') }}">
                                <i class="fas fa-users me-2"></i>회원 관리
                            </a></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="container-fluid">
    <div class="row">
        <div class="col-12">
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <h4 class="card-title">그리드 템플릿 목록</h4>
                        <p class="card-text">저장된 그리드 템플릿들을 관리할 수 있습니다.</p>
                    </div>
                    <div>
                        <button type="button" class="btn btn-outline-info me-2" onclick="showLevelSamples()">
                            <i class="fas fa-eye"></i> 레벨별 샘플보기
                        </button>
                        <a href="{{ route('puzzle.grid-templates.create') }}" class="btn btn-primary">
                            <i class="fas fa-plus"></i> 새 템플릿 생성
                        </a>
                    </div>
                </div>
                <div class="card-body">
                    @if($templates->count() > 0)
                        <div class="table-responsive">
                            <table class="table table-striped table-hover">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>레벨</th>
                                        <th>템플릿 이름</th>
                                        <th>그리드 크기</th>
                                        <th>단어 수</th>
                                        <th>교차점 수</th>
                                        <th>카테고리</th>
                                        <th>생성일</th>
                                        <th>작업</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    @foreach($templates as $template)
                                        <tr>
                                            <td>{{ $template->id }}</td>
                                            <td>
                                                <span class="badge bg-info">레벨 {{ $template->level }}</span>
                                                <br>
                                                <small class="text-muted">{{ $template->level_name }}</small>
                                            </td>
                                            <td>
                                                <strong>{{ $template->template_name }}</strong>
                                            </td>
                                            <td>{{ $template->grid_width }}×{{ $template->grid_height }}</td>
                                            <td>
                                                <span class="badge bg-success">{{ $template->word_count }}</span>
                                            </td>
                                            <td>
                                                <span class="badge bg-warning">{{ $template->intersection_count }}</span>
                                            </td>
                                            <td>
                                                @if($template->category === 'custom')
                                                    <span class="badge bg-primary">사용자 정의</span>
                                                @else
                                                    <span class="badge bg-secondary">{{ $template->category }}</span>
                                                @endif
                                            </td>
                                            <td>
                                                @if(is_string($template->created_at))
                                                    {{ \Carbon\Carbon::parse($template->created_at)->format('Y-m-d H:i') }}
                                                    <br>
                                                    <small class="text-muted">{{ \Carbon\Carbon::parse($template->created_at)->diffForHumans() }}</small>
                                                @else
                                                    {{ $template->created_at->format('Y-m-d H:i') }}
                                                    <br>
                                                    <small class="text-muted">{{ $template->created_at->diffForHumans() }}</small>
                                                @endif
                                            </td>
                                            <td>
                                                <div class="btn-group" role="group">
                                                    <a href="{{ route('puzzle.grid-templates.show', $template->id) }}" 
                                                       class="btn btn-sm btn-outline-primary" 
                                                       title="상세 보기">
                                                        <i class="fas fa-eye"></i>
                                                    </a>
                                                    <button type="button" 
                                                            class="btn btn-sm btn-outline-danger" 
                                                            onclick="deleteTemplate({{ $template->id }}, '{{ $template->template_name }}')"
                                                            title="삭제">
                                                        <i class="fas fa-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    @endforeach
                                </tbody>
                            </table>
                        </div>
                    @else
                        <div class="text-center py-5">
                            <i class="fas fa-puzzle-piece fa-3x text-muted mb-3"></i>
                            <h5 class="text-muted">저장된 템플릿이 없습니다</h5>
                            <p class="text-muted">새로운 그리드 템플릿을 생성해보세요.</p>
                            <a href="{{ route('puzzle.grid-templates.create') }}" class="btn btn-primary">
                                <i class="fas fa-plus"></i> 첫 번째 템플릿 생성
                            </a>
                        </div>
                    @endif
                </div>
            </div>
        </div>
    </div>
</div>

<!-- 삭제 확인 모달 -->
<div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">템플릿 삭제 확인</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>정말로 <strong id="deleteTemplateName"></strong> 템플릿을 삭제하시겠습니까?</p>
                <p class="text-danger"><small>이 작업은 되돌릴 수 없습니다.</small></p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button type="button" class="btn btn-danger" id="confirmDelete">삭제</button>
            </div>
        </div>
    </div>
</div>

<!-- 레벨별 샘플보기 모달 -->
<div class="modal fade" id="levelSamplesModal" tabindex="-1" data-bs-backdrop="true" data-bs-keyboard="true">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">레벨별 샘플 템플릿</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="row mb-3">
                    <div class="col-12">
                        <div class="alert alert-info">
                            <h6>사용 방법</h6>
                            <p class="mb-0">원하는 레벨을 선택하고 샘플 템플릿을 확인한 후, 템플릿 생성 페이지로 이동하여 샘플을 기반으로 새 템플릿을 만들 수 있습니다.</p>
                        </div>
                    </div>
                </div>
                <div class="row" id="levelsContainer">
                    <!-- 레벨 목록이 여기에 동적으로 생성됩니다 -->
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
            </div>
        </div>
    </div>
</div>

<!-- 레벨 샘플 템플릿 모달 -->
<div class="modal fade" id="levelSampleTemplatesModal" tabindex="-1" data-bs-backdrop="true" data-bs-keyboard="true">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">레벨 샘플 템플릿</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="row mb-3">
                    <div class="col-12">
                        <div class="alert alert-info">
                            <h6>레벨 정보</h6>
                            <div class="row">
                                <div class="col-md-3">
                                    <strong>레벨:</strong> <span id="modalLevelInfo">-</span>
                                </div>
                                <div class="col-md-3">
                                    <strong>단어 개수:</strong> <span id="modalWordCount">-</span>
                                </div>
                                <div class="col-md-3">
                                    <strong>교차점 개수:</strong> <span id="modalIntersectionCount">-</span>
                                </div>
                                <div class="col-md-3">
                                    <strong>난이도:</strong> <span id="modalDifficulty">-</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="row" id="modalSamplesContainer">
                    <!-- 샘플 템플릿들이 여기에 동적으로 생성됩니다 -->
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                <a href="{{ route('puzzle.grid-templates.create') }}" class="btn btn-primary" id="goToCreateBtn">
                    <i class="fas fa-plus"></i> 템플릿 생성 페이지로 이동
                </a>
            </div>
        </div>
    </div>
</div>
@endsection

@push('scripts')
<script>
let deleteTemplateId = null;
let currentLevels = [];
let currentSamples = [];

// 레벨별 샘플보기 모달 표시
function showLevelSamples() {
    loadLevels();
}

// 레벨 목록 로드
function loadLevels() {
    fetch('/puzzle/levels', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').getAttribute('content')
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentLevels = data.levels;
            displayLevels(data.levels);
            const modal = new bootstrap.Modal(document.getElementById('levelSamplesModal'));
            modal.show();
        } else {
            alert('레벨 목록 로드 실패: ' + data.message);
        }
    })
    .catch(error => {
        alert('레벨 목록 로드 중 오류가 발생했습니다.');
    });
}

// 레벨 목록 표시
function displayLevels(levels) {
    const container = document.getElementById('levelsContainer');
    container.innerHTML = '';
    
    levels.forEach((level, index) => {
        const levelDiv = document.createElement('div');
        levelDiv.className = 'col-md-4 mb-3';
        levelDiv.innerHTML = `
            <div class="card h-100 level-card" data-level-id="${level.id}">
                <div class="card-header">
                    <h6 class="card-title mb-0">레벨 ${level.level} - ${level.level_name}</h6>
                </div>
                <div class="card-body">
                    <div class="row mb-2">
                        <div class="col-6">
                            <small class="text-muted">단어 수:</small><br>
                            <span class="badge bg-primary">${level.word_count}</span>
                        </div>
                        <div class="col-6">
                            <small class="text-muted">교차점:</small><br>
                            <span class="badge bg-warning">${level.intersection_count}</span>
                        </div>
                    </div>
                    <div class="row mb-2">
                        <div class="col-6">
                            <small class="text-muted">단어 난이도:</small><br>
                            <span class="badge bg-info">${level.word_difficulty}</span>
                        </div>
                        <div class="col-6">
                            <small class="text-muted">힌트 난이도:</small><br>
                            <span class="badge bg-secondary">${level.hint_difficulty}</span>
                        </div>
                    </div>
                    <div class="text-center mt-3">
                        <button type="button" class="btn btn-sm btn-outline-info view-samples-btn" data-level-id="${level.id}">
                            <i class="fas fa-eye"></i> 샘플보기
                        </button>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(levelDiv);
    });
    
    // 샘플보기 버튼 이벤트 추가
    document.querySelectorAll('.view-samples-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const levelId = parseInt(this.getAttribute('data-level-id'));
            viewLevelSamples(levelId);
        });
    });
}

// 레벨 샘플 템플릿 보기
function viewLevelSamples(levelId) {
    const level = currentLevels.find(l => l.id === levelId);
    if (!level) {
        alert('레벨 정보를 찾을 수 없습니다.');
        return;
    }
    
    // 레벨 샘플 모달 닫기
    const levelModal = bootstrap.Modal.getInstance(document.getElementById('levelSamplesModal'));
    levelModal.hide();
    
    // 샘플 템플릿 로드
    fetch('/puzzle/grid-templates/sample-templates', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').getAttribute('content')
        },
        body: JSON.stringify({
            level_id: levelId
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentSamples = data.samples;
            displayModalSampleTemplates(level, data.samples);
            const modal = new bootstrap.Modal(document.getElementById('levelSampleTemplatesModal'));
            modal.show();
        } else {
            alert('샘플 템플릿 로드 실패: ' + data.message);
        }
    })
    .catch(error => {
        alert('샘플 템플릿 로드 중 오류가 발생했습니다.');
    });
}

// 모달에서 샘플 템플릿 표시
function displayModalSampleTemplates(level, samples) {
    // 레벨 정보 업데이트
    document.getElementById('modalLevelInfo').textContent = `레벨 ${level.level} - ${level.level_name}`;
    document.getElementById('modalWordCount').textContent = level.word_count;
    document.getElementById('modalIntersectionCount').textContent = level.intersection_count;
    document.getElementById('modalDifficulty').textContent = `${level.word_difficulty} (${level.hint_difficulty})`;
    
    // 샘플 컨테이너 초기화
    const container = document.getElementById('modalSamplesContainer');
    container.innerHTML = '';
    
    // 샘플 템플릿들 생성
    samples.forEach((sample, index) => {
        const sampleDiv = document.createElement('div');
        sampleDiv.className = 'col-md-4 mb-3';
        sampleDiv.innerHTML = `
            <div class="card h-100 sample-card" data-index="${index}">
                <div class="card-header">
                    <h6 class="card-title mb-0">${sample.name}</h6>
                </div>
                <div class="card-body">
                    <p class="card-text small">${sample.description}</p>
                    <div class="sample-grid-container mb-2">
                        ${generateSampleGridHTML(sample)}
                    </div>
                    <div class="text-center">
                        <span class="badge bg-success">샘플 ${index + 1}</span>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(sampleDiv);
    });
    
    // 템플릿 생성 페이지로 이동 버튼에 레벨 정보 추가
    const goToCreateBtn = document.getElementById('goToCreateBtn');
    goToCreateBtn.href = `/puzzle/grid-templates/create?level_id=${level.id}`;
}

// 공통 그리드 렌더링 함수 (상세보기와 동일)
function renderGridFromAPI(template, container, cellSize = 20, showNumbers = true) {
    const gridContainer = $(container);
    gridContainer.empty();
    
    // 로딩 표시
    gridContainer.html('<div class="text-center"><i class="fas fa-spinner fa-spin"></i> 그리드 로딩 중...</div>');
    
    $.ajax({
        url: 'http://222.100.103.227:8081/K-CrossWord/admin/api/grid-template-management/render-grid',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            gridPattern: JSON.stringify(template.grid),
            wordPositions: JSON.stringify(template.word_positions),
            cellSize: cellSize,
            showNumbers: showNumbers
        }),
        success: function(response) {
            if (response.success) {
                gridContainer.html(response.html);
            } else {
                gridContainer.html('<p class="text-danger">그리드 렌더링 실패: ' + response.error + '</p>');
            }
        },
        error: function(xhr, status, error) {
            gridContainer.html('<p class="text-danger">그리드 렌더링 중 오류가 발생했습니다.</p>');
        }
    });
}

// 샘플 그리드 HTML 생성 (공통 API 사용)
function generateSampleGridHTML(sample) {
    // 임시 컨테이너 생성
    const tempContainer = $('<div class="sample-grid-container"></div>');
    
    // 공통 그리드 렌더링 함수 사용 (20px 셀 크기, 번호 표시)
    renderGridFromAPI(sample, tempContainer, 20, true);
    
    return tempContainer[0].outerHTML;
}

function deleteTemplate(id, name) {
    deleteTemplateId = id;
    document.getElementById('deleteTemplateName').textContent = name;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}

// DOM이 완전히 로드된 후 이벤트 연결
document.addEventListener('DOMContentLoaded', function() {
    const confirmDeleteBtn = document.getElementById('confirmDelete');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', () => {
            if (!deleteTemplateId) return;
            
            fetch(`/puzzle/grid-templates/${deleteTemplateId}`, {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]').getAttribute('content')
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    // 성공 메시지 표시
                    const toast = document.createElement('div');
                    toast.className = 'toast align-items-center text-white bg-success border-0 position-fixed top-0 end-0 m-3';
                    toast.style.zIndex = '9999';
                    toast.innerHTML = `
                        <div class="d-flex">
                            <div class="toast-body">
                                <i class="fas fa-check-circle"></i> ${data.message}
                            </div>
                            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                        </div>
                    `;
                    document.body.appendChild(toast);
                    
                    const bsToast = new bootstrap.Toast(toast);
                    bsToast.show();
                    
                    // 페이지 새로고침
                    setTimeout(() => {
                        location.reload();
                    }, 1000);
                } else {
                    alert('삭제 중 오류가 발생했습니다: ' + data.message);
                }
            })
            .catch(error => {
                alert('삭제 중 오류가 발생했습니다.');
            })
            .finally(() => {
                const modal = bootstrap.Modal.getInstance(document.getElementById('deleteModal'));
                modal.hide();
                deleteTemplateId = null;
            });
        });
    }
});
</script>

@push('styles')
<style>
.level-card {
    transition: all 0.3s ease;
    cursor: pointer;
}

.level-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.sample-grid {
    max-width: 100%;
    overflow: hidden;
}

.sample-cell {
    transition: transform 0.2s;
}

.sample-card:hover .sample-cell {
    transform: scale(1.1);
}

.sample-card {
    transition: all 0.3s ease;
    cursor: pointer;
}

.sample-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.sample-grid-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 120px;
}
</style>
@endpush 