class CrosswordGrid {
    constructor(container, options = {}) {
        this.container = container;
        this.grid = options.grid || [];
        this.wordPositions = options.wordPositions || [];
        this.userInput = options.userInput || {};
        this.selectedCell = options.selectedCell || null;
        this.foundWords = options.foundWords || new Set();
        this.onCellSelect = options.onCellSelect || (() => {});
        this.onCharInput = options.onCharInput || (() => {});
        
        this.init();
    }

    init() {
        this.render();
        this.bindEvents();
    }

    render() {
        this.container.innerHTML = '';
        this.container.className = 'crossword-grid';

        this.grid.forEach((row, y) => {
            const rowElement = document.createElement('div');
            rowElement.className = 'grid-row';

            row.forEach((cell, x) => {
                const cellElement = document.createElement('div');
                cellElement.className = this.getCellClassName(x, y);
                cellElement.dataset.x = x;
                cellElement.dataset.y = y;

                if (!cell.isBlack) {
                    const content = document.createElement('span');
                    content.className = 'cell-content';
                    content.textContent = this.getCellValue(x, y);
                    cellElement.appendChild(content);
                }

                rowElement.appendChild(cellElement);
            });

            this.container.appendChild(rowElement);
        });
    }

    getCellClassName(x, y) {
        const cell = this.grid[y][x];
        let className = 'grid-cell';

        if (cell.isBlack) className += ' black';
        if (this.isSelected(x, y)) className += ' selected';
        if (this.isInFoundWord(x, y)) className += ' found';
        if (this.isCellInWord(x, y)) className += ' word-cell';

        return className;
    }

    getCellValue(x, y) {
        const cell = this.grid[y][x];
        if (cell.isBlack) return '';

        const inputValue = this.userInput[`${x},${y}`];
        if (inputValue) return inputValue;

        return cell.char || '';
    }

    isCellInWord(x, y) {
        return this.wordPositions.some(wordPos =>
            wordPos.positions.some(pos => pos.x === x && pos.y === y)
        );
    }

    isSelected(x, y) {
        return this.selectedCell && this.selectedCell.x === x && this.selectedCell.y === y;
    }

    isInFoundWord(x, y) {
        return this.wordPositions.some(wordPos => {
            if (!this.foundWords.has(wordPos.word)) return false;
            return wordPos.positions.some(pos => pos.x === x && pos.y === y);
        });
    }

    bindEvents() {
        this.container.addEventListener('click', (e) => {
            const cell = e.target.closest('.grid-cell');
            if (!cell) return;

            const x = parseInt(cell.dataset.x);
            const y = parseInt(cell.dataset.y);

            if (this.grid[y][x].isBlack) return;

            this.onCellSelect(x, y);
        });

        document.addEventListener('keydown', (e) => {
            if (!this.selectedCell) return;

            const { x, y } = this.selectedCell;
            const key = e.key;

            if (key === 'Backspace') {
                this.onCharInput(x, y, '');
                return;
            }

            if (key === 'ArrowLeft' || key === 'ArrowRight' || key === 'ArrowUp' || key === 'ArrowDown') {
                e.preventDefault();
                let newX = x;
                let newY = y;

                switch (key) {
                    case 'ArrowLeft':
                        newX = Math.max(0, x - 1);
                        break;
                    case 'ArrowRight':
                        newX = Math.min(this.grid[0].length - 1, x + 1);
                        break;
                    case 'ArrowUp':
                        newY = Math.max(0, y - 1);
                        break;
                    case 'ArrowDown':
                        newY = Math.min(this.grid.length - 1, y + 1);
                        break;
                }

                this.onCellSelect(newX, newY);
                return;
            }

            // 한글 입력 처리
            if (/^[가-힣]$/.test(key)) {
                this.onCharInput(x, y, key);
            }
        });
    }

    update(newData) {
        if (newData.grid) this.grid = newData.grid;
        if (newData.wordPositions) this.wordPositions = newData.wordPositions;
        if (newData.userInput) this.userInput = newData.userInput;
        if (newData.selectedCell) this.selectedCell = newData.selectedCell;
        if (newData.foundWords) this.foundWords = newData.foundWords;

        this.render();
    }
}

// 전역으로 사용할 수 있도록 window 객체에 추가
window.CrosswordGrid = CrosswordGrid;
