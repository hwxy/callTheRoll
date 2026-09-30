<script setup>
import { ref } from 'vue'

const activeSection = ref('quick-start')
const classroomScreenshot = (() => {
  // #ifdef MP-WEIXIN
  return 'https://dianmingfront.hwaxy.cn/static/help/teacher-classroom.png'
  // #endif
  // #ifndef MP-WEIXIN
  return '/static/help/teacher-classroom.png'
  // #endif
})()
const sections = [
  { id: 'quick-start', label: '快速开始', group: '使用指南' },
  { id: 'demo', label: '首页体验点名', group: '使用指南' },
  { id: 'teacher', label: '老师课堂操作', group: '使用指南' },
  { id: 'student', label: '成员积分与宠物成长', group: '使用指南' },
]
const groups = ['使用指南']

function jumpTo(id) {
  activeSection.value = id
  uni.pageScrollTo({ selector: `#guide-${id}`, duration: 260 })
}

function back() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else uni.reLaunch({ url: '/pages/index/index' })
}

function login() {
  uni.navigateTo({ url: '/pages/login/login' })
}

function register() {
  uni.navigateTo({ url: '/pages/register/register' })
}

function previewImage(src) {
  uni.previewImage({ urls: [src], current: src })
}
</script>

<template>
  <view class="help-page">
    <view class="help-header">
      <view class="app-brand"><text class="brand-stamp">点</text><text>点名星球</text><text class="brand-context">使用手册</text></view>
      <button class="back-button" @click="back">← 返回</button>
    </view>

    <view class="help-hero">
      <text class="help-eyebrow">HELP CENTER · CLASSROOM GUIDE</text>
      <text class="help-title">让每一次课堂互动都更有趣</text>
      <text class="help-subtitle">从名单录入、随机点名到积分与宠物成长，了解点名星球为老师准备的课堂工具。</text>
      <view class="hero-meta"><text class="meta-dot"></text><text>4 篇指南</text><text class="meta-divider">/</text><text>面向老师的课堂工具</text></view>
    </view>

    <view class="docs-layout">
      <view class="docs-sidebar">
        <text class="sidebar-caption">目录 CONTENTS</text>
        <view v-for="group in groups" :key="group" class="menu-group">
          <text class="menu-group-label">{{ group }}</text>
          <button
            v-for="item in sections.filter((entry) => entry.group === group)"
            :key="item.id"
            class="menu-item"
            :class="{ active: activeSection === item.id }"
            @click="jumpTo(item.id)"
          ><text class="menu-marker">{{ activeSection === item.id ? '✦' : '·' }}</text>{{ item.label }}</button>
        </view>
        <view class="sidebar-callout"><text class="callout-icon">↗</text><view><text class="callout-title">准备开始使用？</text><text class="callout-copy">创建老师账号，进入你的课堂。</text><button class="callout-link" @click="register">账号注册</button></view></view>
      </view>

      <view class="docs-content">
        <view id="guide-quick-start" class="doc-section intro-section">
          <text class="section-kicker">GETTING STARTED</text>
          <text class="section-title">先从一次点名开始</text>
          <text class="section-copy">可以先在首页体验随机点名；注册并登录老师账号后，即可创建自己的课堂活动、管理名单并查看课堂成长记录。</text>
          <view class="entry-cards">
            <view class="entry-card"><text class="entry-number">01</text><text class="entry-title">先体验</text><text class="entry-copy">在首页粘贴名单或导入 Excel，立即试用随机点名。</text></view>
            <view class="entry-card"><text class="entry-number">02</text><text class="entry-title">老师账号</text><text class="entry-copy">注册并登录老师账号，开始创建自己的课堂活动。</text></view>
            <view class="entry-card"><text class="entry-number">03</text><text class="entry-title">课堂互动</text><text class="entry-copy">点名、加分、共享活动，并查看成员的成长情况。</text></view>
          </view>
        </view>

        <view id="guide-demo" class="doc-section">
          <view class="section-heading"><text class="section-index">01</text><view><text class="section-kicker">OPEN CLASSROOM</text><text class="section-title">首页体验点名</text></view></view>
          <text class="section-copy">首页无需注册即可体验。名单不会上传，也不会累计正式课堂积分。</text>
          <view class="steps-list">
            <view class="step-row"><text class="step-num">1</text><view><text class="step-title">准备名单</text><text class="step-copy">在名单框中每行输入一位同学；也可以粘贴多行文本，或导入 .xlsx 文件。</text></view></view>
            <view class="step-row"><text class="step-num">2</text><view><text class="step-title">使用模板</text><text class="step-copy">点击“下载 Excel 模板”，在首个工作表的“姓名”列填写名字后导入。最多支持 200 位同学。</text></view></view>
            <view class="step-row"><text class="step-num">3</text><view><text class="step-title">开始抽取</text><text class="step-copy">选择“可以重复”或“本轮不重复”，再点击开始。示例名单也可一键载入。</text></view></view>
          </view>
        </view>

        <view id="guide-teacher" class="doc-section">
          <view class="section-heading"><text class="section-index">02</text><view><text class="section-kicker">TEACHER WORKSPACE</text><text class="section-title">老师课堂操作</text></view></view>
          <text class="section-copy">老师登录后进入自己的活动列表。创建活动时填写名称并录入学生姓名；之后仍可编辑活动、重新录入或导入 Excel 名单，并按老师账号共享给其他老师。</text>
          <view class="feature-grid">
            <view class="feature-note"><text class="feature-icon">✦</text><view><text class="feature-title">随机点名</text><text class="feature-copy">服务端抽取成员；不重复模式下全部抽完后，可手动重置新一轮。</text></view></view>
            <view class="feature-note"><text class="feature-icon">＋</text><view><text class="feature-title">处理结果</text><text class="feature-copy">每次抽取结果可选择加 1 分或不加分；同一结果只能处理一次。</text></view></view>
            <view class="feature-note"><text class="feature-icon">↻</text><view><text class="feature-title">名单管理与共享</text><text class="feature-copy">修改名单时同名成员会保留成长数据；共享老师账号会校验是否有效，共享老师可共同管理活动。</text></view></view>
            <view class="feature-note"><text class="feature-icon">◎</text><view><text class="feature-title">成员成长</text><text class="feature-copy">老师可查看成员积分、等级和宠物；宠物在活动中随机分配，每 10 分升 1 级。</text></view></view>
          </view>
          <view class="guide-figure phone-figure" @click="previewImage(classroomScreenshot)">
            <image class="guide-image phone-image" :src="classroomScreenshot" mode="aspectFit" lazy-load />
            <text class="figure-caption">老师课堂点名界面 · 点击查看大图</text>
          </view>
        </view>

        <view id="guide-student" class="doc-section last-section">
          <view class="section-heading"><text class="section-index">03</text><view><text class="section-kicker">MEMBER GROWTH</text><text class="section-title">成员积分与宠物成长</text></view></view>
          <text class="section-copy">打开活动后，老师可以在成员列表查看每位同学的积分、等级和宠物。每个活动分别记录成长数据，积分累计 10 分升 1 级，宠物会在猫、兔、龙中随机分配。</text>
          <view class="highlight-strip"><text class="highlight-symbol">↗</text><text>名单调整时，同名成员会保留该活动已有的积分与宠物成长；不同活动之间的成长数据相互独立。</text></view>
        </view>
      </view>
    </view>

    <view class="help-footer"><text>每一次课堂参与，都值得被看见。</text><view><button class="footer-register" @click="register">账号注册</button><button class="footer-login" @click="login">老师账号登录 ↗</button></view></view>
  </view>
