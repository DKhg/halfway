const CHOSEONG = [
  'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
  'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
]

const HANGUL_SYLLABLE_START = 0xac00
const HANGUL_SYLLABLE_END = 0xd7a3
const CHOSEONG_UNIT = 588

// 완성형 한글 음절에서 초성만 추출, 음절이 아닌 문자는 그대로 둠
export function getChoseong(text) {
  return [...text]
    .map((char) => {
      const code = char.codePointAt(0)
      if (code >= HANGUL_SYLLABLE_START && code <= HANGUL_SYLLABLE_END) {
        const choseongIndex = Math.floor((code - HANGUL_SYLLABLE_START) / CHOSEONG_UNIT)
        return CHOSEONG[choseongIndex]
      }
      return char
    })
    .join('')
}

// 입력값이 초성으로만 이루어졌는지 (초성 검색 모드 판단용)
export function isChoseongOnly(text) {
  return text.length > 0 && [...text].every((char) => CHOSEONG.includes(char))
}

// 초성 입력이면 초성끼리, 아니면 일반 텍스트로 앞부분 일치 비교
export function matchesStationQuery(name, query) {
  if (!query) {
    return true
  }
  if (isChoseongOnly(query)) {
    return getChoseong(name).startsWith(query)
  }
  return name.startsWith(query)
}
