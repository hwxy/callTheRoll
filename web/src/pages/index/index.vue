<script setup>
import { computed, ref } from 'vue'
import { onShow, onHide, onUnload } from '@dcloudio/uni-app'
import Pet from '../../components/Pet.vue'
import { api, auth } from '../../lib/api'
import { createXlsxRosterTemplate, parseXlsxRoster } from '../../lib/demoRoster'

const exampleNames = ['林小满', '陈星野', '许知夏', '周一诺', '沈小禾', '李沐阳']
const rawRoster = ref('')
const selected = ref('先准备一份名单')
const rolling = ref(false)
const repeatMode = ref(true)
const drawnIds = ref([])
const errorMessage = ref('')
const importedFileName = ref('')
const importing = ref(false)
const feedbackVisible = ref(false)
const feedbackTitle = ref('')
const feedbackContent = ref('')
const feedbackContact = ref('')
const feedbackBusy = ref(false)
const feedbackError = ref('')
const maximumRosterSize = 200

const roster = computed(() => {
  const rows = rawRoster.value
    .split(/[\r\n]+/)
    .flatMap((row) => row.split(/[,，;；\t]+/))
    .map((name) => name.trim())
    .filter(Boolean)
  if (rows.length && ['姓名', '学生姓名', '名字', 'name', 'student name'].includes(rows[0].toLowerCase())) {
    rows.shift()
  }
  return rows.map((name, index) => ({ id: index + 1, name }))
})

const previewRoster = computed(() => roster.value.slice(0, 8))
const extraRosterCount = computed(() => Math.max(0, roster.value.length - previewRoster.value.length))
const overLimit = computed(() => roster.value.length > maximumRosterSize)
const duplicateNames = computed(() => {
  const counts = new Map()
  roster.value.forEach(({ name }) => counts.set(name, (counts.get(name) || 0) + 1))
  return [...counts.entries()].filter(([, count]) => count > 1).map(([name]) => name)
})
const remainingRoster = computed(() => roster.value.filter(({ id }) => !drawnIds.value.includes(id)))
const roundFinished = computed(
  () => !repeatMode.value && roster.value.length > 0 && remainingRoster.value.length === 0,
)
const canDraw = computed(
  () => roster.value.length > 0 && !overLimit.value && !rolling.value && !roundFinished.value,
)

let timer
let stopTimer

function stop() {
  clearInterval(timer)
  clearTimeout(stopTimer)
  timer = undefined
  stopTimer = undefined
  rolling.value = false
}

function updateRoster(event) {
  stop()
  rawRoster.value = event.detail.value
  drawnIds.value = []
  errorMessage.value = ''
  importedFileName.value = ''
  selected.value = roster.value.length ? '名单准备好了' : '先准备一份名单'
}

function useExample() {
  updateRoster({ detail: { value: exampleNames.join('\n') } })
}

function clearRoster() {
  updateRoster({ detail: { value: '' } })
}

function changeMode(repeat) {
  if (repeatMode.value === repeat) return
  stop()
  repeatMode.value = repeat
  drawnIds.value = []
  selected.value = '新的一轮，开始吧'
}

function resetRound() {
  drawnIds.value = []
  selected.value = '新的一轮，开始吧'
}

function draw() {
  if (!canDraw.value) return
  errorMessage.value = ''
  const candidates = repeatMode.value ? roster.value : remainingRoster.value
  if (!candidates.length) return

  const target = candidates[Math.floor(Math.random() * candidates.length)]
  rolling.value = true
  timer = setInterval(() => {
    const name = roster.value[Math.floor(Math.random() * roster.value.length)]?.name
    if (name) selected.value = name
  }, 82)
  stopTimer = setTimeout(() => {
    clearInterval(timer)
    timer = undefined
    selected.value = target.name
    if (!repeatMode.value) drawnIds.value = [...drawnIds.value, target.id]
    rolling.value = false
    stopTimer = undefined
  }, 1300)
}

