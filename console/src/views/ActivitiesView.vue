<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
const props = defineProps({ user: Object })
const items = ref([]),
  total = ref(0),
  page = ref(1),
  loading = ref(false),
  error = ref(''),
  tab = ref('all')
const dialog = ref(false),
  saving = ref(false),
  teachers = ref([]),
  students = ref([]),
  initialMembers = ref([])
const form = reactive({
  id: null,
  name: '',
  repeatDraw: false,
  studentIds: [],
  teacherIds: [],
  version: null,
})
function changeTab(key) {
  tab.value = key
  page.value = 1
  load()
}
const historyOpen = ref(false),
  records = ref([]),
  historyName = ref('')
async function load() {
  loading.value = true
  error.value = ''
  try {
    const r = await api(`/activities?page=${page.value}&size=12&scope=${tab.value}`)
    items.value = r.records
    total.value = r.total
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
async function candidates() {
  try {
    const query = new URLSearchParams()
    if (form.id) query.set('activityId', form.id)
    if (form.teacherIds.length) query.set('teacherIds', form.teacherIds.join(','))
    const result = await api(`/activities/candidates?${query}`)
    const merged = new Map(initialMembers.value.map((s) => [s.id, s]))
    result.forEach((s) => merged.set(s.id, s))
    students.value = [...merged.values()]
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function edit(item) {
  try {
    teachers.value = await api('/teachers')
    initialMembers.value = []
    Object.assign(form, {
      id: null,
      creatorId: props.user.id,
      name: '',
      repeatDraw: false,
      studentIds: [],
      teacherIds: [],
      version: null,
    })
    if (item) {
      const r = await api(`/activities/${item.id}`)
      Object.assign(form, r.activity, { studentIds: r.studentIds, teacherIds: r.teacherIds })
      initialMembers.value = r.members
    }
    await candidates()
    dialog.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入活动名称')
    return
  }
  const existing = items.value.find((a) => a.id === form.id)
  if (existing && existing.repeatDraw !== form.repeatDraw) {
    try {
      await ElMessageBox.confirm('修改点名模式将开启新轮次，积分不会清空。', '确认切换模式')
    } catch {
      return
    }
  }
  saving.value = true
  try {
    await api(form.id ? `/activities/${form.id}` : '/activities', {
      method: form.id ? 'PUT' : 'POST',
      body: form,
    })
    dialog.value = false
    ElMessage.success('活动已保存')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}
async function remove(item) {
  try {
    await ElMessageBox.confirm(
      `删除「${item.name}」后所有参与者都将无法访问。历史数据会保留。`,
      '删除活动',
      { type: 'warning' },
    )
    await api(`/activities/${item.id}`, { method: 'DELETE' })
    await load()
  } catch (e) {
    if (e instanceof Error) ElMessage.error(e.message)
  }
}
async function history(item) {
  try {
    records.value = await api(`/activities/${item.id}/draws`)
    historyName.value = item.name
    historyOpen.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}
onMounted(load)
</script>
<template>
  <main class="page-content">
    <div class="page-heading">
      <div>
        <span class="eyebrow">CLASSROOM ACTIVITIES</span>
        <h1>把课堂，变成成长的起点<span class="heading-dot">.</span></h1>
        <p class="muted">创建一场活动，邀请同学，让每一次参与都有回应。</p>
      </div>
      <el-button type="primary" size="large" @click="edit(null)">＋ 创建活动</el-button>
    </div>
    <section class="welcome-banner">
      <div>
        <span class="pill">课堂里的小小仪式感</span>
        <h2>今天，会点亮谁的名字？</h2>
        <p>老师在小程序点名与鼓励，同学收获自己的成长伙伴。</p>
      </div>
      <div class="banner-orbit" aria-hidden="true">
        <span>✦</span><b>+1</b><small>每一点，都算数</small>
      </div>
    </section>
    <div class="section-toolbar">
      <div class="tabs">
        <button
          v-for="t in [
            { key: 'all', label: '全部活动' },
            { key: 'mine', label: '我创建的' },
            { key: 'shared', label: '共享给我的' },
          ]"
          :key="t.key"
          :class="{ active: tab === t.key }"
          @click="changeTab(t.key)"
        >
          {{ t.label }}
        </button>
      </div>
      <span class="muted">共 {{ total }} 场活动</span>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"
      ><el-button link @click="load">重新加载</el-button></el-alert
    >
    <div v-loading="loading" class="activity-grid">
      <article v-for="(item, index) in items" :key="item.id" class="activity-card">
        <div class="card-top">
          <span class="activity-symbol" :class="`tone-${index % 3}`">{{
            ['✳', '✦', '❋'][index % 3]
          }}</span
          ><span class="pill neutral">{{
            item.creatorId === user.id
              ? '我创建的'
              : user.role === 'ADMIN'
                ? '其他活动'
                : '共享活动'
          }}</span>
        </div>
        <h3>{{ item.name }}</h3>
        <p class="muted">
          第 {{ item.roundNo }} 轮 <span class="separator">·</span>
          {{ item.repeatDraw ? '允许重复点名' : '本轮不重复' }}
        </p>
        <div class="card-foot">
          <el-button link type="primary" @click="edit(item)">管理活动 ↗</el-button>
          <div>
            <el-button link @click="history(item)">记录</el-button
            ><el-button link type="danger" @click="remove(item)">删除</el-button>
          </div>
        </div>
      </article>
    </div>
    <div v-if="!loading && !error && !items.length" class="empty-state">
      <span>❋</span>
      <h3>课堂的故事，从这里开始</h3>
      <p>还没有活动。创建第一场活动，邀请同学一起成长。</p>
      <el-button type="primary" @click="edit(null)">创建第一场活动</el-button>
    </div>
    <el-pagination
      v-if="total > 12"
      v-model:current-page="page"
      :page-size="12"
      :total="total"
      layout="prev, pager, next"
      @current-change="load"
    />
    <el-dialog
      v-model="dialog"
      :title="form.id ? '管理活动' : '创建新活动'"
      width="620px"
      :close-on-click-modal="false"
      ><el-form label-position="top"
        ><el-form-item label="活动名称" required
          ><el-input
            v-model="form.name"
            maxlength="100"
            placeholder="例如：三年级 · 数学探索课堂" /></el-form-item
        ><el-form-item label="点名模式"
          ><el-radio-group v-model="form.repeatDraw"
            ><el-radio :value="false">本轮不重复</el-radio
            ><el-radio :value="true">允许重复点名</el-radio></el-radio-group
          ></el-form-item
        ><el-form-item label="共享给老师"
          ><el-select
            v-model="form.teacherIds"
            multiple
            filterable
            placeholder="选择共同管理的老师"
            @change="candidates"
            ><el-option
              v-for="t in teachers.filter((t) => t.id !== (form.creatorId || user.id))"
              :key="t.id"
              :value="t.id"
              :label="t.name" /></el-select
          ><small class="field-help"
            >共享老师拥有完整权限，包括删除活动及继续共享。</small
          ></el-form-item
        ><el-form-item label="参与学生"
          ><el-select
            v-model="form.studentIds"
            multiple
            filterable
            placeholder="选择参与老师名下的学生"
            ><el-option
              v-for="s in students"
              :key="s.id"
              :value="s.id"
              :label="`${s.name} · ${s.studentNo || s.id}`" /></el-select
          ><small class="field-help"
            >已选择 {{ form.studentIds.length }} 人；已有成员在撤销共享后仍会保留。</small
          ></el-form-item
        ></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存活动</el-button></template
      ></el-dialog
    >
    <el-dialog v-model="historyOpen" :title="`${historyName} · 最近100条点名记录`" width="760px"
      ><el-table :data="records"
        ><el-table-column prop="studentName" label="学生" /><el-table-column
          prop="teacherName"
          label="点名老师" /><el-table-column prop="roundNo" label="轮次" /><el-table-column
          label="结果"
          ><template #default="{ row }">{{
            { PENDING: '待确认', AWARDED: '已加1分', SKIPPED: '未加分' }[row.status]
          }}</template></el-table-column
        ><el-table-column prop="createdAt" label="时间" min-width="170" /></el-table
    ></el-dialog>
  </main>
</template>
