<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { roleNames } from '../permissions'
const props = defineProps({ user: Object })
const rows = ref([]),
  total = ref(0),
  page = ref(1),
  search = ref(''),
  loading = ref(false),
  error = ref(''),
  teachers = ref([])
const dialog = ref(false),
  saving = ref(false),
  form = reactive({}),
  fileInput = ref(null),
  preview = ref(null),
  importing = ref(false)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const r = await api(
      `/accounts?page=${page.value}&size=20&search=${encodeURIComponent(search.value)}`,
    )
    rows.value = r.records
    total.value = r.total
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function searchAccounts() {
  page.value = 1
  load()
}
async function edit(row) {
  try {
    teachers.value = await api('/teachers')
    Object.assign(
      form,
      {
        id: null,
        name: '',
        studentNo: '',
        phone: '',
        password: '',
        role: 'STUDENT',
        ownerTeacherId: props.user.role === 'TEACHER' ? props.user.id : null,
        enabled: true,
        version: null,
      },
      row || {},
    )
    dialog.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function save() {
  if (!form.name.trim() || (!(form.studentNo || '').trim() && !(form.phone || '').trim())) {
    ElMessage.warning('请填写姓名及学号或手机号')
    return
  }
  if (!form.id && form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await api(form.id ? `/accounts/${form.id}` : '/accounts', {
      method: form.id ? 'PUT' : 'POST',
      body: form,
    })
    dialog.value = false
    ElMessage.success('账号已保存')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}
async function reset(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为 ${row.name} 设置新密码，现有登录会话将失效。`,
      '重置密码',
      { inputType: 'password', inputValidator: (v) => (v && v.length >= 6) || '密码至少6位' },
    )
    await api(`/accounts/${row.id}/password`, { method: 'POST', body: { password: value } })
    ElMessage.success('密码已重置')
  } catch (e) {
    if (e instanceof Error) ElMessage.error(e.message)
  }
}
async function template() {
  try {
    const blob = await api('/accounts/import/template', { blob: true })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '学生导入模板.xlsx'
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    ElMessage.error(e.message)
  }
}
async function upload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('文件不能超过2MB')
    return
  }
  importing.value = true
  try {
    const body = new FormData()
    body.append('file', file)
    preview.value = await api('/accounts/import/preview', { method: 'POST', body })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    importing.value = false
  }
}
async function confirmImport() {
  importing.value = true
  try {
    const result = await api('/accounts/import/confirm', {
      method: 'POST',
      body: { token: preview.value.token },
    })
    preview.value = null
    ElMessage.success(`已导入 ${result.count} 名学生`)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    importing.value = false
  }
}
onMounted(load)
</script>
<template>
  <main class="page-content">
    <div class="page-heading">
      <div>
        <span class="eyebrow">PEOPLE & CONNECTIONS</span>
        <h1>每个名字，都值得被记住<span class="heading-dot">.</span></h1>
        <p class="muted">
          {{
            user.role === 'ADMIN'
              ? '管理账号、分配身份，为课堂做好准备。'
              : '管理自己创建的学生，让参与课堂变得简单。'
          }}
        </p>
      </div>
      <el-button type="primary" size="large" @click="edit(null)">＋ 创建账号</el-button>
    </div>
    <div class="table-panel">
      <div class="section-toolbar">
        <el-input
          v-model="search"
          placeholder="搜索姓名、学号或手机号"
          clearable
          style="max-width: 300px"
          @keyup.enter="searchAccounts"
          @clear="searchAccounts"
          ><template #append
            ><el-button @click="searchAccounts">搜索</el-button></template
          ></el-input
        >
        <div>
          <el-button @click="template">下载模板</el-button
          ><el-button :loading="importing" @click="fileInput.click()">Excel 导入</el-button
          ><input ref="fileInput" type="file" accept=".xlsx" hidden @change="upload" />
        </div>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table
        v-loading="loading"
        :data="rows"
        empty-text="还没有账号，创建或导入第一位学生吧"
        ><el-table-column prop="name" label="姓名" min-width="130"
          ><template #default="{ row }"
            ><span class="table-name"
              ><i>{{ row.name.slice(0, 1) }}</i
              >{{ row.name }}</span
            ></template
          ></el-table-column
        ><el-table-column prop="studentNo" label="学号" min-width="110" /><el-table-column
          prop="phone"
          label="手机号"
          min-width="130"
        /><el-table-column label="身份" width="110"
          ><template #default="{ row }"
            ><span class="pill neutral">{{ roleNames[row.role] }}</span></template
          ></el-table-column
        ><el-table-column label="状态" width="90"
          ><template #default="{ row }"
            ><span :class="row.enabled ? 'status-on' : 'muted'">{{
              row.enabled ? '● 正常' : '○ 停用'
            }}</span></template
          ></el-table-column
        ><el-table-column label="操作" width="165" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click="edit(row)">编辑</el-button
            ><el-button link @click="reset(row)">重置密码</el-button></template
          ></el-table-column
        ></el-table
      >
      <div class="table-bottom">
        <span class="muted">共 {{ total }} 个账号</span
        ><el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="prev,pager,next"
          @current-change="load"
        />
      </div>
    </div>
    <el-dialog
      v-model="dialog"
      :title="form.id ? '编辑账号' : '创建账号'"
      width="540px"
      :close-on-click-modal="false"
      ><el-form label-position="top"
        ><el-form-item label="姓名" required
          ><el-input v-model="form.name" maxlength="80"
        /></el-form-item>
        <div class="form-columns">
          <el-form-item label="学号"
            ><el-input
              :model-value="form.studentNo || ''"
              @update:model-value="form.studentNo = $event"
              maxlength="64" /></el-form-item
          ><el-form-item label="手机号"
            ><el-input
              :model-value="form.phone || ''"
              @update:model-value="form.phone = $event"
              maxlength="11"
          /></el-form-item>
        </div>
        <p class="field-help">学号与手机号至少填写一个，两者都填时均可登录。</p>
        <el-form-item v-if="!form.id" label="初始密码" required
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="至少6位字符"
            autocomplete="new-password" /></el-form-item
        ><el-form-item v-if="user.role === 'ADMIN'" label="角色"
          ><el-select v-model="form.role"
            ><el-option
              v-for="(label, key) in roleNames"
              :key="key"
              :label="label"
              :value="key" /></el-select></el-form-item
        ><el-form-item
          v-if="user.role === 'ADMIN' && form.role === 'STUDENT'"
          label="归属老师"
          required
          ><el-select v-model="form.ownerTeacherId" filterable
            ><el-option
              v-for="teacher in teachers"
              :key="teacher.id"
              :label="teacher.name"
              :value="teacher.id" /></el-select></el-form-item
        ><el-form-item label="账号状态"
          ><el-switch
            v-model="form.enabled"
            active-text="正常"
            inactive-text="停用" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存账号</el-button></template
      ></el-dialog
    >
    <el-dialog
      :model-value="!!preview"
      title="导入预览 · 确认后写入"
      width="680px"
      @close="preview = null"
      ><template v-if="preview"
        ><el-alert
          :type="preview.errors.length ? 'error' : 'success'"
          :closable="false"
          :title="
            preview.errors.length
              ? `发现 ${preview.errors.length} 行错误，整批不会导入，请修正后重新上传`
              : `${preview.count} 名学生校验通过，请在10分钟内确认`
          " />
        <ul v-if="preview.errors.length" class="error">
          <li v-for="e in preview.errors" :key="e.row">第 {{ e.row }} 行：{{ e.message }}</li>
        </ul>
        <el-table :data="preview.rows" max-height="350"
          ><el-table-column prop="row" label="行号" /><el-table-column
            prop="name"
            label="姓名" /><el-table-column prop="studentNo" label="学号" /><el-table-column
            prop="phone"
            label="手机号" /></el-table></template
      ><template #footer
        ><el-button @click="preview = null">取消</el-button
        ><el-button
          type="primary"
          :disabled="!preview?.token"
          :loading="importing"
          @click="confirmImport"
          >确认导入</el-button
        ></template
      ></el-dialog
    >
  </main>
</template>
