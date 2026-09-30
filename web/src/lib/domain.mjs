export const pets = [
  { key: 'cat', name: '奶糖猫', note: '把好奇心，变成超能力', color: '#edc684' },
  { key: 'rabbit', name: '云朵兔', note: '小步向前，也是一大步', color: '#e9c9bf' },
  { key: 'dragon', name: '青芽龙', note: '勇敢一点，成长多一点', color: '#97b987' },
]
export const levelFor = (points) => 1 + Math.floor(Math.max(0, points) / 10)
export const scaleFor = (level) => 0.7 + (Math.min(10, Math.max(1, level)) - 1) * 0.055
export const destinationFor = (role) =>
  role === 'TEACHER' ? '/pages/activities/activities' : null
export function requestKey() {
  return `draw_${Date.now()}_${Math.random().toString(36).slice(2, 14)}`
}
