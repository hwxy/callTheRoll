<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const range = ref(30)
const summary = ref({ records: [], totalPv: 0, totalUv: 0 })
const loading = ref(false)
const error = ref('')
const maxPv = computed(() => Math.max(1, ...summary.value.records.map((row) => Number(row.pv) || 0)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    summary.value = await api(`/admin/analytics/home?days=${range.value}`)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function format(value) {
  return new Intl.NumberFormat('zh-CN').format(Number(value) || 0)
}

onMounted(load)
</script>

<template>
  <main class="page-content analytics-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">HOMEPAGE TRAFFIC</span>
        <h1>PV / UV 统计<span class="heading-dot">.</span></h1>
        <p class="muted">仅统计前台首页访问；同一匿名访客按自然日去重为 UV。</p>
      </div>
      <div class="analytics-actions">
        <el-radio-group v-model="range" size="default" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="load">↻ 刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="analytics-alert" />

    <section v-loading="loading" class="analytics-content">
      <div class="analytics-summary">
        <article class="analytics-card pv-card">
          <span class="analytics-card-label">页面浏览量 · PV</span>
          <strong>{{ format(summary.totalPv) }}</strong>
          <span class="analytics-card-note">所选周期累计页面访问次数</span>
          <span class="analytics-mark">↗</span>
        </article>
        <article class="analytics-card uv-card">
          <span class="analytics-card-label">独立访客 · UV</span>
          <strong>{{ format(summary.totalUv) }}</strong>
          <span class="analytics-card-note">所选周期内去重后的匿名访客人数</span>
          <span class="analytics-mark">◎</span>
        </article>
        <article class="analytics-period">
          <span class="period-label">统计区间</span>
          <strong>{{ summary.from || '—' }}</strong>
          <span class="period-divider">至</span>
          <strong>{{ summary.to || '—' }}</strong>
          <small>时区：北京时间</small>
        </article>
      </div>

      <section class="table-panel analytics-table-panel">
        <header class="analytics-table-head">
          <div><span class="pill">每日访问</span><strong>{{ summary.records.length }} 天有访问记录</strong></div>
          <span class="muted">UV 按天去重，同一访客跨天会再次计入</span>
        </header>
        <el-table :data="summary.records" row-key="visitDate" empty-text="所选周期暂无首页访问记录">
          <el-table-column prop="visitDate" label="日期" width="180" />
          <el-table-column label="PV 页面浏览量" min-width="220" sortable prop="pv">
            <template #default="{ row }">
              <div class="traffic-cell"><strong>{{ format(row.pv) }}</strong><span class="traffic-track"><i :style="{ width: `${Math.max(3, (Number(row.pv) / maxPv) * 100)}%` }"></i></span></div>
            </template>
          </el-table-column>
          <el-table-column label="UV 独立访客" width="180" sortable prop="uv">
            <template #default="{ row }">{{ format(row.uv) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <p class="analytics-privacy">隐私说明：前台只使用随机访客编号，后台仅保存不可逆哈希，不保存原始编号；访客哈希明细最多保留 90 天。</p>
    </section>
  </main>
</template>

<style scoped>
.analytics-actions { display: flex; align-items: center; gap: 12px; }
.analytics-alert { margin-bottom: 18px; }
.analytics-content { min-height: 320px; }
.analytics-summary { display: grid; grid-template-columns: 1fr 1fr minmax(190px, .82fr); gap: 14px; margin-bottom: 19px; }
.analytics-card,.analytics-period { position: relative; display: flex; min-height: 145px; flex-direction: column; justify-content: center; overflow: hidden; padding: 22px 24px; border: 1px solid #e6ece2; border-radius: 16px; background: #fffefb; }
.pv-card { background: linear-gradient(130deg, #fffefb 25%, #f3f7ee); }
.uv-card { background: linear-gradient(130deg, #fffefb 25%, #eff7f4); }
.analytics-card-label,.period-label { color: #7c8d80; font-size: 12px; font-weight: 600; }
.analytics-card strong { margin-top: 9px; color: #315b4a; font-size: 34px; font-variant-numeric: tabular-nums; line-height: 1; }
.uv-card strong { color: #287b70; }
.analytics-card-note { margin-top: 10px; color: #9aa69a; font-size: 11px; }
.analytics-mark { position: absolute; right: 18px; top: 11px; color: rgba(55,129,100,.12); font-size: 56px; line-height: 1; }
.analytics-period { gap: 7px; background: #f8f8f1; }
.analytics-period strong { color: #546e5d; font-size: 13px; font-variant-numeric: tabular-nums; }
.period-divider,.analytics-period small { color: #9aa69a; font-size: 10px; }
.analytics-table-panel { overflow: hidden; }
.analytics-table-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 17px 20px; border-bottom: 1px solid #edf0e9; }
.analytics-table-head > div { display: flex; align-items: center; gap: 12px; }
.analytics-table-head strong { color: #607568; font-size: 13px; font-weight: 600; }
.analytics-table-head .muted { margin: 0; font-size: 11px; }
.traffic-cell { display: flex; align-items: center; gap: 12px; }
.traffic-cell > strong { min-width: 48px; color: #506c5b; font-size: 13px; font-variant-numeric: tabular-nums; }
.traffic-track { display: block; width: min(180px, 45%); height: 6px; overflow: hidden; border-radius: 99px; background: #edf1e9; }
.traffic-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg,#82b58a,#399a89); }
.analytics-privacy { margin: 14px 2px 0; color: #99a59a; font-size: 11px; line-height: 1.7; }
@media (max-width: 940px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .analytics-summary { grid-template-columns: 1fr 1fr; }
  .analytics-period { grid-column: 1 / -1; min-height: auto; flex-direction: row; align-items: center; justify-content: flex-start; }
}
@media (max-width: 620px) {
  .analytics-actions { width: 100%; flex-wrap: wrap; }
  .analytics-summary { grid-template-columns: 1fr; }
  .analytics-period { grid-column: auto; flex-wrap: wrap; }
  .analytics-table-head { align-items: flex-start; flex-direction: column; }
  .analytics-table-head > div { flex-wrap: wrap; }
}
</style>
