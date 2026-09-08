<script setup>
import { computed } from 'vue'
import { scaleFor } from '../lib/domain.mjs'
const props = defineProps({
  kind: { type: String, default: 'cat' },
  level: { type: Number, default: 1 },
  animate: { type: Boolean, default: true },
})
const scale = computed(() => scaleFor(props.level))
</script>
<template>
  <view class="pet-frame"
    ><view class="pet-shadow"></view
    ><view
      :class="['pet-drawing', kind, { breathe: animate }]"
      :style="{ transform: `scale(${scale})` }"
      ><view class="pet-tail"></view><view class="pet-body"></view><view class="pet-ear left"></view
      ><view class="pet-ear right"></view
      ><view class="pet-head"
        ><view class="pet-eye left"></view><view class="pet-eye right"></view
        ><view class="pet-cheek left"></view><view class="pet-cheek right"></view
        ><view class="pet-nose"></view><view class="pet-mouth"></view></view
      ><view class="pet-foot left"></view><view class="pet-foot right"></view
      ><view v-if="kind === 'dragon'" class="pet-leaf"></view></view
  ></view>
</template>
<style scoped>
.pet-frame {
  width: 220px;
  height: 220px;
  position: relative;
  margin: 0 auto;
}
.pet-shadow {
  position: absolute;
  bottom: 15px;
  left: 42px;
  width: 136px;
  height: 16px;
  background: #42513914;
  border-radius: 50%;
}
.pet-drawing {
  width: 220px;
  height: 220px;
  position: absolute;
  transform-origin: 50% 90%;
  --fur: #ebc27b;
  --dark: #a86f37;
}
.pet-drawing.rabbit {
  --fur: #ead4ce;
  --dark: #bb9287;
}
.pet-drawing.dragon {
  --fur: #a1c08b;
  --dark: #648d51;
}
.pet-body {
  position: absolute;
  width: 115px;
  height: 113px;
  border-radius: 49% 49% 38% 38%;
  background: var(--fur);
  left: 52px;
  top: 91px;
  box-shadow: inset -12px -4px 0 #00000008;
}
.pet-body::after {
  content: '';
  position: absolute;
  width: 66px;
  height: 75px;
  border-radius: 50%;
  background: #fffcf14d;
  left: 24px;
  top: 26px;
}
.pet-head {
  position: absolute;
  width: 142px;
  height: 118px;
  left: 38px;
  top: 45px;
  background: var(--fur);
  border-radius: 42% 42% 45% 45%;
  box-shadow: inset -7px -4px 0 #00000006;
}
.pet-ear {
  position: absolute;
  top: 22px;
  width: 51px;
  height: 65px;
  background: var(--fur);
  border-radius: 10px 80% 10px 10px;
}
.pet-ear.left {
  left: 43px;
  transform: rotate(-14deg);
}
.pet-ear.right {
  right: 43px;
  transform: scaleX(-1) rotate(-14deg);
}
.pet-ear::after {
  content: '';
  position: absolute;
  width: 23px;
  height: 29px;
  border-radius: inherit;
  background: #c9927770;
  left: 10px;
  top: 15px;
}
.pet-eye {
  position: absolute;
  top: 55px;
  width: 9px;
  height: 13px;
  border-radius: 50%;
  background: #4a493e;
}
.pet-eye.left {
  left: 37px;
}
.pet-eye.right {
  right: 37px;
}
.pet-eye::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 3px;
  height: 4px;
  background: white;
  border-radius: 50%;
}
.pet-cheek {
  position: absolute;
  width: 23px;
  height: 12px;
  border-radius: 50%;
  background: #e58e7855;
  top: 74px;
}
.pet-cheek.left {
  left: 17px;
}
.pet-cheek.right {
  right: 17px;
}
.pet-nose {
  position: absolute;
  left: 67px;
  top: 70px;
  width: 8px;
  height: 6px;
  border-radius: 50%;
  background: var(--dark);
}
.pet-mouth {
  position: absolute;
  left: 62px;
  top: 76px;
  width: 17px;
  height: 9px;
  border-bottom: 2px solid var(--dark);
  border-radius: 0 0 50% 50%;
}
.pet-foot {
  position: absolute;
  width: 43px;
  height: 25px;
  background: var(--fur);
  top: 180px;
  border-radius: 50%;
  box-shadow: inset 0 -4px 0 #00000007;
}
.pet-foot.left {
  left: 47px;
}
.pet-foot.right {
  right: 47px;
}
.pet-tail {
  position: absolute;
  top: 123px;
  right: 24px;
  width: 51px;
  height: 58px;
  border: 17px solid var(--fur);
  border-left: 0;
  border-bottom: 0;
  border-radius: 0 80% 0 0;
  transform: rotate(24deg);
}
.rabbit .pet-ear {
  height: 94px;
  width: 33px;
  top: -14px;
  border-radius: 70% 70% 30% 30%;
}
.rabbit .pet-ear.left {
  left: 58px;
  transform: rotate(-9deg);
}
.rabbit .pet-ear.right {
  right: 58px;
  transform: rotate(9deg);
}
.rabbit .pet-ear::after {
  height: 62px;
  width: 15px;
  left: 9px;
  top: 12px;
  background: #c99d9859;
}
.rabbit .pet-tail {
  border: 0;
  background: #f3e6e1;
  width: 35px;
  height: 35px;
  top: 157px;
  right: 26px;
  border-radius: 50%;
}
.dragon .pet-ear {
  height: 31px;
  width: 31px;
  top: 32px;
  border-radius: 50%;
  background: #d7bc76;
}
.dragon .pet-tail {
  border-color: #a1c08b;
  transform: rotate(68deg);
  right: 16px;
  top: 155px;
}
.dragon .pet-head {
  border-radius: 45% 45% 38% 38%;
}
.pet-leaf {
  position: absolute;
  background: #719952;
  width: 37px;
  height: 22px;
  left: 100px;
  top: 28px;
  border-radius: 2px 90% 2px 90%;
  transform: rotate(-28deg);
}
.breathe .pet-head {
  animation: pet-breathe 3s ease-in-out infinite;
}
@keyframes pet-breathe {
  50% {
    top: 42px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .breathe .pet-head {
    animation: none;
  }
}
</style>
