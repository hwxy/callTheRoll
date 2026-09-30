<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const error = ref('')
const dialog = ref(false)
const detailLoading = ref(false)
const selected = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await api(`/admin/feedback?page=${page.value}&size=20`)
    rows.value = result.records
    total.value = result.total
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function view(row) {
  dialog.value = true
  selected.value = null
  detailLoading.value = true
  try {
    selected.value = await api(`/admin/feedback/${row.id}`)
  } catch (e) {
    dialog.value = false
    ElMessage.error(e.message)
  } finally {
    detailLoading.value = false
  }
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(load)
</script>

<template>
  <main class="page-content feedback-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">USER VOICE & SUPPORT</span>
        <h1>使用问题<span class="heading-dot">.</span></h1>
        <p class="muted">查看首页用户提交的问题与建议。联系信息仅对系统管理员开放。</p>
      </div>
      <el-button @click="load">↻ 刷新列表</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="feedback-alert">
      <el-button link @click="load">重新加载</el-button>
    </el-alert>

    <section v-loading="loading" class="table-panel feedback-table-panel">
      <div class="feedback-table-head">
        <div><span class="pill">提交记录</span><strong>共 {{ total }} 条</strong></div>
        <span class="muted">按提交时间由近到远排列</span>
      </div>
      <el-table :data="rows" empty-text="还没有收到使用问题" row-key="id">
        <el-table-column prop="title" label="问题标题" min-width="260" show-overflow-tooltip />
        <el-table-column label="联系方式" min-width="180">
          <template #default="{ row }">{{ row.contact || '未提供' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="view(row)">查看内容 ↗</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 20"
        v-model:current-page="page"
        :page-size="20"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load"
      />
    </section>

    <el-dialog v-model="dialog" title="问题详情" width="min(640px, 92vw)">
      <div v-loading="detailLoading" class="feedback-detail">
        <template v-if="selected">
          <div class="detail-title-row">
            <div><span class="eyebrow">FEEDBACK #{{ selected.id }}</span><h2>{{ selected.title }}</h2></div>
            <span class="detail-date">{{ formatTime(selected.createdAt) }}</span>
          </div>
          <div class="detail-content">{{ selected.content }}</div>
          <div class="detail-contact"><span>联系方式</span><strong>{{ selected.contact || '用户未提供' }}</strong></div>
        </template>
      </div>
    </el-dialog>
  </main>
</template>

<style scoped>
.feedback-alert { margin-bottom: 18px; }
.feedback-table-panel { overflow: hidden; }
.feedback-table-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 17px 20px; border-bottom: 1px solid #edf0e9; }
.feedback-table-head > div { display: flex; align-items: center; gap: 12px; }
.feedback-table-head strong { color: #607568; font-size: 13px; font-weight: 600; }
.feedback-table-head .muted { margin: 0; font-size: 12px; }
.feedback-detail { min-height: 150px; }
.detail-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.detail-title-row h2 { margin: 8px 0 0; color: #304d3d; font-size: 20px; line-height: 1.5; }
.detail-date { color: #91a095; font-size: 12px; white-space: nowrap; }
.detail-content { margin-top: 22px; padding: 18px; border: 1px solid #e9eee5; border-radius: 12px; background: #fafbf7; color: #53675a; font-size: 14px; line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.detail-contact { display: flex; justify-content: space-between; gap: 12px; margin-top: 16px; padding: 0 3px; color: #89968a; font-size: 12px; }
.detail-contact strong { color: #526b5b; font-weight: 600; }
@media (max-width: 640px) {
  .feedback-table-head { align-items: flex-start; flex-direction: column; }
  .detail-title-row { flex-direction: column; gap: 8px; }
}
</style>
