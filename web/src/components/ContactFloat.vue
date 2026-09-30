<script setup>
import { ref } from 'vue'

const expanded = ref(false)
const phone = '15715156223'

function toggle() {
  expanded.value = !expanded.value
}
</script>

<template>
  <!-- #ifdef H5 -->
  <view class="contact-float" :class="{ expanded }">
    <view v-if="expanded" class="contact-card" role="dialog" aria-label="微信联系方式">
      <view class="contact-card-head">
        <view>
          <text class="contact-kicker">LET’S KEEP IN TOUCH</text>
          <text class="contact-title">微信联系</text>
        </view>
        <button class="contact-close" aria-label="收起联系方式" @click="toggle">×</button>
      </view>
      <text class="contact-tip">微信扫码添加好友</text>
      <image class="contact-qr" src="/static/wechat-contact-qr.jpg" mode="widthFix" />
      <view class="contact-phone-row">
        <view><text class="phone-label">联系电话</text><text class="phone-number">{{ phone }}</text></view>
        <a class="phone-call" :href="`tel:${phone}`">拨打</a>
      </view>
    </view>
    <button
      class="contact-trigger"
      :aria-expanded="expanded"
      aria-label="展开微信联系方式"
      @click="toggle"
    >
      <text class="chat-mark"><text></text><text></text></text>
      <text class="trigger-label">微信</text>
    </button>
  </view>
  <!-- #endif -->
</template>

<style scoped>
.contact-float {
  position: fixed;
  z-index: 90;
  right: max(20px, env(safe-area-inset-right));
  bottom: calc(22px + env(safe-area-inset-bottom));
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 12px;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}
.contact-trigger {
  display: flex;
  width: 62px;
  height: 62px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 2px;
  margin: 0;
  padding: 0;
  border: 1px solid rgba(255,255,255,.78);
  border-radius: 21px 21px 21px 8px;
  background: #177f75;
  box-shadow: 0 9px 25px rgba(31, 93, 76, .24), inset 0 1px 0 rgba(255,255,255,.25);
  color: #fffdf7;
  transition: transform .2s ease, box-shadow .2s ease, background .2s ease;
}
.contact-trigger::after,.contact-close::after { border: 0; }
.contact-trigger:hover { transform: translateY(-3px); background: #126e66; box-shadow: 0 13px 28px rgba(31, 93, 76, .28); }
.chat-mark { position: relative; display: flex; width: 25px; height: 18px; align-items: flex-start; }
.chat-mark::before,.chat-mark::after { position: absolute; width: 16px; height: 12px; border-radius: 7px; background: #fffdf7; content: ''; }
.chat-mark::before { top: 0; left: 0; }
.chat-mark::after { right: 0; bottom: 0; border: 2px solid #177f75; background: #d7ede1; }
.chat-mark text { z-index: 1; width: 3px; height: 3px; margin: 4px 0 0 4px; border-radius: 50%; background: #177f75; }
.chat-mark text + text { margin-left: 2px; }
.trigger-label { font-size: 10px; font-weight: 700; letter-spacing: .5px; line-height: 1; }
.contact-card {
  box-sizing: border-box;
  width: min(322px, calc(100vw - 32px));
  max-height: calc(100dvh - 110px - env(safe-area-inset-bottom));
  overflow-y: auto;
  padding: 17px 19px 16px;
  border: 1px solid rgba(226, 235, 219, .95);
  border-radius: 20px 20px 7px 20px;
  background: rgba(255, 254, 248, .98);
  box-shadow: 0 20px 58px rgba(35, 68, 51, .2);
  animation: contact-rise .2s ease-out both;
}
.contact-card-head { display: flex; align-items: center; justify-content: space-between; }
.contact-kicker,.contact-title,.contact-tip,.phone-label,.phone-number { display: block; }
.contact-kicker { color: #85a393; font-size: 8px; font-weight: 700; letter-spacing: 1.6px; }
.contact-title { margin-top: 4px; color: #2f5949; font-family: 'STSong', 'Songti SC', 'Noto Serif SC', serif; font-size: 19px; font-weight: 700; }
.contact-close { width: 30px; height: 30px; margin: 0; padding: 0; border: 1px solid #e0e9db; border-radius: 50%; background: #f5f8f0; color: #698373; font-size: 21px; line-height: 1; }
.contact-tip { margin-top: 8px; color: #84968a; font-size: 11px; text-align: center; }
.contact-qr { display: block; width: min(100%, 254px); height: auto; margin: 5px auto 2px; border-radius: 12px; }
.contact-phone-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 11px 13px; border: 1px solid #e7ecdf; border-radius: 12px; background: #f7f8f0; }
.phone-label { color: #91a093; font-size: 9px; }
.phone-number { margin-top: 3px; color: #345d4d; font-size: 15px; font-weight: 700; letter-spacing: .4px; }
.phone-call { flex: 0 0 auto; padding: 8px 13px; border-radius: 999px; background: #e4f1e8; color: #287663; font-size: 11px; font-weight: 700; text-decoration: none; }
@keyframes contact-rise { from { opacity: 0; transform: translateY(9px) scale(.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
@media (max-width: 480px) {
  .contact-float { right: max(14px, env(safe-area-inset-right)); bottom: calc(16px + env(safe-area-inset-bottom)); }
  .contact-trigger { width: 56px; height: 56px; border-radius: 19px 19px 19px 7px; }
  .contact-card { width: min(306px, calc(100vw - 28px)); padding: 14px; }
  .contact-qr { width: min(100%, 238px); }
}
@media (prefers-reduced-motion: reduce) {
  .contact-trigger { transition: none; }
  .contact-card { animation: none; }
}
</style>
