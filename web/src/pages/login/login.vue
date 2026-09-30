<script setup>
import { ref } from 'vue'
import { api, auth } from '../../lib/api'
import { destinationFor } from '../../lib/domain.mjs'
const login = ref(''),
  password = ref(''),
  busy = ref(false),
  error = ref('')
function register() {
  uni.navigateTo({ url: '/pages/register/register' })
}
function backHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
async function submit() {
  if (!login.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const r = await api('/auth/login', {
      method: 'POST',
      body: { login: login.value.trim(), password: password.value },
    })
    if (r.user.role !== 'TEACHER') throw new Error('目前前台暂时仅支持老师账号登录')
    const url = destinationFor(r.user.role)
    if (!url) throw new Error('系统管理员请使用管理后台')
    auth.save(r.token)
    password.value = ''
    uni.reLaunch({ url })
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>
<template>
  <view class="app-page login-page"
    ><view class="login-top"><view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view><button class="login-back" @click="backHome">← 返回首页</button></view
    ><view class="hero-copy"
      ><text class="overline">TEACHER WORKSPACE</text
      ><text class="hero-title">今天的课堂，<br />从这里开始。</text
      ><text class="subtext">老师登录后创建活动、点名并陪伴学生成长。</text></view
    ><view class="white-card login-form"
      ><text class="input-label">账号</text
      ><input
        v-model="login"
        class="text-input"
        placeholder="输入你的账号"
        maxlength="64"
        confirm-type="next"
      /><text class="input-label">密码</text
      ><input
        v-model="password"
        class="text-input"
        password
        placeholder="输入密码"
        maxlength="72"
        confirm-type="done"
        @confirm="submit"
      /><text v-if="error" class="error-text">{{ error }}</text
      ><button class="primary-button" :loading="busy" :disabled="busy" @click="submit">
        进入课堂 ↗</button
      ><view class="register-prompt"><text>还没有老师账号？</text><button class="register-link" @click="register">账号注册 ↗</button></view></view
    ><text class="footnote"
      >请使用老师账号登录<br />忘记密码？请联系账号管理员。</text>
    </view>
</template>

<style scoped>
.login-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.login-back { flex: 0 0 auto; margin: 0; padding: 9px 14px; border: 1px solid #d8e6d8; border-radius: 999px; background: rgba(255,255,255,.64); color: #5c7c6b; font-size: 11px; line-height: 1.2; }
.login-back::after { border: 0; }
.register-prompt { display: flex; align-items: center; justify-content: center; gap: 5px; margin-top: 18px; color: #87988b; font-size: 11px; }
.register-link { margin: 0; padding: 4px 7px; border-radius: 7px; background: transparent; color: #267b6d; font-size: 11px; font-weight: 700; line-height: 1.4; }
.register-link::after { border: 0; }
</style>