function notifyImportError(message) {
  errorMessage.value = message
  uni.showToast({ title: message, icon: 'none', duration: 2600 })
}

function applyExcelNames(names, fileName = '') {
  if (names.length > maximumRosterSize) {
    notifyImportError(`名单最多支持 ${maximumRosterSize} 位同学`)
    return
  }
  updateRoster({ detail: { value: names.join('\n') } })
  importedFileName.value = fileName
  uni.showToast({ title: `已读取 ${names.length} 位同学`, icon: 'success' })
}

function parseExcelBytes(bytes, fileName) {
  if (!/\.xlsx$/i.test(fileName || '')) {
    notifyImportError('请选择 .xlsx 格式的 Excel 文件')
    return
  }
  if (bytes.byteLength > 5 * 1024 * 1024) {
    notifyImportError('Excel 文件不能超过 5MB')
    return
  }

  importing.value = true
  try {
    const names = parseXlsxRoster(bytes)
    applyExcelNames(names, fileName)
  } catch (error) {
    notifyImportError(error.message || '读取失败，请检查 Excel 文件格式')
  } finally {
    importing.value = false
  }
}

function chooseExcel() {
  errorMessage.value = ''
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
      if (!file || !filePath) {
        notifyImportError('没有读取到所选文件')
        return
      }
      if (file.size > 5 * 1024 * 1024) {
        notifyImportError('Excel 文件不能超过 5MB')
        return
      }
      importing.value = true
      uni.getFileSystemManager().readFile({
        filePath,
        success(readResult) {
          parseExcelBytes(new Uint8Array(readResult.data), file.name)
        },
        fail() {
          importing.value = false
          notifyImportError('读取文件失败，请重新选择')
        },
      })
    },
    fail() {
      // 用户取消选择时不打断体验流程。
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
    notifyImportError('读取文件失败，请重新选择')
  }
}

function downloadExcelTemplate() {
  const bytes = createXlsxRosterTemplate()
  // #ifdef H5
  const blob = new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '点名体验名单模板.xlsx'
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  // #endif
  // #ifdef MP-WEIXIN
  const filePath = `${wx.env.USER_DATA_PATH}/点名体验名单模板.xlsx`
  const data = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)
  uni.getFileSystemManager().writeFile({
    filePath,
    data,
    success() {
      uni.openDocument({
        filePath,
        fileType: 'xlsx',
        showMenu: true,
        success() {
          uni.showToast({ title: '模板已打开，可从菜单保存', icon: 'none' })
        },
        fail() {
          notifyImportError('模板已生成，但打开失败')
        },
      })
    },
    fail() {
      notifyImportError('生成模板失败，请重试')
    },
  })
  // #endif
}

function login() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function registerTeacher() {
  uni.navigateTo({ url: '/pages/register/register' })
}

function openHelp() {
  uni.navigateTo({ url: '/pages/help/help' })
}

function getVisitorId() {
  const key = 'rollcall-home-visitor-id'
  const saved = uni.getStorageSync(key)
  if (typeof saved === 'string' && /^[A-Za-z0-9_-]{20,80}$/.test(saved)) return saved

  const bytes = new Uint8Array(24)
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) crypto.getRandomValues(bytes)
  else bytes.forEach((_, index) => { bytes[index] = Math.floor(Math.random() * 256) })
  const visitorId = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  uni.setStorageSync(key, visitorId)
  return visitorId
}

function recordHomepageView() {
  api('/analytics/home-visit', {
    method: 'POST',
    body: { visitorId: getVisitorId() },
  }).catch(() => {})
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
  } catch (error) {
    feedbackError.value = error.message || '提交失败，请稍后重试'
  } finally {
    feedbackBusy.value = false
  }
}

onShow(() => {
  if (typeof document !== 'undefined') {
    document.title = '课堂随机点名小程序｜班级点名与课堂互动工具 - 点名星球'
  }
  recordHomepageView()
  if (auth.token()) uni.reLaunch({ url: '/pages/activities/activities' })
})
onHide(stop)
onUnload(stop)
</script>

