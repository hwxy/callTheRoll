<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const content = ref('')
const savedContent = ref('')
const updatedAt = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const changed = computed(() => content.value !== savedContent.value)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await api('/site/about')
    content.value = result.content || ''
    savedContent.value = content.value
    updatedAt.value = result.updatedAt || ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const result = await api('/site/about', { method: 'PUT', body: { content: content.value } })
    content.value = result.content || ''
    savedContent.value = content.value
    updatedAt.value = result.updatedAt || ''
    ElMessage.success('关于我们已发布')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page-content settings-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">SITE VOICE & STORY</span>
        <h1>网站设置<span class="heading-dot">.</span></h1>
        <p class="muted">在这里维护前台“关于我们”的公开内容，仅系统管理员可以修改。</p>
      </div>
      <el-button type="primary" :loading="saving" :disabled="loading || !changed" @click="save">
        发布内容 ↗
      </el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon class="settings-alert" />
    <section v-loading="loading" class="settings-grid">
      <article class="settings-editor">
        <div class="settings-card-head">
          <div><span class="pill">公开内容</span><h2>关于我们</h2></div>
          <span class="settings-count">{{ content.length }} / 10000</span>
        </div>
        <el-input
          v-model="content"
          type="textarea"
          :rows="18"
          maxlength="10000"
          resize="vertical"
          placeholder="介绍团队、教学理念、联系方式等内容。支持换行，将以纯文本安全展示。"
        />
        <div class="settings-meta">
          <span>{{ changed ? '有尚未发布的修改' : '内容已同步' }}</span>
          <span v-if="updatedAt">最近更新：{{ updatedAt.replace('T', ' ') }}</span>
        </div>
      </article>
      <article class="about-preview">
        <span class="about-preview-mark">关<br />于</span>
        <span class="eyebrow">ABOUT DIANDIANMING</span>
        <h2>让每一次参与，<br />都被温柔看见。</h2>
        <p>{{ content || '关于我们的内容正在准备中。' }}</p>
        <footer>点点名 · CLASSROOM GARDEN</footer>
      </article>
    </section>
  </main>
</template>
