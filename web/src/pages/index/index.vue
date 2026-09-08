<script setup>
import { ref } from 'vue'
import { onShow, onHide, onUnload } from '@dcloudio/uni-app'
import Pet from '../../components/Pet.vue'
import { auth } from '../../lib/api'
const names = ['林小满', '陈星野', '许知夏', '周一诺', '沈小禾', '李沐阳'],
  selected = ref('准备好了吗？'),
  rolling = ref(false)
let timer, stopTimer
function stop() {
  clearInterval(timer)
  clearTimeout(stopTimer)
  rolling.value = false
}
function draw() {
  if (rolling.value) return
  rolling.value = true
  timer = setInterval(() => {
    selected.value = names[Math.floor(Math.random() * names.length)]
  }, 85)
  stopTimer = setTimeout(stop, 1400)
}
function login() {
  uni.navigateTo({ url: '/pages/login/login' })
}
function about() {
  uni.navigateTo({ url: '/pages/about/about' })
}
onShow(() => {
  if (auth.token()) uni.reLaunch({ url: '/pages/activities/activities' })
})
onHide(stop)
onUnload(stop)
</script>
<template>
  <view class="app-page home"
    ><view class="app-brand"
      ><text class="brand-stamp">点</text><text>点点名</text
      ><text class="brand-tag">课堂成长伙伴</text></view
    ><view class="hero-copy"
      ><text class="overline">A LITTLE MOMENT. A BIG GROWTH.</text
      ><text class="hero-title">点到名字，<br />也点亮成长。</text
      ><text class="subtext">让每一次参与，都成为长大的力量。</text></view
    ><view class="demo-stage"
      ><view class="stage-caption"
        ><text class="small-badge">体验课堂</text
        ><text class="hint">虚构名单 · 不记录积分</text></view
      ><Pet kind="dragon" :level="4" /><text class="draw-name" :class="{ rolling }">{{
        selected
      }}</text
      ><text class="hint">{{
        rolling ? '下一个闪闪发光的名字是…' : '每位同学，都有自己的高光时刻'
      }}</text
      ><button class="primary-button" :disabled="rolling" @click="draw">
        {{ rolling ? '名字跳动中…' : '试试随机点名' }} <text>✦</text>
      </button></view
    ><view class="home-bottom"
      ><view
        ><text class="section-title">把成长带进你的课堂</text
        ><text class="hint">老师组织活动，同学养成专属伙伴</text></view
      ><view class="home-actions"><button class="text-button  about-button" @click="about">关于我们</button
        ><button class="text-button" @click="login">登录 ↗</button></view></view
    ><text class="footnote">不是比较谁更快，而是看见每一点进步。</text></view
  >
</template>