<template>
  <view class="app-page home-v2">
    <view class="home-header">
      <view class="app-brand">
        <text class="brand-stamp">点</text>
        <text>点名星球</text>
        <text class="brand-tag">课堂成长伙伴</text>
      </view>
      <view class="home-nav">
        <button class="home-link feedback-trigger" @click="showFeedback">✎ 问题反馈</button>
        <button class="home-link nav-about-link" @click="openHelp">关于我们</button>
        <button class="home-register" @click="registerTeacher">账号注册</button>
        <button class="home-login" @click="login">账号登录 <text>↗</text></button>
      </view>
    </view>

    <view class="home-intro">
      <view class="intro-copy">
        <text class="overline">OPEN CLASSROOM · DEMO MODE</text>
        <text class="home-title">先放进名单，<br />马上试一轮点名。</text>
        <text class="home-subtitle">粘贴学生姓名，或导入 Excel。准备好后，点一下就能开始。</text>
      </view>
      <view class="privacy-note"><text class="privacy-dot">●</text><text>免登录体验 · 名单仅在本机处理，不上传、不记积分</text></view>
    </view>

    <view class="home-workbench">
      <view class="roster-card">
        <view class="card-heading">
          <view>
            <text class="card-kicker">STEP 01 / YOUR ROSTER</text>
            <text class="card-title">准备体验名单</text>
          </view>
          <text class="roster-count">{{ roster.length }}<text> / {{ maximumRosterSize }} 人</text></text>
        </view>

        <textarea
          class="roster-input"
          :value="rawRoster"
          maxlength="8000"
          placeholder="每行输入一位同学，例如：&#10;林小满&#10;陈星野&#10;许知夏"
          placeholder-class="roster-placeholder"
          @input="updateRoster"
        />

        <view class="roster-tools">
          <button class="tool-button sample-button" @click="useExample">放入示例名单</button>
          <button class="tool-button" :disabled="!rawRoster" @click="clearRoster">清空</button>
          <button class="tool-button excel-button" :disabled="importing" @click="chooseExcel">
            <text class="sheet-icon">▦</text>{{ importing ? '读取中…' : '导入 Excel' }}
          </button>
          <button class="tool-button template-button" @click="downloadExcelTemplate">↓ 下载 Excel 模板</button>
        </view>
        <text class="roster-hint">支持多行粘贴，Excel 支持 .xlsx；读取首个工作表的“姓名”列或第一列</text>

        <view v-if="errorMessage" class="import-error">{{ errorMessage }}</view>
        <view v-if="importedFileName" class="file-note"><text>✓</text> 已导入 {{ importedFileName }}</view>

        <view class="roster-preview">
          <view class="preview-heading">
            <text class="preview-label">名单预览</text>
            <text class="preview-state">{{ roster.length ? '确认后即可点名' : '等你添加同学' }}</text>
          </view>
          <view v-if="roster.length" class="name-chips">
            <text v-for="person in previewRoster" :key="person.id" class="name-chip">{{ person.name }}</text>
            <text v-if="extraRosterCount" class="name-chip more-chip">+{{ extraRosterCount }}</text>
          </view>
          <view v-else class="empty-roster"><text class="empty-roster-icon">＋</text><text>粘贴名单，或用示例名单快速开始</text></view>
          <text v-if="duplicateNames.length" class="duplicate-note">
            检测到重名：{{ duplicateNames.slice(0, 3).join('、') }}。每一行都会作为一位同学参与抽取。
          </text>
          <text v-if="overLimit" class="duplicate-note">名单超过 {{ maximumRosterSize }} 人，请删减后再开始。</text>
        </view>
      </view>

      <view class="draw-card">
        <view class="draw-card-head">
          <view><text class="card-kicker">STEP 02 / LET’S DRAW</text><text class="draw-card-title">轮到谁来闪耀？</text></view>
          <text class="demo-stamp">演示点名</text>
        </view>

        <view class="draw-modes">
          <view class="mode-option" :class="{ active: repeatMode }" @click="changeMode(true)">
            <text class="mode-icon">↻</text><text>可以重复</text>
          </view>
          <view class="mode-option" :class="{ active: !repeatMode }" @click="changeMode(false)">
            <text class="mode-icon">✓</text><text>本轮不重复</text>
          </view>
        </view>

        <view class="draw-illustration">
          <view class="draw-orbit orbit-one"></view><view class="draw-orbit orbit-two"></view>
          <text class="sparkle sparkle-one">✦</text><text class="sparkle sparkle-two">✦</text>
          <Pet kind="cat" :level="4" :animate="!rolling" />
        </view>

        <view class="result-box" :class="{ active: rolling, ready: roster.length && !rolling }">
          <text class="result-name">{{ selected }}</text>
          <text class="result-caption">
            {{ rolling ? '名字正在星球间跳跃…' : roundFinished ? '本轮同学都已被点到' : roster.length ? '每一份参与，都值得被看见' : '添加名单后，抽取按钮就会点亮' }}
          </text>
        </view>

        <button class="draw-button" :disabled="!canDraw" @click="draw">
          {{ rolling ? '正在抽取…' : roundFinished ? '本轮已完成' : roster.length ? '开始随机点名' : '先添加体验名单' }}
          <text>✦</text>
        </button>
        <button v-if="roundFinished" class="reset-round" @click="resetRound">重新开始一轮 ↻</button>
        <view v-else-if="!repeatMode && roster.length" class="round-progress">本轮还可抽取 {{ remainingRoster.length }} 位同学</view>
        <view v-else class="local-footnote"><text>✦</text> 你的名单只用于本次体验</view>
      </view>
    </view>

    <view class="home-footer">
      <text>让每一次课堂参与，都有自己的高光时刻。</text>
      <view>
        <button class="home-link footer-about-link" @click="openHelp">了解点名星球</button>
        <button class="home-link footer-login-link" @click="login">账号登录 <text>↗</text></button>
        <button class="home-link footer-register-link" @click="registerTeacher">账号注册</button>
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
        <input
          v-model="feedbackTitle"
          class="feedback-field"
          maxlength="120"
          placeholder="问题标题（必填）"
        />
        <textarea
          v-model="feedbackContent"
          class="feedback-field feedback-content"
          maxlength="4000"
          placeholder="请尽量写清楚操作步骤和遇到的现象（必填）"
        />
        <input
          v-model="feedbackContact"
          class="feedback-field"
          maxlength="120"
          placeholder="联系方式（选填，便于我们回复）"
        />
        <text class="feedback-privacy">请勿填写密码等敏感信息。提交内容仅供问题排查。</text>
        <text v-if="feedbackError" class="feedback-error">{{ feedbackError }}</text>
        <view class="feedback-actions">
          <button class="feedback-cancel" :disabled="feedbackBusy" @click="closeFeedback">暂不提交</button>
          <button class="feedback-submit" :disabled="feedbackBusy" @click="submitFeedback">
            {{ feedbackBusy ? '正在提交…' : '提交问题' }} <text v-if="!feedbackBusy">↗</text>
          </button>
        </view>
      </view>
    </view>
    <!-- #ifdef H5 -->
    <ContactFloat />
    <!-- #endif -->
  </view>