</template>

<style scoped>
.help-page { min-height: 100vh; padding: 25px 42px 45px; color: #294b40; background: radial-gradient(ellipse at 92% 8%, rgba(210,234,209,.5), transparent 29%), radial-gradient(ellipse at 3% 53%, rgba(244,226,184,.25), transparent 27%); }
.help-header,.app-brand,.help-footer,.help-footer > view { display: flex; align-items: center; }
.help-header { justify-content: space-between; max-width: 1180px; margin: 0 auto 30px; }
.app-brand { gap: 10px; font-size: 19px; font-weight: 700; letter-spacing: .4px; }
.brand-stamp { display: flex; width: 36px; height: 36px; align-items: center; justify-content: center; border-radius: 12px 12px 12px 4px; background: #137d76; color: #fff; font-size: 18px; }
.brand-context { margin-left: 3px; color: #8ba094; font-size: 11px; font-weight: 400; letter-spacing: 1px; }
.back-button { margin: 0; padding: 9px 15px; border: 1px solid #d8e6d8; border-radius: 999px; background: rgba(255,255,255,.64); color: #5c7c6b; font-size: 11px; }
.help-hero { max-width: 1180px; margin: 0 auto 22px; padding: 25px 30px 23px; border: 1px solid rgba(221,233,215,.8); border-radius: 21px; background: linear-gradient(112deg, rgba(255,255,250,.9), rgba(235,245,229,.82)); }
.help-eyebrow,.section-kicker { display: block; color: #7c9d8d; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; }
.help-title { display: block; margin-top: 9px; color: #254d42; font-family: 'STSong','Songti SC','Noto Serif SC',serif; font-size: 29px; font-weight: 700; letter-spacing: .4px; }
.help-subtitle { display: block; max-width: 650px; margin-top: 7px; color: #789082; font-size: 14px; line-height: 1.8; }
.hero-meta { display: flex; align-items: center; gap: 8px; margin-top: 14px; color: #829588; font-size: 9px; }
.meta-dot { width: 6px; height: 6px; border-radius: 50%; background: #4db29d; }
.meta-divider { color: #c4cdbf; }
.docs-layout { display: grid; grid-template-columns: 218px minmax(0,1fr); gap: 18px; max-width: 1180px; margin: 0 auto; align-items: start; }
.docs-sidebar { position: sticky; top: 16px; padding: 18px 12px 13px; border: 1px solid #e2eadf; border-radius: 17px; background: rgba(255,255,250,.91); box-shadow: 0 10px 26px rgba(48,83,62,.035); }
.sidebar-caption { display: block; padding: 0 9px 12px; color: #91a397; font-size: 9px; font-weight: 700; letter-spacing: 1px; }
.menu-group { margin-top: 8px; }
.menu-group-label { display: block; margin: 0 9px 5px; color: #a3afa2; font-size: 9px; }
.menu-item { display: flex; width: 100%; align-items: center; gap: 8px; margin: 2px 0; padding: 8px 9px; border-radius: 8px; background: transparent; color: #6c8374; font-size: 12px; line-height: 1.45; text-align: left; }
.menu-item.active { background: #eaf4e9; color: #26796d; font-weight: 700; }
.menu-marker { width: 12px; color: #9eafa1; text-align: center; }
.menu-item.active .menu-marker { color: #3fa794; }
.sidebar-callout { display: flex; gap: 9px; margin: 15px 2px 0; padding: 12px 9px; border-radius: 11px; background: #f2f6eb; }
.callout-icon { display: flex; width: 22px; height: 22px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 7px; background: #dcefe2; color: #398b77; font-size: 12px; }
.callout-title,.callout-copy { display: block; }
.callout-title { color: #54725d; font-size: 9px; font-weight: 700; }
.callout-copy { margin-top: 4px; color: #829587; font-size: 9px; line-height: 1.55; }
.callout-link { display: flex; width: 100%; min-height: 36px; align-items: center; justify-content: center; margin: 9px 0 0; padding: 8px 12px; border: 1px solid #d5e5d6; border-radius: 8px; background: #fffef9; color: #287e70; font-size: 10px; font-weight: 700; line-height: 1.3; text-align: center; transition: background .18s ease, border-color .18s ease, transform .18s ease; }
.callout-link:hover { border-color: #b9d8c1; background: #e8f0e6; transform: translateY(-1px); }
.docs-content { min-width: 0; padding: 0 22px; border: 1px solid #e3eadc; border-radius: 18px; background: rgba(255,255,250,.92); box-shadow: 0 12px 32px rgba(48,83,62,.04); }
.doc-section { padding: 24px 2px; border-bottom: 1px solid #edf0e8; scroll-margin-top: 20px; }
.intro-section { padding-top: 27px; }
.section-title { display: block; margin-top: 6px; color: #365d4e; font-size: 19px; font-weight: 700; }
.intro-section .section-title { font-family: 'STSong','Songti SC','Noto Serif SC',serif; font-size: 23px; }
.section-copy { display: block; margin-top: 10px; color: #667f70; font-size: 14px; line-height: 1.9; }
.entry-cards { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 9px; margin-top: 16px; }
.entry-card { min-width: 0; padding: 12px; border: 1px solid #e8eee3; border-radius: 11px; background: #fbfcf7; }
.entry-number { display: block; color: #46a08d; font-size: 8px; font-weight: 700; letter-spacing: 1px; }
.entry-title { display: block; margin-top: 6px; color: #4e705b; font-size: 13px; font-weight: 700; }
.entry-copy { display: block; margin-top: 5px; color: #718879; font-size: 12px; line-height: 1.75; }
.section-heading { display: flex; align-items: center; gap: 12px; }
.section-index { display: flex; width: 34px; height: 34px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 10px; background: #eaf4e8; color: #378d7d; font-size: 10px; font-weight: 700; }
.section-heading .section-title { margin-top: 4px; }
.steps-list { display: grid; gap: 11px; margin-top: 15px; }
.step-row { display: flex; align-items: flex-start; gap: 10px; }
.step-num { display: flex; width: 20px; height: 20px; flex: 0 0 auto; align-items: center; justify-content: center; margin-top: 1px; border-radius: 50%; background: #edf5e8; color: #3d8c7d; font-size: 9px; font-weight: 700; }
.step-title,.step-copy { display: block; }
.step-title { color: #526f5e; font-size: 13px; font-weight: 700; }
.step-copy { margin-top: 4px; color: #718879; font-size: 13px; line-height: 1.8; }
.feature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; margin-top: 15px; }
.feature-note { display: flex; gap: 9px; padding: 11px; border: 1px solid #e8eee3; border-radius: 10px; background: #fbfcf7; }
.feature-icon { display: flex; width: 22px; height: 22px; flex: 0 0 auto; align-items: center; justify-content: center; border-radius: 7px; background: #eaf4e8; color: #378d7d; font-size: 11px; }
.feature-title,.feature-copy { display: block; }
.feature-title { color: #526f5e; font-size: 12px; font-weight: 700; }
.feature-copy { margin-top: 4px; color: #718879; font-size: 12px; line-height: 1.8; }
.highlight-strip { display: flex; align-items: flex-start; gap: 9px; margin-top: 14px; padding: 13px; border-radius: 10px; background: #f3f6e9; color: #687b66; font-size: 13px; line-height: 1.85; }
.highlight-symbol { color: #b79a50; font-size: 12px; }
.admin-section { border-color: #efece1; }
.admin-index { background: #f6f0df; color: #a18442; }
.admin-section .section-kicker { color: #a18e60; }
.admin-section .section-index { background: #f6f0df; color: #a18442; }
.admin-step { background: #f6f0df; color: #a18442; }
.role-table { margin-top: 14px; overflow: hidden; border: 1px solid #e8ebdf; border-radius: 10px; }
.role-table-row { display: grid; grid-template-columns: 110px minmax(0,1fr); gap: 10px; padding: 11px 12px; border-top: 1px solid #edf0e8; color: #687f70; font-size: 13px; line-height: 1.7; }
.role-table-row:first-child { border-top: 0; }
.role-table-head { background: #f7f7ee; color: #71816d; font-weight: 700; }
.admin-highlight { background: #f8f3e5; }
.last-section { border-bottom: 0; }
.guide-figure { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-top: 15px; padding: 11px; overflow: hidden; border: 1px solid #e8eee3; border-radius: 13px; background: #f8faf4; }
.guide-image { display: block; }
.overview-image { width: 100%; height: 570px; }
.phone-image { width: 100%; height: 570px; }
.console-image { width: 100%; height: 430px; }
.figure-caption { color: #758a7b; font-size: 11px; line-height: 1.6; text-align: center; }
.help-footer { justify-content: space-between; gap: 15px; max-width: 1180px; margin: 18px auto 0; padding: 15px 2px 0; border-top: 1px solid #e2e9dd; color: #8a9b8d; font-size: 10px; }
.help-footer > view { gap: 8px; }
.help-footer button { margin: 0; padding: 8px 12px; border-radius: 999px; font-size: 10px; }
.footer-register { border: 1px solid #d5e6d8; background: #f4f8ef; color: #387666; }
.footer-login { border: 1px solid #cfe2d4; background: #e8f3e9; color: #286153; font-weight: 700; }
@media(max-width:760px) {
  .help-page { padding: 18px 15px calc(30px + env(safe-area-inset-bottom)); }
  .help-header { margin-bottom: 18px; }
  .help-hero { padding: 19px 18px; margin-bottom: 14px; }
  .help-title { font-size: 23px; line-height: 1.45; }
  .help-subtitle { font-size: 13px; }
  .docs-layout { display: block; }
  .docs-sidebar { position: static; margin-bottom: 11px; padding: 11px 12px; }
  .sidebar-caption { padding: 0 2px 8px; }
  .menu-group { display: flex; align-items: center; gap: 5px; margin-top: 7px; overflow-x: auto; }
  .menu-group-label { flex: 0 0 auto; margin: 0 5px 0 2px; }
  .menu-item { display: inline-flex; width: auto; flex: 0 0 auto; margin: 0; padding: 7px 9px; white-space: nowrap; }
  .sidebar-callout { display: none; }
  .docs-content { padding: 0 16px; }
  .doc-section { padding: 20px 0; }
  .section-copy { font-size: 13px; }
  .intro-section .section-title { font-size: 21px; }
  .entry-copy { font-size: 12px; }
  .step-copy { font-size: 12px; }
  .feature-copy { font-size: 12px; }
  .entry-cards { grid-template-columns: 1fr; }
  .entry-card { padding: 10px 12px; }
  .feature-grid { grid-template-columns: 1fr; }
  .role-table-row { grid-template-columns: 83px minmax(0,1fr); }
  .overview-image,.phone-image { height: 500px; }
  .console-image { height: 300px; }
  .help-footer { align-items: flex-start; flex-direction: column; }
}
</style>
