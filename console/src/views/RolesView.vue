<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
const roles = ref([]),
  error = ref('')
onMounted(async () => {
  try {
    roles.value = await api('/roles')
  } catch (e) {
    error.value = e.message
  }
})
</script>
<template>
  <main class="page-content">
    <div class="page-heading">
      <div>
        <span class="eyebrow">ROLES & BOUNDARIES</span>
        <h1>各有角色，一起成长<span class="heading-dot">.</span></h1>
        <p class="muted">固定三种角色，在账号管理中分配。实际权限始终由服务端校验。</p>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" />
    <div class="activity-grid">
      <article v-for="(role, index) in roles" :key="role.role" class="activity-card">
        <span class="activity-symbol" :class="`tone-${index}`">{{ ['◇', '✳', '❋'][index] }}</span>
        <h3>{{ role.name }}</h3>
        <span class="eyebrow">{{ role.role }}</span>
        <p class="muted">{{ role.permission }}</p>
        <div class="card-foot">
          {{
            role.role === 'STUDENT'
              ? '仅小程序 / H5'
              : role.role === 'ADMIN'
                ? '仅管理后台'
                : '管理后台 / 小程序 / H5'
          }}
        </div>
      </article>
    </div>
  </main>
</template>
