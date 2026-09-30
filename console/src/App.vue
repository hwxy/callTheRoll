<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, auth } from './api'
import { canUseConsole, menusFor, roleNames } from './permissions'
import ActivitiesView from './views/ActivitiesView.vue'
import AccountsView from './views/AccountsView.vue'
import RolesView from './views/RolesView.vue'
import SiteSettingsView from './views/SiteSettingsView.vue'
import FeedbackView from './views/FeedbackView.vue'
import AnalyticsView from './views/AnalyticsView.vue'
const user = ref(null),
  page = ref('activities'),
  busy = ref(false),
  restoring = ref(true)
const login = ref(''),
  password = ref(''),
  error = ref('')
const menus = computed(() => menusFor(user.value))
async function signIn() {
  if (!login.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const result = await api('/auth/login', {
      method: 'POST',
      body: { login: login.value.trim(), password: password.value },
    })
    if (!canUseConsole(result.user)) throw new Error('此账号不能使用管理后台')
    auth.save(result.token)
    user.value = result.user
    password.value = ''
    page.value = 'activities'
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
function expired() {
  user.value = null
  error.value = '登录已失效，请重新登录'
}
async function logout() {
  try {
    await api('/auth/logout', { method: 'POST' })
  } catch (e) {
    ElMessage.warning(e.message)
  } finally {
    auth.clear()
    user.value = null
  }
}
onMounted(async () => {
  window.addEventListener('session-expired', expired)
  try {
    const params = new URLSearchParams(window.location.hash.slice(1))
    const ticket = params.get('ticket')
    if (ticket) {
      window.history.replaceState({}, document.title, window.location.pathname + window.location.search)
      auth.clear()
      const result = await api('/auth/console-ticket/exchange', {
        method: 'POST',
        body: { ticket },
      })
      if (!canUseConsole(result.user)) throw new Error('此账号不能使用管理后台')
      auth.save(result.token)
      user.value = result.user
    } else if (auth.token()) {
      const me = await api('/auth/me')
      if (!canUseConsole(me)) throw new Error('当前账号不能使用管理后台')
      user.value = me
    }
  } catch (e) {
    auth.clear()
    error.value = e.message
  } finally {
    restoring.value = false
  }
})
onUnmounted(() => window.removeEventListener('session-expired', expired))
</script>

<template>
  <div v-if="restoring" class="loading-screen">正在确认登录状态…</div>
  <main v-else-if="!user" class="login-layout">
    <section class="login-story">
      <div class="brand">
        <span class="brand-mark">点</span> 点点名 <span class="brand-en">CLASSROOM GARDEN</span>
      </div>
      <div class="story-copy">
        <span class="eyebrow">让每一次参与，都被看见</span>
        <h1>点到名字，<br />也点亮成长。</h1>
        <p>从课堂上的一次举手，<br />到小小伙伴的一点成长。</p>
      </div>
      <div class="garden-art" aria-hidden="true">
        <span class="sun"></span>
        <div class="sprout"><i></i><i></i><b></b></div>
        <span class="growth-note">每一点进步，都值得鼓励 ↗</span>
      </div>
      <span class="story-foot">ROLL CALL. GROW TOGETHER.</span>
    </section>
    <section class="login-panel">
      <div class="login-card">
        <span class="eyebrow">TEACHING WORKSPACE</span>
        <h2>欢迎回到课堂</h2>
        <p class="muted">登录后，系统将自动识别你的账号身份。</p>
        <form @submit.prevent="signIn">
          <label for="account">账号</label
          ><el-input
            id="account"
            v-model="login"
            size="large"
            placeholder="输入你的账号"
            autocomplete="username"
          />
          <label for="password">密码</label
          ><el-input
            id="password"
            v-model="password"
            size="large"
            type="password"
            show-password
            placeholder="输入密码"
            autocomplete="current-password"
          />
          <p v-if="error" class="error" role="alert">{{ error }}</p>
          <el-button
            class="login-submit"
            native-type="submit"
            type="primary"
            size="large"
            :loading="busy"
            >进入教学空间 <span>↗</span></el-button
          >
        </form>
        <div class="login-help">仅向老师及系统管理员开放<br />忘记密码？请联系账号管理员。</div>
      </div>
      <span class="login-footer">点点名 · 让成长有迹可循</span>
    </section>
  </main>
  <div v-else class="workspace">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">点</span> 点点名</div>
      <span class="sidebar-caption">教学管理空间</span>
      <nav>
        <button
          v-for="item in menus"
          :key="item.key"
          :class="{ active: page === item.key }"
          @click="page = item.key"
        >
          <span>{{ item.icon }}</span
          >{{ item.label }}<b v-if="page === item.key">↗</b>
        </button>
      </nav>
      <div class="sidebar-note">
        <span>一声点名，<br />一份成长。</span><small>让每位同学都被看见。</small>
        <div class="note-leaf">❧</div>
      </div>
      <div class="profile">
        <span class="avatar">{{ user.name.slice(0, 1) }}</span>
        <div>
          <strong>{{ user.name }}</strong
          ><small>{{ roleNames[user.role] }}</small>
        </div>
        <button title="退出登录" aria-label="退出登录" @click="logout">↪</button>
      </div>
    </aside>
    <section class="main-area">
      <header class="topbar">
        <span>工作台 <b>/</b> {{ menus.find((m) => m.key === page)?.label }}</span
        ><span class="topbar-tag">● 今天也是成长的一天</span>
      </header>
      <ActivitiesView v-if="page === 'activities'" :user="user" />
      <AccountsView v-else-if="page === 'accounts'" :user="user" />
      <RolesView v-else-if="page === 'roles' && user.role === 'ADMIN'" />
      <SiteSettingsView v-else-if="page === 'settings' && user.role === 'ADMIN'" />
      <FeedbackView v-else-if="page === 'feedback' && user.role === 'ADMIN'" />
      <AnalyticsView v-else-if="page === 'analytics' && user.role === 'ADMIN'" />
    </section>
  </div>
</template>
