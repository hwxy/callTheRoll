<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '../../lib/api'

const content = ref('')
const updatedAt = ref('')
const loading = ref(true)
const error = ref('')
const paragraphs = computed(() =>
  (content.value || '关于我们的内容正在准备中。')
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean),
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await api('/site/about')
    content.value = result.content || ''
    updatedAt.value = result.updatedAt || ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function back() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else uni.reLaunch({ url: '/pages/index/index' })
}

onLoad(load)
onPullDownRefresh(load)
</script>

<template>
  <view class="app-page about-page">
    <view class="page-top">
      <view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view>
    </view>
    <view class="about-hero">
      <text class="about-index">01 / ABOUT</text>
      <text class="about-title">让每一次参与，<br />都被温柔看见。</text>
      <view class="about-orbit"><text>点</text><view class="orbit-dot dot-one"></view
        ><view class="orbit-dot dot-two"></view><view class="orbit-dot dot-three"></view></view>
    </view>
    <view class="about-letter">
      <view class="letter-rule"><text>关于我们</text><text>CLASSROOM GARDEN</text></view>
      <text v-if="loading" class="about-copy muted-copy">正在打开这封信…</text>
      <view v-else-if="error" class="error-card">
        <text>{{ error }}</text><button class="text-button" @click="load">重新加载</button>
      </view>
      <view v-else class="about-copy">
        <text v-for="(paragraph, index) in paragraphs" :key="index" class="about-paragraph">{{
          paragraph
        }}</text>
      </view>
      <view class="letter-sign">
        <text>点名星球团队</text><text>和课堂一起成长 ↗</text>
      </view>
    </view>
    <text v-if="updatedAt" class="footnote">更新于 {{ updatedAt.replace('T', ' ') }}</text>
  </view>
</template>
