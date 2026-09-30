export const roleNames = { ADMIN: '系统管理员', TEACHER: '老师', STUDENT: '学生' }
export function canUseConsole(user) {
  return ['ADMIN', 'TEACHER'].includes(user?.role)
}
export function menusFor(user) {
  if (!canUseConsole(user)) return []
  return [
    { key: 'activities', label: '活动创建', icon: '◈' },
    ...(user.role === 'ADMIN' ? [{ key: 'roles', label: '角色管理', icon: '◇' }] : []),
    ...(user.role === 'ADMIN'
      ? [
          { key: 'accounts', label: '账号管理', icon: '◎' },
          { key: 'analytics', label: 'PV/UV 统计', icon: '⌁' },
          { key: 'settings', label: '网站设置', icon: '⚙' },
          { key: 'feedback', label: '问题查看', icon: '✉' },
        ]
      : []),
  ]
}
