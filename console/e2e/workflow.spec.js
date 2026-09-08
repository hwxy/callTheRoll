import { test, expect } from '@playwright/test'
const backend=process.env.E2E_API||'http://127.0.0.1:8000/api/v1'
const consoleUrl='http://localhost:5173', appUrl='http://localhost:5174'
const adminLogin=process.env.E2E_ADMIN_LOGIN,adminPassword=process.env.E2E_ADMIN_PASSWORD
let teacher,student,other,activity,teacherToken
test.skip(process.env.E2E_WRITE_ENABLED!=='true','Explicit opt-in required: E2E_WRITE_ENABLED=true (only use isolated local databases)')
test.skip(!adminLogin||!adminPassword,'E2E_ADMIN_LOGIN and E2E_ADMIN_PASSWORD are required')
test.beforeAll(async({request})=>{
  if(!/^http:\/\/(localhost|127\.0\.0\.1):/.test(backend))throw new Error('E2E fixture writes only allowed on localhost')
  const response=await request.post(`${backend}/auth/login`,{headers:{'X-Client':'console'},data:{login:adminLogin,password:adminPassword}})
  expect(response.ok(),await response.text()).toBeTruthy();const root=await response.json()
  const headers={Authorization:`Bearer ${root.token}`}
  const prefix=`e${Date.now()}`
  async function create(name,role,suffix,ownerTeacherId){const r=await request.post(`${backend}/accounts`,{headers,data:{name,studentNo:prefix+suffix,password:'Study123',role,ownerTeacherId,enabled:true}});expect(r.ok(),await r.text()).toBeTruthy();return r.json()}
  teacher=await create('林老师','TEACHER','teacher');other=await create('陈老师','TEACHER','other');student=await create('林小满','STUDENT','student',teacher.id)
  const r=await request.post(`${backend}/auth/login`,{headers:{'X-Client':'console'},data:{login:teacher.studentNo,password:'Study123'}});teacherToken=(await r.json()).token
  const a=await request.post(`${backend}/activities`,{headers:{Authorization:`Bearer ${teacherToken}`},data:{name:`三年级 · 数学探索 ${prefix.slice(-5)}`,repeatDraw:false,studentIds:[student.id],teacherIds:[other.id]}});expect(a.ok()).toBeTruthy();activity=await a.json()
})
async function consoleLogin(page,user,password='Study123'){
  await page.goto(consoleUrl);await page.getByPlaceholder('输入你的账号').fill(user.studentNo);await page.getByPlaceholder('输入密码',{exact:true}).fill(password);await page.getByRole('button',{name:/进入教学空间/}).click();await expect(page.locator('.workspace')).toBeVisible()
}
async function appLogin(page,user){
  await page.goto(`${appUrl}/#/pages/login/login`);await page.locator('uni-input input').nth(0).fill(user.studentNo);await page.locator('uni-input input').nth(1).fill('Study123');await page.locator('.primary-button').click()
}
test('console login has no role selector and handles errors',async({page})=>{
  await page.goto(consoleUrl);await expect(page.locator('select,[role="combobox"],[role="radio"]')).toHaveCount(0)
  await page.screenshot({path:'../docs/screenshots/console-login.png',fullPage:true})
  await page.getByPlaceholder('输入你的账号').fill(teacher.studentNo);await page.getByPlaceholder('输入密码',{exact:true}).fill('wrong-password');await page.getByRole('button',{name:/进入教学空间/}).click();await expect(page.getByRole('alert')).toContainText('账号或密码错误')
})
test('teacher console shows activity and restricted menus',async({page})=>{
  await consoleLogin(page,teacher);await expect(page.locator('.activity-card').filter({hasText:activity.name})).toBeVisible();await expect(page.locator('.sidebar nav')).not.toContainText('角色管理');await expect(page.locator('.el-loading-mask')).toHaveCount(0)
  await page.screenshot({path:'../docs/screenshots/console-activities.png',fullPage:true})
  await page.getByRole('button',{name:/账号管理/}).click();await expect(page.getByRole('table').getByText('林小满')).toBeVisible()
  await page.getByRole('button',{name:'编辑',exact:true}).first().click();await page.getByRole('button',{name:'保存账号',exact:true}).click();await expect(page.getByText('账号已保存',{exact:true})).toBeVisible()
  await page.screenshot({path:'../docs/screenshots/console-accounts.png',fullPage:true})
})
test('guest demo is standalone and fits mobile viewport',async({page})=>{
  await page.setViewportSize({width:390,height:844});await page.goto(appUrl);await expect(page.getByText('虚构名单 · 不记录积分')).toBeVisible()
  let realRequests=0;page.on('request',r=>{if(r.url().includes('/api/v1/'))realRequests++})
  await page.locator('.primary-button').click();await expect(page.locator('.primary-button')).toContainText('试试随机点名',{timeout:7000});expect(realRequests).toBe(0)
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBeTruthy()
  await page.screenshot({path:'../docs/screenshots/app-demo-mobile.png',fullPage:true})
})
test('teacher award reaches student pet across independent browser sessions',async({browser})=>{
  const ctx=await browser.newContext({viewport:{width:390,height:844}}),page=await ctx.newPage()
  try{
    await appLogin(page,teacher);await page.getByText(activity.name,{exact:true}).click();await expect(page.locator('.draw-stage')).toBeVisible()
    await page.locator('.draw-stage .primary-button').click();await expect(page.locator('.draw-name')).toHaveText('林小满');await page.locator('.award-actions .primary-button').click();await expect(page.getByText('本轮已抽完或暂无有效学生',{exact:true})).toBeVisible()
    await page.screenshot({path:'../docs/screenshots/app-teacher-mobile.png',fullPage:true})
  }finally{await ctx.close()}
  const ctx2=await browser.newContext({viewport:{width:390,height:844}}),studentPage=await ctx2.newPage()
  try{
    await appLogin(studentPage,student);await expect(studentPage.locator('.pet-picker')).toBeVisible();await studentPage.locator('.pet-picker').getByText('云朵兔').click();await studentPage.locator('.pet-stage .primary-button').click();await studentPage.locator('uni-modal .uni-modal__btn_primary').click();await expect(studentPage.locator('.growth-stats')).toBeVisible();await expect(studentPage.locator('.stat-number.gold')).toContainText('1');await expect(studentPage.locator('.pet-name')).toHaveText('云朵兔')
    await studentPage.reload();await expect(studentPage.locator('.pet-name')).toHaveText('云朵兔');await expect(studentPage.locator('.record-row')).toContainText('老师的鼓励')
    expect(await studentPage.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBeTruthy()
    await studentPage.screenshot({path:'../docs/screenshots/app-student-mobile.png',fullPage:true})
  }finally{await ctx2.close()}
})
test('server forbids other teacher account edits and student access to staff endpoints',async({request})=>{
  const r=await request.post(`${backend}/auth/login`,{headers:{'X-Client':'app'},data:{login:student.studentNo,password:'Study123'}});const {token}=await r.json()
  expect((await request.get(`${backend}/accounts`,{headers:{Authorization:`Bearer ${token}`}})).status()).toBe(403)
  const t=await request.post(`${backend}/auth/login`,{headers:{'X-Client':'console'},data:{login:other.studentNo,password:'Study123'}});const second=await t.json()
  expect((await request.post(`${backend}/accounts/${student.id}/password`,{headers:{Authorization:`Bearer ${second.token}`},data:{password:'Attempt123'}})).status()).toBe(403)
})
