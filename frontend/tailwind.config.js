/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        // 日系清新色板：宣纸底 + 轻墨文字 + 粉彩点缀
        paper: '#fafaf8',
        ink: '#4a5568',
        'ink-sub': '#5f6e80',
        'ink-weak': '#6b7787',
        hairline: 'rgba(212, 212, 207, 0.3)',
        sky: '#64b5f6',
        mint: '#98d8c8',
        pink: '#ffb7c5',
        powder: '#b8d4e3'
      },
      fontFamily: {
        sans: [
          '-apple-system',
          'PingFang SC',
          'Hiragino Sans',
          'Noto Sans SC',
          'Microsoft YaHei',
          'Helvetica Neue',
          'Arial',
          'sans-serif'
        ]
      },
      letterSpacing: {
        jp: '0.02em',
        jpwide: '0.14em'
      },
      borderRadius: {
        jp: '12px',
        'jp-btn': '8px'
      }
    }
  },
  plugins: []
}
