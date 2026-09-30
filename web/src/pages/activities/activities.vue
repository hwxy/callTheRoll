<script setup>
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api, auth, requireUser, notify } from '../../lib/api'
import { createXlsxRosterTemplate, parseXlsxRoster } from '../../lib/demoRoster'
const user = ref(null),
  items = ref([]),
  total = ref(0),
  page = ref(1),
  busy = ref(false),
  error = ref(''),
  feedbackVisible = ref(false),
  feedbackTitle = ref(''),
  feedbackContent = ref(''),
  feedbackContact = ref(''),
  feedbackBusy = ref(false),
  feedbackError = ref(''),
  createVisible = ref(false),
  editingActivityId = ref(null),
  editingVersion = ref(null),
  createName = ref(''),
  createRosterText = ref(''),
  sharedTeacherAccounts = ref(''),
  availableTeachers = ref([]),
  createRepeatDraw = ref(false),
  createBusy = ref(false),
  importing = ref(false),
  importError = ref(''),
  importedFileName = ref('')
const maximumRosterSize = 200
const createRoster = computed(() => {
  const rows = createRosterText.value
    .split(/[\r\n]+/)
    .flatMap((row) => row.split(/[,，;；\t]+/))
    .map((name) => name.trim())
    .filter(Boolean)
  if (rows.length && ['姓名', '学生姓名', '名字', 'name', 'student name'].includes(rows[0].toLowerCase())) rows.shift()
  return rows
})
let autoEntered = false
async function load(more = false) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    const me = await requireUser()
    if (!me) return
    user.value = me
    const next = more ? page.value + 1 : 1
    const r = await api(`/activities?page=${next}&size=30`)
    items.value = more ? [...items.value, ...r.records] : r.records
    page.value = next
    total.value = r.total
    if (me.role === 'STUDENT' && r.total === 1 && !autoEntered) {
      autoEntered = true
      enter(r.records[0])
    }
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
    uni.stopPullDownRefresh()
  }
}
async function openCreate() {
  editingActivityId.value = null
  editingVersion.value = null
  createName.value = ''
  createRosterText.value = ''
  sharedTeacherAccounts.value = ''
  createRepeatDraw.value = false
  importError.value = ''
  importedFileName.value = ''
  try {
    availableTeachers.value = await api('/teachers')
    createVisible.value = true
  } catch (e) {
    notify(e)
  }
}
async function editActivity(activity) {
  createBusy.value = true
  importError.value = ''
  try {
    const [detail, teachers] = await Promise.all([
      api(`/activities/${activity.id}`),
      api('/teachers'),
    ])
    editingActivityId.value = activity.id
    editingVersion.value = detail.activity.version
    createName.value = detail.activity.name
    createRosterText.value = (detail.members || []).map((member) => member.name).join('\n')
    createRepeatDraw.value = detail.activity.repeatDraw
    availableTeachers.value = teachers
    sharedTeacherAccounts.value = (detail.teacherIds || [])
      .map((id) => teachers.find((teacher) => teacher.id === id))
      .filter(Boolean)
      .map((teacher) => teacher.phone || teacher.studentNo)
      .join('\n')
    importedFileName.value = ''
    createVisible.value = true
  } catch (e) {
    notify(e)
  } finally {
    createBusy.value = false
  }
}
function closeCreate() {
  if (!createBusy.value) createVisible.value = false
}
function updateCreateRoster(event) {
  createRosterText.value = event.detail.value
  importError.value = ''
  importedFileName.value = ''
}
function importFailed(message) {
  importError.value = message
  uni.showToast({ title: message, icon: 'none', duration: 2600 })
}
function applyExcelRoster(names, fileName) {
  if (names.length > maximumRosterSize) {
    importFailed(`名单最多支持 ${maximumRosterSize} 位同学`)
    return
  }
  createRosterText.value = names.join('\n')
  importedFileName.value = fileName
  importError.value = ''
  uni.showToast({ title: `已读取 ${names.length} 位同学`, icon: 'success' })
}
function parseExcelBytes(bytes, fileName) {
  if (!/\.xlsx$/i.test(fileName || '')) return importFailed('请选择 .xlsx 格式的 Excel 文件')
  if (bytes.byteLength > 5 * 1024 * 1024) return importFailed('Excel 文件不能超过 5MB')
  importing.value = true
  try {
    applyExcelRoster(parseXlsxRoster(bytes), fileName)
  } catch (e) {
    importFailed(e.message || '读取失败，请检查 Excel 文件格式')
  } finally {
    importing.value = false
  }
}
function chooseExcel() {
  importError.value = ''
  // #ifdef H5
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  input.style.position = 'fixed'
  input.style.left = '-9999px'
  input.addEventListener('change', (event) => {
    importH5File(event)
    input.remove()
  }, { once: true })
  input.addEventListener('cancel', () => input.remove(), { once: true })
  document.body.appendChild(input)
  input.click()
  // #endif
  // #ifdef MP-WEIXIN
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['xlsx'],
    success(result) {
      const file = result.tempFiles?.[0]
      const filePath = file?.path || result.tempFilePaths?.[0]
      if (!file || !filePath) return importFailed('没有读取到所选文件')
      if (file.size > 5 * 1024 * 1024) return importFailed('Excel 文件不能超过 5MB')
      importing.value = true
      uni.getFileSystemManager().readFile({
        filePath,
        success(readResult) { parseExcelBytes(new Uint8Array(readResult.data), file.name) },
        fail() { importing.value = false; importFailed('读取文件失败，请重新选择') },
      })
    },
  })
  // #endif
}
async function importH5File(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    parseExcelBytes(new Uint8Array(await file.arrayBuffer()), file.name)
  } catch {
    importFailed('读取文件失败，请重新选择')
  }
}
function downloadRosterTemplate() {
  const bytes = createXlsxRosterTemplate()
  // #ifdef H5
  const blob = new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '点名星球学生名单模板.xlsx'
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  // #endif
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/点名星球学生名单模板.xlsx`
  const data = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)
  uni.getFileSystemManager().writeFile({
    filePath,
    data,
    success() { uni.openDocument({ filePath, fileType: 'xlsx', showMenu: true }) },
    fail() { importFailed('生成模板失败，请重试') },
  })
  // #endif
}
async function createActivity() {
  const name = createName.value.trim()
  if (!name) return importFailed('请填写活动名称')
  if (!createRoster.value.length) return importFailed('请先录入学生名单或导入 Excel')
  if (createRoster.value.length > maximumRosterSize) return importFailed(`名单最多支持 ${maximumRosterSize} 位同学`)
  const teacherAccounts = sharedTeacherAccounts.value.split(/[\r\n,，;；\t ]+/).map((account) => account.trim()).filter(Boolean)
  const selectedTeachers = []
  for (const account of teacherAccounts) {
    const teacher = availableTeachers.value.find((item) => item.phone === account || item.studentNo === account)
    if (!teacher) return importFailed(`未找到有效老师账号「${account}」`)
    if (!selectedTeachers.includes(teacher.id)) selectedTeachers.push(teacher.id)
  }
  createBusy.value = true
  importError.value = ''
  try {
    await api(editingActivityId.value ? `/activities/${editingActivityId.value}` : '/activities', {
      method: editingActivityId.value ? 'PUT' : 'POST',
      body: {
        name,
        repeatDraw: createRepeatDraw.value,
        studentIds: [],
        teacherIds: selectedTeachers,
        studentNames: createRoster.value,
        ...(editingActivityId.value ? { version: editingVersion.value } : {}),
      },
    })
    createVisible.value = false
    uni.showToast({ title: editingActivityId.value ? '活动已更新' : '活动创建成功', icon: 'success' })
    editingActivityId.value = null
    await load()
  } catch (e) {
    importError.value = e.message || '创建失败，请稍后重试'
  } finally {
    createBusy.value = false
  }
}
function enter(a) {
  const kind = user.value.role === 'TEACHER' ? 'teacher' : 'student'
  uni.navigateTo({ url: `/pages/${kind}/${kind}?id=${a.id}` })
}
async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' })
  } catch (e) {
    notify(e)
  } finally {
    auth.clear()
    uni.reLaunch({ url: '/pages/index/index' })
  }
}
function openHelp() {
  uni.navigateTo({ url: '/pages/help/help' })
}
function showFeedback() {
  feedbackError.value = ''
  feedbackVisible.value = true
}
function closeFeedback() {
  if (!feedbackBusy.value) feedbackVisible.value = false
}
async function submitFeedback() {
  const title = feedbackTitle.value.trim()
  const content = feedbackContent.value.trim()
  if (!title || !content) {
    feedbackError.value = '请填写问题标题和具体描述'
    return
  }
  feedbackBusy.value = true
  feedbackError.value = ''
  try {
    await api('/feedback', {
      method: 'POST',
      body: { title, content, contact: feedbackContact.value.trim() },
    })
    feedbackVisible.value = false
    feedbackTitle.value = ''
    feedbackContent.value = ''
    feedbackContact.value = ''
    uni.showToast({ title: '提交成功', icon: 'success' })
  } catch (e) {
    feedbackError.value = e.message || '提交失败，请稍后重试'
  } finally {
    feedbackBusy.value = false
  }
}
onShow(() => load())
onPullDownRefresh(() => load())
</script>
<template>
  <view class="app-page"
  ><view class="page-top"
      ><view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view
      ><view class="top-actions activity-actions"><button class="text-button help-action" @click="openHelp">使用帮助</button
        ><button class="text-button feedback-action" @click="showFeedback">问题反馈</button
        ><button class="text-button logout-action" @click="logout">退出</button></view></view
    ><view class="hero-copy compact"
      ><text class="overline">YOUR CLASSROOM GARDEN</text
      ><text class="hero-title">你好，{{ user?.name || '同学' }}<text class="gold"> ✦</text></text
      ><text class="subtext">{{
        user?.role === 'TEACHER'
          ? '选择一场活动，开始今天的课堂。'
          : '选择一场活动，看看伙伴又长大了多少。'
      }}</text></view
    ><button v-if="user?.role === 'TEACHER'" class="primary-button create-activity-trigger" @click="openCreate">＋ 创建活动</button
    ><view v-if="error" class="error-card"
      ><text>{{ error }}</text
      ><button class="text-button" @click="load()">重试</button></view
    ><text v-if="busy" class="hint center">正在加载课堂…</text
    ><view class="activity-list"
      ><view v-for="(a, index) in items" :key="a.id" class="activity-tile" @click="enter(a)"
        ><view class="tile-symbol" :class="`color-${index % 3}`">{{
          ['✳', '✦', '❋'][index % 3]
        }}</view
        ><view class="tile-info"
          ><text class="section-title">{{ a.name }}</text
          ><text class="hint"
            >{{
              user?.role === 'STUDENT'
                ? '我的成长伙伴'
                : a.creatorId === user?.id
                  ? '我创建的'
                  : '共享给我的'
            }}
            · 第{{ a.roundNo }}轮</text
          ></view
        ><button v-if="user?.role === 'TEACHER'" class="tile-edit" @click.stop="editActivity(a)">编辑</button><button class="tile-view" @click.stop="enter(a)">查看</button></view
      ></view
    ><view v-if="!busy && !error && !items.length" class="empty-box"
      ><text class="empty-symbol">❋</text><text class="section-title">还没有活动</text
      ><text class="hint">请创建一场活动，录入学生名单后开始点名。</text
      ><button v-if="user?.role === 'TEACHER'" class="primary-button empty-create" @click="openCreate">创建第一场活动 ↗</button></view
    ><button
      v-if="items.length < total"
      class="secondary-button"
      :loading="busy"
      @click="load(true)"
    >
      加载更多</button
    ><text class="footnote">共 {{ total }} 场活动 · 每一场都是新的开始</text>
    <view v-if="createVisible" class="roster-modal-backdrop" @click="closeCreate">
      <view class="roster-modal" @click.stop>
        <view class="roster-modal-heading"><view><text class="overline">{{ editingActivityId ? 'EDIT CLASSROOM' : 'NEW CLASSROOM' }}</text><text class="section-title">{{ editingActivityId ? '编辑课堂活动' : '创建课堂活动' }}</text></view><button class="modal-close" :disabled="createBusy" @click="closeCreate">×</button></view>
        <view class="roster-modal-body">
        <text class="input-label">活动名称</text>
        <input v-model="createName" class="text-input roster-title-input" maxlength="100" placeholder="例如：三年级 · 数学探索课堂" />
        <view class="roster-field-heading"><text class="input-label">参与学生</text><text class="roster-count">{{ createRoster.length }} / {{ maximumRosterSize }} 人</text></view>
        <textarea class="text-input roster-modal-input" :value="createRosterText" maxlength="8000" placeholder="每行输入一位同学，例如：&#10;林小满&#10;陈星野&#10;许知夏" @input="updateCreateRoster" />
        <view class="roster-modal-tools"><button class="roster-tool" @click="createRosterText = '林小满\n陈星野\n许知夏'; importError = ''">放入示例名单</button><button class="roster-tool" :disabled="importing" @click="chooseExcel">{{ importing ? '读取中…' : '▦ 导入 Excel' }}</button><button class="roster-tool" @click="downloadRosterTemplate">↓ 下载 Excel 模板</button></view>
        <text class="roster-modal-hint">支持多行粘贴；可再次导入 Excel 替换名单。保存时同名学生会保留原积分和宠物成长；重名按不同学生保留。</text>
        <text v-if="importedFileName" class="roster-import-success">✓ 已导入 {{ importedFileName }}</text>
        <text v-if="importError" class="roster-import-error">{{ importError }}</text>
        <view class="roster-preview-line"><text>名单预览</text><text>{{ createRoster.slice(0, 6).join('、') }}{{ createRoster.length > 6 ? ` 等 ${createRoster.length} 人` : '' }}</text></view>
        <text class="input-label share-label">共享给其他老师（账号选填）</text>
        <textarea v-model="sharedTeacherAccounts" class="text-input shared-teacher-input" maxlength="2000" placeholder="每行填写一个老师的账号，例如：&#10;13800138000&#10;T10086" />
        <text class="roster-modal-hint">保存时会校验老师账号是否存在且已启用；共享老师可共同管理活动。</text>
        <view class="repeat-option" :class="{ selected: createRepeatDraw }" @click="createRepeatDraw = !createRepeatDraw"><text class="repeat-check">{{ createRepeatDraw ? '✓' : '' }}</text><view><text>允许重复点名</text><text class="roster-modal-hint">关闭时，本轮每位学生只会被抽中一次。</text></view></view>
        </view>
        <view class="roster-modal-actions"><button class="roster-cancel" :disabled="createBusy" @click="closeCreate">取消</button><button class="roster-submit" :disabled="createBusy" @click="createActivity">{{ createBusy ? '正在保存…' : editingActivityId ? '保存活动' : '创建活动并开始' }} <text>↗</text></button></view>
      </view>
    </view>
    <view v-if="feedbackVisible" class="feedback-overlay" @click="closeFeedback">
      <view class="feedback-dialog" @click.stop>
        <view class="feedback-dialog-head">
          <view>
            <text class="feedback-eyebrow">YOUR VOICE MATTERS</text>
            <text class="feedback-dialog-title">告诉我们遇到的问题</text>
          </view>
          <button class="feedback-close" :disabled="feedbackBusy" aria-label="关闭" @click="closeFeedback">×</button>
        </view>
        <text class="feedback-intro">描述你在使用点名星球时遇到的情况，我们会认真查看。</text>
        <input v-model="feedbackTitle" class="feedback-field" maxlength="120" placeholder="问题标题（必填）" />
        <textarea v-model="feedbackContent" class="feedback-field feedback-content" maxlength="4000" placeholder="请尽量写清楚操作步骤和遇到的现象（必填）" />
        <input v-model="feedbackContact" class="feedback-field" maxlength="120" placeholder="联系方式（选填，便于我们回复）" />
        <text class="feedback-privacy">请勿填写密码等敏感信息。提交内容仅供问题排查。</text>
        <text v-if="feedbackError" class="feedback-error">{{ feedbackError }}</text>
        <view class="feedback-actions">
          <button class="feedback-cancel" :disabled="feedbackBusy" @click="closeFeedback">暂不提交</button>
          <button class="feedback-submit" :disabled="feedbackBusy" @click="submitFeedback">{{ feedbackBusy ? '正在提交…' : '提交问题' }} <text v-if="!feedbackBusy">↗</text></button>
        </view>
      </view>
    </view>
  </view
  >
</template>

<style scoped>
.activity-actions .feedback-action { min-width: 76px; }
.tile-edit, .tile-view { flex: 0 0 auto; margin: 0; padding: 7px 11px; border: 1px solid #dce7d8; border-radius: 8px; background: #f7faf3; color: #557463; font-size: 11px; line-height: 1.3; }
.tile-view { background: #eaf2e8; color: #376b54; }
.create-activity-trigger { width: auto; max-width: none; min-width: 132px; margin: 0 0 19px; padding: 10px 16px; font-size: 13px; letter-spacing: .2px; }
.empty-create { max-width: 240px; margin-top: 17px; }
.roster-modal-backdrop { position: fixed; z-index: 400; inset: 0; display: flex; align-items: center; justify-content: center; padding: 18px; background: rgba(29, 52, 43, .42); backdrop-filter: blur(5px); }
.roster-modal { box-sizing: border-box; display: flex; flex-direction: column; width: 100%; max-width: 560px; max-height: 92vh; max-height: 92dvh; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; padding: 26px; border: 1px solid #e5ecdf; border-radius: 22px; background: #fffef9; box-shadow: 0 26px 80px rgba(31, 60, 43, .25); }
.roster-modal-heading { display: flex; flex: 0 0 auto; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.roster-modal-body { flex: 0 0 auto; min-height: 0; overflow: visible; padding: 0 0 10px; }
.roster-modal-heading .section-title { display: block; margin-top: 7px; color: #315747; font-size: 21px; font-weight: 700; }
.modal-close { width: 34px; height: 34px; margin: 0; padding: 0; border: 1px solid #e3ebdf; border-radius: 50%; background: #f7f9f2; color: #718779; font-size: 23px; line-height: 1; }
.roster-title-input { margin-bottom: 15px; }
.share-label { display: block; margin: 7px 0 8px; }
.shared-teacher-input { display: block; box-sizing: border-box; width: 100%; height: 92px; min-height: 92px; max-height: 92px; overflow-y: auto; resize: none; margin: 0 0 7px; padding: 11px 13px; border: 1px solid #e2e9dc; border-radius: 10px; background: #f8faf4; color: #435f50; font-size: 12px; line-height: 1.6; }
.roster-field-heading { display: flex; align-items: center; justify-content: space-between; }
.roster-field-heading .input-label { margin-bottom: 9px; }
.roster-count { color: #238779; font-size: 12px; font-weight: 700; }
.roster-modal-input { display: block; box-sizing: border-box; width: 100%; height: 150px; min-height: 150px; max-height: 150px; overflow-y: auto; resize: none; margin: 0 0 10px; padding: 13px 14px; border: 1px solid #e2e9dc; border-radius: 11px; background: #f8faf4; color: #435f50; font-size: 13px; line-height: 1.65; }
.roster-modal-input :deep(.uni-textarea-textarea), .shared-teacher-input :deep(.uni-textarea-textarea) { scrollbar-width: none; -ms-overflow-style: none; }
.roster-modal-input :deep(.uni-textarea-textarea::-webkit-scrollbar),
.shared-teacher-input :deep(.uni-textarea-textarea::-webkit-scrollbar) { display: none !important; width: 0 !important; height: 0 !important; }
.roster-modal-tools { display: flex; flex-wrap: wrap; gap: 8px; margin: 4px 0 10px; }
.roster-tool { margin: 0; padding: 8px 11px; border: 1px solid #dce7d8; border-radius: 9px; background: #fff; color: #557463; font-size: 11px; line-height: 1.3; }
.roster-modal-hint { display: block; color: #8b9a8d; font-size: 10px; line-height: 1.65; }
.roster-import-success, .roster-import-error { display: block; margin-top: 8px; font-size: 11px; }
.roster-import-success { color: #31806d; }
.roster-import-error { color: #b95f4f; }
.roster-preview-line { display: flex; justify-content: space-between; gap: 14px; margin-top: 16px; padding: 12px 0; border-top: 1px solid #edf0e8; color: #597364; font-size: 11px; }
.roster-preview-line text:last-child { text-align: right; color: #89988a; }
.repeat-option { display: flex; align-items: flex-start; gap: 10px; margin-top: 5px; padding: 12px; border: 1px solid #e8ecdf; border-radius: 11px; color: #526f5b; font-size: 12px; }
.repeat-option.selected { background: #f3f7ec; border-color: #cddfca; }
.repeat-check { display: flex; flex: 0 0 18px; width: 18px; height: 18px; align-items: center; justify-content: center; border: 1px solid #bdcfb3; border-radius: 5px; color: #287f70; }
.repeat-option .roster-modal-hint { margin-top: 4px; }
.roster-modal-actions { position: sticky; z-index: 2; bottom: -26px; display: flex; flex: 0 0 auto; justify-content: flex-end; gap: 9px; margin: 0 -26px -26px; padding: 12px 26px 26px; border-top: 1px solid #edf0e8; background: #fffef9; }
.roster-modal-actions button { margin: 0; padding: 10px 15px; border-radius: 9px; font-size: 11px; line-height: 1.25; }
.roster-cancel { border: 1px solid #e2e9dd; background: #fff; color: #718476; }
.roster-submit { border: 1px solid #137d76; background: #177f75; color: #fff; font-weight: 700; }
.roster-submit text { margin-left: 6px; color: #f3d77d; }
.feedback-overlay { position: fixed; z-index: 200; inset: 0; display: flex; align-items: center; justify-content: center; padding: 18px; background: rgba(29, 52, 43, .38); backdrop-filter: blur(5px); }
.feedback-dialog { width: 100%; max-width: 520px; padding: 27px; border: 1px solid rgba(230,237,224,.9); border-radius: 22px; background: #fffef9; box-shadow: 0 24px 70px rgba(24, 54, 39, .22); }
.feedback-dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }
.feedback-eyebrow { display: block; color: #83a090; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; }
.feedback-dialog-title { display: block; margin-top: 9px; color: #315747; font-size: 21px; font-weight: 700; }
.feedback-close { width: 30px; height: 30px; margin: 0; padding: 0; border: 1px solid #e4ebdf; border-radius: 50%; background: #f8faf4; color: #718779; font-size: 22px; line-height: 1; }
.feedback-intro { display: block; margin: 9px 0 19px; color: #87968a; font-size: 11px; line-height: 1.7; }
.feedback-field { display: block; box-sizing: border-box; width: 100%; height: 44px; margin: 0 0 11px; padding: 12px 13px; border: 1px solid #e2e9dc; border-radius: 10px; background: #fbfcf7; color: #435f50; font-size: 12px; line-height: 1.6; }
.feedback-field :deep(.uni-input-input), .feedback-field :deep(.uni-input-placeholder) { box-sizing: border-box; color: #435f50; font-size: 12px; line-height: 1.6; }
.feedback-field :deep(.uni-input-placeholder) { color: #a0aea1; }
.feedback-content { height: 140px; min-height: 140px; max-height: 140px; }
.feedback-privacy { display: block; color: #9aa79b; font-size: 10px; line-height: 1.6; }
.feedback-error { display: block; margin-top: 9px; color: #b95f4f; font-size: 11px; }
.feedback-actions { display: flex; justify-content: flex-end; gap: 9px; margin-top: 19px; }
.feedback-actions button { margin: 0; padding: 10px 15px; border-radius: 9px; font-size: 11px; line-height: 1.2; }
.feedback-cancel { border: 1px solid #e2e9dd; background: #fff; color: #718476; }
.feedback-submit { border: 1px solid #137d76; background: #177f75; color: #fff; font-weight: 700; }
.feedback-submit text { margin-left: 6px; color: #f3d77d; }
.feedback-actions button[disabled], .feedback-close[disabled] { opacity: .55; }
@media (max-width: 420px) {
  .page-top { gap: 4px; }
  .page-top .app-brand { gap: 4px; font-size: 14px; }
  .page-top .brand-stamp { width: 28px; height: 28px; }
  .activity-actions .help-action, .activity-actions .feedback-action { min-width: 58px; padding-right: 4px; padding-left: 4px; font-size: 10px; }
  .activity-actions .logout-action { min-width: 34px; padding-right: 3px; padding-left: 3px; font-size: 10px; }
  .roster-modal { max-height: 94vh; max-height: 94dvh; padding: 18px 16px; border-radius: 18px; }
  .roster-modal-input { height: 130px; min-height: 130px; max-height: 130px; }
  .roster-preview-line { flex-direction: column; gap: 5px; }
  .roster-preview-line text:last-child { text-align: left; }
  .feedback-dialog { padding: 21px 17px; border-radius: 18px; }
  .feedback-dialog-title { font-size: 19px; }
}
</style>
