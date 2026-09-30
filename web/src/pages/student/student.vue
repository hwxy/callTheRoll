<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onHide, onUnload, onPullDownRefresh } from '@dcloudio/uni-app'
import Pet from '../../components/Pet.vue'
import { api, requireUser, notify, confirm } from '../../lib/api'
import { pets } from '../../lib/domain.mjs'
const id = ref(null),
  growth = ref(null),
  selected = ref('cat'),
  busy = ref(false),
  error = ref('')
let timer,
  inFlight = false,
  visible = false
const currentPet = computed(
  () => pets.find((p) => p.key === growth.value?.pet) || pets.find((p) => p.key === selected.value),
)
async function load() {
  if (!id.value || inFlight) return
  inFlight = true
  try {
    growth.value = await api(`/activities/${id.value}/growth`)
    error.value = ''
  } catch (e) {
    growth.value = null
    error.value = e.message
  } finally {
    inFlight = false
    uni.stopPullDownRefresh()
  }
}
async function choose() {
  if (
    busy.value ||
    !(await confirm(
      '认领成长伙伴',
      `选择${currentPet.value.name}后暂不能更换。它将陪你在这场活动里一起长大。`,
    ))
  )
    return
  busy.value = true
  try {
    growth.value = await api(`/activities/${id.value}/pet`, {
      method: 'POST',
      body: { pet: selected.value },
    })
  } catch (e) {
    notify(e)
  } finally {
    busy.value = false
  }
}
function stop() {
  visible = false
  clearInterval(timer)
}
onLoad((q) => {
  id.value = q.id
})
onShow(async () => {
  visible = true
  try {
    if (!(await requireUser('STUDENT'))) return
    await load()
    clearInterval(timer)
    if (visible) timer = setInterval(load, 10000)
  } catch (e) {
    error.value = e.message
  }
})
onHide(stop)
onUnload(stop)
onPullDownRefresh(load)
</script>
<template>
  <view class="app-page"
    ><view class="hero-copy compact"
      ><text class="overline">GROW AT YOUR OWN PACE</text
      ><text class="page-title">{{ growth?.activity.name || '我的成长伙伴' }}</text
      ><text class="subtext">{{
        growth?.pet ? '你在课堂里努力，它在这里悄悄长大。' : '挑选一位伙伴，一起开启成长的旅程。'
      }}</text></view
    ><view v-if="error" class="error-card"
      ><text>{{ error }}</text
      ><button class="text-button" @click="load">重试</button></view
    ><template v-if="growth"
      ><view class="pet-stage"
        ><view class="stage-caption"
          ><text class="small-badge">{{ growth.pet ? '我的专属伙伴' : '初次见面' }}</text
          ><text class="pet-spark">✦</text></view
        ><Pet :kind="growth.pet || selected" :level="growth.level" /><text class="pet-name">{{
          currentPet.name
        }}</text
        ><text class="hint">{{ currentPet.note }}</text
        ><view v-if="!growth.pet" class="pet-picker"
          ><view
            v-for="p in pets"
            :key="p.key"
            :class="{ selected: selected === p.key }"
            @click="selected = p.key"
            ><text class="pet-dot" :style="{ background: p.color }"></text
            ><text>{{ p.name }}</text></view
          ></view
        ><button
          v-if="!growth.pet"
          class="primary-button"
          :disabled="busy"
          :loading="busy"
          @click="choose"
        >
          就选你，{{ currentPet.name }} ↗</button
        ><view v-else class="growth-stats"
          ><view
            ><text class="stat-number">{{ growth.level }}<text class="stat-unit">级</text></text
            ><text class="hint">伙伴等级</text></view
          ><view class="stats-divider"></view
          ><view
            ><text class="stat-number gold"
              >{{ growth.points }}<text class="stat-unit">分</text></text
            ><text class="hint">课堂的每份努力</text></view
          ></view
        ></view
      ><view class="white-card progress-card"
        ><view class="row-between"
          ><text class="section-title">下一点成长</text
          ><text class="hint">{{ growth.progress }} / 10</text></view
        ><view class="progress-track"
          ><view class="progress-fill" :style="{ width: `${growth.progress * 10}%` }"></view></view
        ><text class="hint"
          >再获得 {{ 10 - growth.progress }} 分，伙伴就能升到 {{ growth.level + 1 }} 级</text
        ></view
      ><view class="white-card"
        ><view class="row-between"
          ><text class="section-title">被看见的每一刻</text
          ><text class="hint">最近的成长记录</text></view
        ><view v-for="r in growth.records" :key="r.id" class="record-row"
          ><view
            ><text>{{ r.teacherName }}{{ r.teacherName.endsWith('老师') ? '' : '老师' }}的鼓励</text
            ><text class="hint">{{ r.createdAt?.replace('T', ' ').slice(0, 16) }}</text></view
          ><text class="score-badge">+1</text></view
        ><view v-if="!growth.records.length" class="small-empty"
          ><text class="hint">第一份鼓励，正在下一次勇敢举手中等你。</text></view
        ></view
      ><text class="footnote">每场活动独立成长 · 下拉可刷新</text></template>
    </view>
</template>
