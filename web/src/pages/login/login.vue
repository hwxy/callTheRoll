<script setup>
import { ref } from 'vue'
import { api, auth } from '../../lib/api'
import { destinationFor } from '../../lib/domain.mjs'
const login = ref(''),
  password = ref(''),
  busy = ref(false),
  error = ref('')
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
    ><view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view
    ><view class="hero-copy"
      ><text class="overline">WELCOME BACK</text
      ><text class="hero-title">你的成长故事，<br />继续发生。</text
      ><text class="subtext">输入账号，回到属于你的课堂。</text></view
    ><view class="white-card login-form"
      ><text class="input-label">学号 / 手机号</text
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
      ><text class="hint center">系统根据账号自动识别老师或学生身份</text></view
    ><text class="footnote"
      >账号由老师或管理员创建<br />忘记密码？请联系你的账号管理员。</text
    ></view
  >
</template>
