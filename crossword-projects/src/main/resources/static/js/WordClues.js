class WordClues {
    constructor(container, options = {}) {
        this.container = container;
        this.words = options.words || [];
        this.wordPositions = options.wordPositions || [];
        this.foundWords = options.foundWords || new Set();
        this.selectedWord = options.selectedWord || null;
        this.onWordSelect = options.onWordSelect || (() => {});
        
        this.init();
    }

    init() {
        this.render();
    }

    render() {
        this.container.innerHTML = '';
        this.container.className = 'word-clues';

        // 단어들을 가로/세로로 분류 (위치 정보를 기반으로)
        const horizontalWords = [];
        const verticalWords = [];
        
        this.words.forEach((word, index) => {
            const wordPosition = this.wordPositions.find(wp => wp.word_id === word.id);
            if (wordPosition && wordPosition.positions && wordPosition.positions.length > 0) {
                const firstPos = wordPosition.positions[0];
                const secondPos = wordPosition.positions[1];
                
                if (secondPos && firstPos.y === secondPos.y) {
                    // 가로 단어 (y 좌표가 같음)
                    horizontalWords.push({...word, direction: 0});
                } else if (secondPos && firstPos.x === secondPos.x) {
                    // 세로 단어 (x 좌표가 같음)
                    verticalWords.push({...word, direction: 1});
                } else {
                    // 기본적으로 가로로 분류
                    horizontalWords.push({...word, direction: 0});
                }
            } else {
                // 위치 정보가 없으면 기본적으로 가로로 분류
                horizontalWords.push({...word, direction: 0});
            }
        });

        // 가로 단어들
        const horizontalSection = this.createCluesSection('가로', horizontalWords, 0);
        this.container.appendChild(horizontalSection);

        // 세로 단어들
        const verticalSection = this.createCluesSection('세로', verticalWords, horizontalWords.length);
        this.container.appendChild(verticalSection);

        // 진행 상황 요약
        const progressSummary = this.createProgressSummary();
        this.container.appendChild(progressSummary);
    }

    createCluesSection(title, words, startIndex) {
        const section = document.createElement('div');
        section.className = 'clues-section';

        const titleElement = document.createElement('h3');
        titleElement.textContent = title;
        section.appendChild(titleElement);

        const cluesList = document.createElement('div');
        cluesList.className = 'clues-list';

        words.forEach((word, index) => {
            const clueItem = document.createElement('div');
            clueItem.className = this.getClueItemClassName(word);
            clueItem.dataset.wordId = word.id;

            const clueNumber = document.createElement('span');
            clueNumber.className = 'clue-number';
            clueNumber.textContent = `${startIndex + index + 1}.`;
            clueItem.appendChild(clueNumber);

            const clueText = document.createElement('span');
            clueText.className = 'clue-text';
            clueText.textContent = word.hint || word.clue || '힌트 없음';
            clueItem.appendChild(clueText);

            if (this.isWordFound(word)) {
                const foundIndicator = document.createElement('span');
                foundIndicator.className = 'found-indicator';
                foundIndicator.textContent = '✓';
                clueItem.appendChild(foundIndicator);
            }

            clueItem.addEventListener('click', () => {
                const wordPosition = this.wordPositions.find(wp => wp.word_id === word.id);
                this.onWordSelect(wordPosition);
            });

            cluesList.appendChild(clueItem);
        });

        section.appendChild(cluesList);
        return section;
    }

    createProgressSummary() {
        const summary = document.createElement('div');
        summary.className = 'progress-summary';
        
        const progressText = document.createElement('p');
        progressText.textContent = `완성된 단어: ${this.foundWords.size} / ${this.words.length}`;
        summary.appendChild(progressText);

        return summary;
    }

    getClueItemClassName(word) {
        let className = 'clue-item';

        if (this.isWordFound(word)) className += ' found';
        if (this.isWordSelected(word)) className += ' selected';

        return className;
    }

    isWordFound(word) {
        return this.foundWords.has(word.word);
    }

    isWordSelected(word) {
        return this.selectedWord && this.selectedWord.word_id === word.id;
    }

    update(newData) {
        if (newData.words) this.words = newData.words;
        if (newData.wordPositions) this.wordPositions = newData.wordPositions;
        if (newData.foundWords) this.foundWords = newData.foundWords;
        if (newData.selectedWord) this.selectedWord = newData.selectedWord;

        this.render();
    }
}

// 전역으로 사용할 수 있도록 window 객체에 추가
window.WordClues = WordClues;
