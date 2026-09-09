<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { api, auth, requireUser, notify } from '../../lib/api'
const user = ref(null),
  items = ref([]),
  total = ref(0),
  page = ref(1),
  busy = ref(false),
  error = ref('')
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
function about() {
  uni.navigateTo({ url: '/pages/about/about' })
}
onShow(() => load())
onPullDownRefresh(() => load())
</script>
<template>
  <view class="app-page"
    ><view class="page-top"
      ><view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view
      ><view class="top-actions"><button class="text-button" @click="about">关于</button
        ><button class="text-button" @click="logout">退出</button></view></view
    ><view class="hero-copy compact"
      ><text class="overline">YOUR CLASSROOM GARDEN</text
      ><text class="hero-title">你好，{{ user?.name || '同学' }}<text class="gold"> ✦</text></text
      ><text class="subtext">{{
        user?.role === 'TEACHER'
          ? '选择一场活动，开始今天的课堂。'
          : '选择一场活动，看看伙伴又长大了多少。'
      }}</text></view
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
        ><text class="tile-arrow">↗</text></view
      ></view
    ><view v-if="!busy && !error && !items.length" class="empty-box"
      ><text class="empty-symbol">❋</text><text class="section-title">还没有活动</text
      ><text class="hint">{{
        user?.role === 'TEACHER'
          ? '请在管理后台创建活动，或请其他老师共享给你。'
          : '请联系老师，将你的账号加入活动。'
      }}</text></view
    ><button
      v-if="items.length < total"
      class="secondary-button"
      :loading="busy"
      @click="load(true)"
    >
      加载更多</button
    ><text class="footnote">共 {{ total }} 场活动 · 每一场都是新的开始</text></view
  >
</template>
