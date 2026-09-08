<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onHide, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import { api, requireUser, notify, confirm } from '../../lib/api'
import { requestKey } from '../../lib/domain.mjs'
const id = ref(null),
  data = ref(null),
  records = ref([]),
  busy = ref(false),
  loading = ref(true),
  error = ref(''),
  tab = ref('draw'),
  animationName = ref(''),
  rolling = ref(false)
let timer,
  animationTimer,
  requestId = null,
  refreshInFlight = false,
  visible = false
const pending = computed(() => data.value?.pending)
const selectedName = computed(() =>
  rolling.value
    ? animationName.value
    : pending.value
      ? data.value.members.find((m) => m.id === pending.value.studentId)?.name || '同学'
      : '准备好了吗？',
)
async function load() {
  if (!id.value || refreshInFlight) return
  refreshInFlight = true
  try {
    data.value = await api(`/activities/${id.value}`)
    records.value = await api(`/activities/${id.value}/draws`)
    error.value = ''
  } catch (e) {
    data.value = null
    records.value = []
    error.value = e.message
  } finally {
    loading.value = false
    refreshInFlight = false
    uni.stopPullDownRefresh()
  }
}
function stop() {
  visible = false
  clearInterval(timer)
  clearInterval(animationTimer)
  rolling.value = false
}
async function draw() {
  if (busy.value || pending.value) return
  busy.value = true
  rolling.value = true
  requestId ||= requestKey()
  animationTimer = setInterval(() => {
    const members = data.value?.members || []
    animationName.value = members[Math.floor(Math.random() * members.length)]?.name || '…'
  }, 80)
  try {
    await api(`/activities/${id.value}/draws`, { method: 'POST', body: { requestKey: requestId } })
    requestId = null
    await load()
  } catch (e) {
    notify(e)
    await load()
  } finally {
    clearInterval(animationTimer)
    rolling.value = false
    busy.value = false
  }
}
async function resolve(award) {
  if (busy.value || !pending.value) return
  busy.value = true
  try {
    await api(`/activities/${id.value}/draws/${pending.value.id}/resolve`, {
      method: 'POST',
      body: { award },
    })
    requestId = null
    notify(award ? '已加 1 分，成长已送达' : '已完成本次点名')
    await load()
  } catch (e) {
    notify(e)
    await load()
  } finally {
    busy.value = false
  }
}
async function reset() {
  if (busy.value) return
  if (!(await confirm('开启新一轮', '重置抽取进度，所有学生重新参与，积分不会清空。'))) return
  busy.value = true
  try {
    await api(`/activities/${id.value}/reset`, { method: 'POST' })
    requestId = null
    await load()
  } catch (e) {
    notify(e)
  } finally {
    busy.value = false
  }
}
onLoad((q) => {
  id.value = q.id
})
onShow(async () => {
  visible = true
  try {
    if (!(await requireUser('TEACHER'))) return
    await load()
    clearInterval(timer)
    if (visible)
      timer = setInterval(() => {
        if (!busy.value) load()
      }, 10000)
  } catch (e) {
    error.value = e.message
    loading.value = false
  }
})
onHide(stop)
onUnload(stop)
onPullDownRefresh(load)
</script>
<template>
  <view class="app-page"
    ><view class="hero-copy compact"
      ><text class="overline">A NAME. A CHANCE. A MOMENT.</text
      ><text class="page-title">{{ data?.activity.name || '课堂点名' }}</text
      ><view class="inline-meta"
        ><text class="small-badge">第 {{ data?.activity.roundNo || 1 }} 轮</text
        ><text class="hint"
          >{{ data?.activity.repeatDraw ? '可重复点名' : '本轮不重复' }} · 剩余
          {{ data?.remaining || 0 }} 人</text
        ></view
      ></view
    ><view v-if="error" class="error-card"
      ><text>{{ error }}</text
      ><button class="text-button" @click="load">重新加载</button></view
    ><text v-if="loading" class="hint center">正在进入课堂…</text
    ><view v-if="data"
      ><view class="segmented"
        ><view
          v-for="t in [
            { key: 'draw', label: '随机点名' },
            { key: 'members', label: '活动成员' },
            { key: 'history', label: '点名记录' },
          ]"
          :key="t.key"
          :class="{ active: tab === t.key }"
          @click="tab = t.key"
          >{{ t.label }}</view
        ></view
      ><view v-if="tab === 'draw'" class="draw-stage"
        ><view class="draw-decor">✳</view
        ><text class="hint">{{ pending ? '本次幸运同学' : '下一个闪闪发光的名字' }}</text
        ><text class="draw-name" :class="{ rolling }">{{ selectedName }}</text
        ><text class="subtext">{{
          pending ? '一个小小的鼓励，让参与更有力量。' : '每个人，都值得一次勇敢表达的机会。'
        }}</text
        ><view v-if="pending" class="award-actions"
          ><button class="primary-button" :disabled="busy" @click="resolve(true)">
            表现很棒，加 1 分 ✦</button
          ><button class="secondary-button" :disabled="busy" @click="resolve(false)">
            本次不加分
          </button></view
        ><button
          v-else
          class="primary-button"
          :disabled="busy || !data.remaining"
          :loading="busy"
          @click="draw"
        >
          {{
            busy ? '名字跳动中…' : data.remaining ? '开始点名' : '本轮已抽完或暂无有效学生'
          }}</button
        ><button class="text-button center" :disabled="busy || !!pending" @click="reset">
          ↻ 重置点名进度</button
        ><text v-if="pending" class="hint center"
          >所有共享老师看到同一结果，确认后可继续点名。</text
        ></view
      ><view v-else-if="tab === 'members'" class="white-card"
        ><text class="section-title">活动成员 · {{ data.members.length }} 人</text
        ><view v-for="s in data.members" :key="s.id" class="record-row"
          ><text>{{ s.name }}</text
          ><text class="hint"
            >{{ s.studentNo || '—' }}{{ s.enabled ? '' : ' · 已停用' }}</text
          ></view
        ><text v-if="!data.members.length" class="hint">请在管理后台添加学生。</text></view
      ><view v-else class="white-card"
        ><text class="section-title">最近 100 条记录</text
        ><view v-for="r in records" :key="r.id" class="record-row"
          ><view
            ><text>{{ r.studentName }}</text
            ><text class="hint">{{ r.teacherName }} · 第{{ r.roundNo }}轮</text></view
          ><text :class="r.status === 'AWARDED' ? 'score-badge' : 'hint'">{{
            { PENDING: '待确认', AWARDED: '+1', SKIPPED: '未加分' }[r.status]
          }}</text></view
        ><text v-if="!records.length" class="hint">还没有点名记录，开始第一次点名吧。</text></view
      ></view
    ></view
  >
</template>