</template>

<style scoped>
.home-v2 {
  width: 100%;
  max-width: 1180px;
  min-height: 100vh;
  padding: 28px 44px 26px;
  color: #28483e;
  background:
    radial-gradient(ellipse at 85% 16%, rgba(206, 232, 205, 0.52), transparent 32%),
    radial-gradient(ellipse at 3% 57%, rgba(239, 222, 182, 0.34), transparent 27%);
}
.home-header,
.app-brand,
.home-nav,
.card-heading,
.roster-tools,
.preview-heading,
.draw-card-head,
.home-footer,
.home-footer > view {
  display: flex;
  align-items: center;
}
.home-header,
.card-heading,
.preview-heading,
.draw-card-head,
.home-footer {
  justify-content: space-between;
}
.home-header { margin-bottom: 42px; }
.app-brand { gap: 10px; font-size: 20px; font-weight: 700; letter-spacing: .5px; }
.brand-stamp { display: flex; width: 38px; height: 38px; align-items: center; justify-content: center; border-radius: 13px 13px 13px 4px; background: #137d76; color: #fff; font-size: 19px; }
.brand-tag { margin-left: 3px; color: #81968b; font-size: 11px; font-weight: 400; letter-spacing: 1px; }
.home-nav { gap: 22px; }
.home-link { min-width: 0; margin: 0; padding: 9px 0; background: transparent; color: #627d70; font-size: 13px; line-height: 1.2; }
.feedback-trigger { padding: 9px 12px; border: 1px solid #d5e6d8; border-radius: 999px; background: rgba(242, 248, 239, .8); color: #387666; transition: background .18s ease, border-color .18s ease, transform .18s ease; }
.nav-about-link { padding: 9px 14px; border: 1px solid rgba(208, 224, 210, .9); border-radius: 999px; background: rgba(255,255,255,.48); color: #507263; transition: background .18s ease, border-color .18s ease, transform .18s ease; }
.home-login { margin: 0; padding: 10px 17px; border: 1px solid #cfe1d5; border-radius: 999px; background: #eaf4ed; color: #286153; font-size: 13px; font-weight: 600; line-height: 1.2; transition: background .18s ease, box-shadow .18s ease, transform .18s ease; }
.home-register { margin: 0; padding: 9px 12px; border: 1px solid #d6e8db; border-radius: 999px; background: rgba(255,255,255,.7); color: #387666; font-size: 12px; line-height: 1.2; white-space: nowrap; }
.feedback-trigger,.nav-about-link,.home-register,.home-login { background: #f1f6ee; }
.home-login text { margin-left: 8px; color: #da8a51; }
.home-intro { display: flex; align-items: end; justify-content: space-between; gap: 30px; margin: 0 0 25px; }
.intro-copy { max-width: 710px; }
.overline,.card-kicker { display: block; color: #72958b; font-size: 10px; font-weight: 700; letter-spacing: 1.8px; }
.home-title { display: block; margin-top: 13px; color: #244b41; font-family: 'STSong', 'Songti SC', 'Noto Serif SC', serif; font-size: 43px; font-weight: 700; letter-spacing: .2px; line-height: 1.35; }
.home-subtitle { display: block; margin-top: 9px; color: #748b7e; font-size: 14px; line-height: 1.8; }
.privacy-note { display: flex; align-items: center; flex-shrink: 0; gap: 8px; padding: 11px 14px; border: 1px solid #deeadc; border-radius: 999px; background: rgba(255,255,255,.64); color: #708777; font-size: 10px; }
.privacy-dot { color: #70aa87; font-size: 8px; }
.home-workbench { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(360px, .92fr); gap: 20px; align-items: stretch; }
.roster-card,.draw-card { min-width: 0; border: 1px solid #e3e9da; border-radius: 23px; box-shadow: 0 15px 44px rgba(48, 83, 62, .055); }
.roster-card { padding: 25px 26px 22px; background: rgba(255,255,250,.94); }
.card-heading { margin-bottom: 15px; }
.card-title,.draw-card-title { display: block; margin-top: 7px; color: #315447; font-size: 19px; font-weight: 700; }
.roster-count { color: #1b8277; font-size: 25px; font-weight: 700; line-height: 1; }
.roster-count text { color: #92a198; font-size: 10px; font-weight: 400; }
.roster-input { display: block; box-sizing: border-box; width: 100%; height: 180px; min-height: 180px; max-height: 180px; overflow: hidden; padding: 0; border: 1px solid #e3e9dc; border-radius: 13px; background: #f9faf4; color: #3e5a4d; font-size: 14px; line-height: 1.85; }
.roster-input :deep(.uni-textarea-textarea) { box-sizing: border-box; overflow-x: hidden; padding: 14px 15px; border: 0; background: transparent; color: #3e5a4d; font-size: 14px; line-height: 1.85; }
.roster-input :deep(.roster-placeholder) { box-sizing: border-box; overflow: hidden; padding: 14px 15px; color: #a5b0a3; font-size: 13px; line-height: 1.85; }
.roster-tools { flex-wrap: wrap; gap: 9px; margin-top: 12px; }
.tool-button { min-width: 0; margin: 0; padding: 8px 12px; border: 1px solid #e2e9dc; border-radius: 9px; background: #fff; color: #738779; font-size: 11px; }
.tool-button[disabled] { opacity: .45; }
.sample-button { border-color: #cfe4d9; background: #eef7f0; color: #297466; }
.excel-button { margin: 0; border-color: #d9e8d7; color: #407453; }
.template-button { color: #658474; }
.sheet-icon { margin-right: 6px; color: #5d9c65; font-size: 13px; }
.roster-hint { display: block; margin-top: 9px; color: #99a69a; font-size: 10px; line-height: 1.7; }
.import-error,.duplicate-note { display: block; margin-top: 9px; color: #bd795e; font-size: 10px; line-height: 1.7; }
.file-note { margin-top: 9px; color: #48825f; font-size: 10px; }
.file-note text { margin-right: 5px; }
.roster-preview { margin-top: 19px; padding-top: 15px; border-top: 1px solid #edf0e8; }
.preview-label { color: #496a5b; font-size: 11px; font-weight: 700; }
.preview-state { color: #9aa89a; font-size: 10px; }
.name-chips { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 11px; }
.name-chip { padding: 6px 10px; border-radius: 999px; background: #f0f5ea; color: #5b7965; font-size: 10px; }
.more-chip { background: #e6f2ed; color: #248479; }
.empty-roster { display: flex; align-items: center; gap: 9px; margin-top: 12px; color: #a1ab9e; font-size: 11px; }
.empty-roster-icon { display: flex; width: 23px; height: 23px; align-items: center; justify-content: center; border: 1px dashed #b8c9b7; border-radius: 50%; color: #7aa38b; font-size: 15px; }
.draw-card { position: relative; overflow: hidden; padding: 25px 25px 20px; background: linear-gradient(155deg, #e6f2df 0%, #e1f0e9 52%, #e8f2dd 100%); }
.draw-card::after { position: absolute; right: -80px; bottom: -170px; width: 360px; height: 360px; border: 1px solid rgba(40,111,96,.12); border-radius: 50%; content: ''; pointer-events: none; }
.draw-card-head { position: relative; z-index: 1; }
.draw-card-title { color: #2c594d; }
.demo-stamp { padding: 6px 10px; border-radius: 999px; background: rgba(255,255,255,.65); color: #688f79; font-size: 10px; }
.draw-modes { position: relative; z-index: 1; display: flex; gap: 5px; margin-top: 17px; padding: 4px; border: 1px solid rgba(199,218,196,.8); border-radius: 11px; background: rgba(255,255,255,.42); }
.mode-option { display: flex; flex: 1; align-items: center; justify-content: center; gap: 6px; padding: 8px 5px; border-radius: 8px; color: #81988a; font-size: 10px; }
.mode-option.active { background: #fffefa; color: #327568; box-shadow: 0 2px 9px rgba(52,99,76,.08); }
.mode-icon { font-size: 12px; }
.draw-illustration { position: relative; display: flex; height: 220px; align-items: center; justify-content: center; margin-top: -4px; }
.draw-illustration :deep(.pet-frame) { z-index: 1; margin-top: 18px; transform: scale(.9); }
.draw-orbit { position: absolute; border: 1px solid rgba(57,132,116,.15); border-radius: 50%; }
.orbit-one { width: 215px; height: 140px; transform: rotate(-14deg); }
.orbit-two { width: 186px; height: 168px; transform: rotate(34deg); }
.sparkle { position: absolute; color: #d7aa4d; }
.sparkle-one { top: 26px; left: 21%; font-size: 19px; }
.sparkle-two { top: 58px; right: 19%; color: #51aaa0; font-size: 14px; }
.result-box { position: relative; z-index: 1; min-height: 83px; padding: 10px 12px; text-align: center; }
.result-name { display: block; color: #748b7b; font-family: 'STSong', 'Songti SC', 'Noto Serif SC', serif; font-size: 26px; font-weight: 700; line-height: 1.45; }
.result-box.ready .result-name { color: #176f68; }
.result-box.active .result-name { color: #cb8054; }
.result-caption { display: block; margin-top: 3px; color: #88a092; font-size: 10px; }
.draw-button { position: relative; z-index: 1; width: 100%; padding: 13px 18px; border-radius: 12px; background: #177f75; box-shadow: 0 5px 0 rgba(20,94,83,.12); color: #fff; font-size: 14px; font-weight: 700; letter-spacing: .4px; }
.draw-button text { margin-left: 8px; color: #f6d778; }
.draw-button[disabled] { background: #c6d9ca; box-shadow: none; color: #809689; }
.reset-round { display: block; margin: 8px auto 0; padding: 5px 8px; background: transparent; color: #397d70; font-size: 10px; }
.round-progress,.local-footnote { position: relative; z-index: 1; margin-top: 10px; color: #81998b; font-size: 10px; text-align: center; }
.local-footnote text { margin-right: 4px; color: #d6aa53; }
.home-footer { gap: 20px; margin-top: 24px; padding-top: 15px; border-top: 1px solid #e4eade; color: #98a697; font-size: 10px; }
.home-footer > view { gap: 9px; }
.home-footer .home-link { padding: 8px 12px; border: 1px solid #e1e9dd; border-radius: 999px; background: rgba(255,255,255,.48); color: #668372; font-size: 11px; line-height: 1.2; transition: background .18s ease, border-color .18s ease, transform .18s ease; }
.home-footer .footer-login-link { border-color: #d4e6d9; background: #eaf4ed; color: #286153; font-weight: 600; }
.home-footer .footer-register-link { border-color: #d5e6d8; background: #f4f8ef; color: #387666; }
.footer-login-link text { margin-left: 4px; color: #d28c55; }
.feedback-overlay { position: fixed; z-index: 200; inset: 0; display: flex; align-items: center; justify-content: center; padding: 18px; background: rgba(29, 52, 43, .38); backdrop-filter: blur(5px); }
.feedback-dialog { width: 100%; max-width: 520px; padding: 27px; border: 1px solid rgba(230,237,224,.9); border-radius: 22px; background: #fffef9; box-shadow: 0 24px 70px rgba(24, 54, 39, .22); }
.feedback-dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }
.feedback-eyebrow { display: block; color: #83a090; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; }
.feedback-dialog-title { display: block; margin-top: 9px; color: #315747; font-size: 21px; font-weight: 700; }
.feedback-close { width: 30px; height: 30px; margin: 0; padding: 0; border: 1px solid #e4ebdf; border-radius: 50%; background: #f8faf4; color: #718779; font-size: 22px; line-height: 1; }
.feedback-intro { display: block; margin: 9px 0 19px; color: #87968a; font-size: 11px; line-height: 1.7; }
.feedback-field { display: block; box-sizing: border-box; width: 100%; height: 44px; margin: 0 0 11px; padding: 12px 13px; border: 1px solid #e2e9dc; border-radius: 10px; background: #fbfcf7; color: #435f50; font-size: 12px; line-height: 1.6; }
.feedback-field :deep(.uni-input-input),.feedback-field :deep(.uni-input-placeholder) { box-sizing: border-box; color: #435f50; font-size: 12px; line-height: 1.6; }
.feedback-field :deep(.uni-input-placeholder) { color: #a0aea1; }
.feedback-content { height: 140px; min-height: 140px; max-height: 140px; }
.feedback-privacy { display: block; color: #9aa79b; font-size: 10px; line-height: 1.6; }
.feedback-error { display: block; margin-top: 9px; color: #b95f4f; font-size: 11px; }
.feedback-actions { display: flex; justify-content: flex-end; gap: 9px; margin-top: 19px; }
.feedback-actions button { margin: 0; padding: 10px 15px; border-radius: 9px; font-size: 11px; line-height: 1.2; }
.feedback-cancel { border: 1px solid #e2e9dd; background: #fff; color: #718476; }
.feedback-submit { border: 1px solid #137d76; background: #177f75; color: #fff; font-weight: 700; }
.feedback-submit text { margin-left: 6px; color: #f3d77d; }
.feedback-actions button[disabled],.feedback-close[disabled] { opacity: .55; }
@media (max-width: 560px) {
  .app-brand { flex-shrink: 0; gap: 6px; font-size: 16px; }
  .brand-stamp { width: 32px; height: 32px; font-size: 16px; }
  .brand-tag { display: none; }
  .home-nav { flex-shrink: 0; gap: 5px; }
  .home-nav .home-link,.home-login,.home-register { padding: 8px 6px; font-size: 9px; white-space: nowrap; }
  .home-nav { gap: 4px; }
}
@media (hover: hover) and (pointer: fine) {
  .feedback-trigger:hover,.nav-about-link:hover,.home-register:hover,.home-login:hover { background: #e8f0e6; transform: translateY(-1px); }
  .nav-about-link:hover,.home-footer .footer-about-link:hover { border-color: #bad6c4; }
  .home-login:hover,.home-footer .footer-login-link:hover { box-shadow: 0 4px 12px rgba(51, 112, 82, .1); }
  .feedback-trigger:hover { border-color: #b9d8c1; }
}
@media (max-width: 850px) {
  .home-v2 { max-width: 620px; padding: 22px 22px calc(28px + env(safe-area-inset-bottom)); }
  .home-header { margin-bottom: 33px; }
  .home-intro { display: block; margin-bottom: 19px; }
  .home-title { font-size: 35px; }
  .privacy-note { display: inline-flex; margin-top: 14px; font-size: 9px; }
  .home-workbench { grid-template-columns: 1fr; gap: 14px; }
  .draw-card { padding: 22px 22px 18px; }
  .draw-illustration { height: 194px; }
  .draw-illustration :deep(.pet-frame) { transform: scale(.82); }
}
@media (max-width: 420px) {
  .home-v2 { padding-left: 17px; padding-right: 17px; }
  .home-header { margin-bottom: 28px; }
  .app-brand { gap: 7px; font-size: 17px; }
  .brand-stamp { width: 32px; height: 32px; font-size: 16px; }
  .brand-tag { font-size: 9px; letter-spacing: 0; }
  .home-nav { gap: 6px; }
  .home-nav .home-link { padding: 8px 7px; font-size: 10px; }
  .home-register { padding: 8px 7px; font-size: 10px; }
  .home-login { padding: 8px 8px; font-size: 10px; }
  .home-title { font-size: 32px; }
  .home-subtitle { font-size: 12px; }
  .roster-card { padding: 20px 17px 17px; }
  .roster-input { height: 150px; min-height: 150px; max-height: 150px; }
  .draw-card { padding: 20px 17px 17px; }
  .tool-button { padding: 8px 10px; }
  .excel-button { margin-left: 0; }
  .home-footer { align-items: flex-start; flex-direction: column; gap: 7px; }
  .home-footer > view { gap: 16px; }
}
@media (max-width: 390px) {
  .brand-tag { display: none; }
  .app-brand { gap: 6px; font-size: 16px; }
  .home-nav { gap: 4px; }
  .home-nav .home-link { padding: 7px 5px; font-size: 9px; }
  .home-register { padding: 7px 5px; font-size: 9px; }
  .home-login { padding: 7px 6px; font-size: 9px; }
  .feedback-dialog { padding: 21px 17px; border-radius: 18px; }
  .feedback-dialog-title { font-size: 19px; }
}
</style>
