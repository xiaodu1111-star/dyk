<script setup lang="ts">
import { computed, ref } from 'vue'
import dayjs from 'dayjs'

/**
 * 逾期行「改期」日期弹层。
 *
 * 选完日期即 emit reschedule（新的 ISO 时间字符串），由父组件组装全量 payload 调更新接口。
 */

const props = defineProps<{
  /** 当前截止时间（ISO） */
  currentDue: string | null
}>()

const emit = defineEmits<{
  (e: 'reschedule', dueAt: string): void
}>()

/** 弹层开合 */
const open = ref(false)

/** 日期选择值（datetime-local 字符串） */
const value = ref('')

/** 输入框默认值：当前 dueAt 或今日 18:00 */
const defaultValue = computed(() => {
  if (props.currentDue) {
    return dayjs(props.currentDue).format('YYYY-MM-DDTHH:mm')
  }
  return dayjs().hour(18).minute(0).second(0).format('YYYY-MM-DDTHH:mm')
})

function toggle(): void {
  open.value = !open.value
  if (open.value) {
    value.value = defaultValue.value
  }
}

function confirm(): void {
  if (!value.value) {
    return
  }
  emit('reschedule', value.value)
  open.value = false
}
</script>

<template>
  <div class="reschedule">
    <button
      type="button"
      class="reschedule__trigger glass glass--thin"
      aria-label="改期"
      title="改期"
      :aria-expanded="open"
      @click.stop="toggle"
    >
      改期
    </button>

    <div v-if="open" class="reschedule__pop glass glass--thick" role="dialog" aria-label="更改截止时间">
      <label class="reschedule__label" for="reschedule-input">新的截止时间</label>
      <input
        id="reschedule-input"
        v-model="value"
        class="reschedule__input glass glass--thin"
        type="datetime-local"
      />
      <div class="reschedule__actions">
        <button type="button" class="reschedule__btn" @click.stop="open = false">取消</button>
        <button type="button" class="reschedule__btn reschedule__btn--primary" @click.stop="confirm">
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.reschedule {
  position: relative;
  flex: 0 0 auto;
}

.reschedule__trigger {
  height: 28px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--accent);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.reschedule__pop {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 5;
  display: flex;
  width: 232px;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: var(--r-md);
}

.reschedule__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.reschedule__input {
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--text-primary);
  border-radius: var(--r-sm);
}

.reschedule__input:focus-visible {
  outline: none;
  border-color: var(--accent);
}

.reschedule__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reschedule__btn {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
}

.reschedule__btn--primary {
  font-weight: 600;
  color: #fff;
  background: var(--accent);
}
</style>
