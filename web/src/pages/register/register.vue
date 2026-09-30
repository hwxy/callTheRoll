<script setup>
import { ref } from 'vue'
import { api, auth } from '../../lib/api'

const name = ref('')
const phone = ref('')
const password = ref('')
const passwordAgain = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  const cleanPhone = phone.value.trim()
  if (!name.value.trim() || !cleanPhone || !password.value || !passwordAgain.value) {
    error.value = '请填写完整信息'
    return
  }
  if (!/^1[3-9]\d{9}$/.test(cleanPhone)) {
    error.value = '请输入 11 位中国大陆手机号'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少需要 6 位'
    return
  }
  if (password.value !== passwordAgain.value) {
    error.value = '两次输入的密码不一致'
    return
  }

  busy.value = true
  error.value = ''
  try {
    const result = await api('/auth/register/teacher', {
      method: 'POST',
      body: { name: name.value.trim(), phone: cleanPhone, password: password.value },
    })
    auth.save(result.token)
    password.value = ''
    passwordAgain.value = ''
    uni.reLaunch({ url: '/pages/activities/activities' })
  } catch (requestError) {
    error.value = requestError.message || '注册失败，请稍后重试'
  } finally {
    busy.value = false
  }
}

function backHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}
</script>

<template>
  <view class="app-page register-page">
    <view class="register-top">
      <view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text></view>
      <button class="register-back" @click="backHome">← 返回首页</button>
    </view>
    <view class="register-layout">
      <view class="register-story">
        <text class="register-overline">A BETTER CLASSROOM STARTS HERE</text>
        <text class="register-title">让课堂互动，<br />从第一位同学开始。</text>
        <text class="register-copy">创建老师账号，添加学生、组织活动，让每一次参与都被看见。</text>
        <view class="story-note"><text>✦</text><text>本页面仅开放老师注册，学生账号由老师创建。</text></view>
      </view>
      <view class="register-card">
        <text class="card-eyebrow">TEACHER ACCOUNT</text>
        <text class="card-title">账号注册</text>
        <text class="card-subtitle">注册后即可进入课堂管理。</text>

        <text class="field-label">老师姓名</text>
        <input v-model="name" class="register-input" placeholder="请输入姓名" maxlength="80" />
        <text class="field-label">手机号</text>
        <input v-model="phone" class="register-input" type="number" placeholder="手机号将作为登录账号" maxlength="11" />
        <text class="field-label">设置密码</text>
        <input v-model="password" class="register-input" password placeholder="至少 6 位字符" maxlength="72" />
        <text class="field-label">确认密码</text>
        <input v-model="passwordAgain" class="register-input" password placeholder="再次输入密码" maxlength="72" confirm-type="done" @confirm="submit" />
        <text v-if="error" class="register-error">{{ error }}</text>
        <button class="register-submit" :disabled="busy" @click="submit">{{ busy ? '正在创建…' : '注册账号' }} <text v-if="!busy">↗</text></button>
        <view class="login-prompt"><text>已有账号？</text><text class="login-link" @click="goLogin">直接登录</text></view>
        <text class="secure-note">账号角色由服务端固定为老师，不会创建学生或管理员账号。</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.register-page { min-height: 100vh; padding: 28px 44px 48px; color: #28483e; background: radial-gradient(ellipse at 82% 18%, rgba(206,232,205,.55), transparent 32%), radial-gradient(ellipse at 10% 82%, rgba(239,222,182,.3), transparent 28%); }
.register-top,.app-brand { display: flex; align-items: center; }
.register-top { justify-content: space-between; max-width: 1120px; margin: 0 auto; }
.app-brand { gap: 10px; font-size: 19px; font-weight: 700; }
.brand-stamp { display: flex; width: 36px; height: 36px; align-items: center; justify-content: center; border-radius: 12px 12px 12px 4px; background: #137d76; color: white; font-size: 18px; }
.register-back { margin: 0; padding: 9px 14px; border: 1px solid #d8e6d8; border-radius: 999px; background: rgba(255,255,255,.65); color: #5c7c6b; font-size: 11px; }
.register-layout { display: grid; grid-template-columns: minmax(0,1fr) minmax(340px,430px); gap: clamp(38px,9vw,130px); align-items: center; max-width: 990px; min-height: calc(100vh - 112px); margin: 0 auto; }
.register-story { max-width: 490px; padding-bottom: 24px; }
.register-overline,.card-eyebrow { display: block; color: #7d9a8a; font-size: 9px; font-weight: 700; letter-spacing: 1.8px; }
.register-title { display: block; margin-top: 17px; color: #244b41; font-family: 'STSong','Songti SC','Noto Serif SC',serif; font-size: clamp(34px,4.2vw,51px); font-weight: 700; line-height: 1.4; }
.register-copy { display: block; max-width: 385px; margin-top: 13px; color: #748b7e; font-size: 13px; line-height: 1.9; }
.story-note { display: flex; align-items: center; gap: 9px; margin-top: 25px; padding: 11px 14px; border: 1px solid #deeadc; border-radius: 999px; background: rgba(255,255,255,.62); color: #718777; font-size: 10px; }
.story-note text:first-child { color: #d0a54e; }
.register-card { padding: 29px 30px 24px; border: 1px solid #e3eadc; border-radius: 22px; background: rgba(255,255,250,.94); box-shadow: 0 18px 48px rgba(48,83,62,.07); }
.card-title,.card-subtitle { display: block; }
.card-title { margin-top: 10px; color: #315447; font-size: 22px; font-weight: 700; }
.card-subtitle { margin-top: 6px; margin-bottom: 20px; color: #89988c; font-size: 11px; }
.field-label { display: block; margin: 13px 0 7px; color: #587362; font-size: 11px; font-weight: 600; }
.register-input { display: block; box-sizing: border-box; width: 100%; height: 43px; padding: 0 12px; border: 1px solid #e2e9dc; border-radius: 9px; background: #fbfcf7; color: #435f50; font-size: 12px; }
.register-error { display: block; margin-top: 12px; color: #b95f4f; font-size: 11px; line-height: 1.5; }
.register-submit { width: 100%; margin-top: 21px; padding: 12px 16px; border-radius: 10px; background: #177f75; color: white; font-size: 12px; font-weight: 700; }
.register-submit text { margin-left: 7px; color: #f3d77d; }
.login-prompt { display: flex; justify-content: center; gap: 5px; margin-top: 17px; color: #87968a; font-size: 10px; }
.login-link { color: #267b6d; font-weight: 700; }
.secure-note { display: block; margin-top: 17px; padding-top: 13px; border-top: 1px solid #edf0e8; color: #9aa79b; font-size: 9px; line-height: 1.6; text-align: center; }
@media (max-width: 720px) {
  .register-page { padding: 20px 17px calc(30px + env(safe-area-inset-bottom)); }
  .register-layout { display: flex; min-height: auto; flex-direction: column; align-items: stretch; gap: 21px; margin-top: 34px; }
  .register-story { padding-bottom: 0; }
  .register-title { margin-top: 12px; font-size: 32px; }
  .register-copy { max-width: none; font-size: 11px; }
  .story-note { width: fit-content; max-width: 100%; box-sizing: border-box; border-radius: 13px; line-height: 1.6; }
  .register-card { padding: 23px 19px 19px; border-radius: 18px; }
}
</style>
