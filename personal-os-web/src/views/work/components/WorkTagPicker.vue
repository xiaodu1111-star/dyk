<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { get, post } from '@/api/request'
import type { TaskTagVO } from '@/views/work/types'

/**
 * 标签选择器：多选 chips。
 *
 * 通过 M0 标签引擎 GET /system/tags 拉取可选标签（不传 scope 取全部），
 * 输入新标签名回车调 POST /system/tags（scope=work）创建并选中。
 */

const props = defineProps<{
  /** 已选标签 id */
  modelValue: number[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void
}>()

/** 可选标签池（后端返回，含 id/name/color） */
const allTags = ref<TaskTagVO[]>([])

/** 新建输入 */
const inputName = ref('')

/** 加载中 */
const loading = ref(false)

/** 已选标签对象 */
const selectedTags = computed(() =>
  props.modelValue
    .map((id) => allTags.value.find((t) => t.id === id))
    .filter((t): t is TaskTagVO => !!t)
)

/** 未选标签 */
const availableTags = computed(() =>
  allTags.value.filter((t) => !props.modelValue.includes(t.id))
)

async function loadTags(): Promise<void> {
  loading.value = true
  try {
    allTags.value = await get<TaskTagVO[]>('/system/tags')
  } finally {
    loading.value = false
  }
}

function add(tag: TaskTagVO): void {
  if (!props.modelValue.includes(tag.id)) {
    emit('update:modelValue', [...props.modelValue, tag.id])
  }
}

function remove(tagId: number): void {
  emit(
    'update:modelValue',
    props.modelValue.filter((id) => id !== tagId)
  )
}

async function createAndSelect(): Promise<void> {
  const name = inputName.value.trim()
  if (!name) {
    return
  }
  loading.value = true
  try {
    const created = await post<TaskTagVO>('/system/tags', { name, scope: 'work' })
    // 后端可能返回完整 VO 或仅有 id 的 VO；补全 name/color 兜底
    const tag: TaskTagVO = {
      id: created.id,
      name: created.name ?? name,
      color: created.color ?? '#007aff'
    }
    if (!allTags.value.some((t) => t.id === tag.id)) {
      allTags.value = [...allTags.value, tag]
    }
    add(tag)
    inputName.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTags().catch(() => undefined)
})
</script>

<template>
  <div class="tag-picker">
    <!-- 已选 -->
    <div v-if="selectedTags.length" class="tag-picker__selected">
      <span
        v-for="tag in selectedTags"
        :key="tag.id"
        class="tag-chip"
        :style="{ backgroundColor: tag.color + '22', borderColor: tag.color + '55' }"
      >
        <span class="tag-chip__dot" :style="{ backgroundColor: tag.color }" aria-hidden="true" />
        {{ tag.name }}
        <button
          type="button"
          class="tag-chip__remove"
          :aria-label="`移除标签 ${tag.name}`"
          @click="remove(tag.id)"
        >
          ×
        </button>
      </span>
    </div>

    <!-- 可选 -->
    <div v-if="availableTags.length" class="tag-picker__available">
      <button
        v-for="tag in availableTags"
        :key="tag.id"
        type="button"
        class="tag-pill glass glass--thin"
        @click="add(tag)"
      >
        <span class="tag-pill__dot" :style="{ backgroundColor: tag.color }" aria-hidden="true" />
        {{ tag.name }}
      </button>
    </div>

    <!-- 新建 -->
    <div class="tag-picker__create glass glass--thin">
      <input
        v-model="inputName"
        class="tag-picker__input"
        type="text"
        placeholder="新建标签，回车添加"
        :disabled="loading"
        @keyup.enter.prevent="createAndSelect"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.tag-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-picker__selected,
.tag-picker__available {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 8px 0 12px;
  font-size: 13px;
  color: var(--text-primary);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

.tag-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.tag-chip__remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  font-size: 15px;
  line-height: 1;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
}

.tag-chip__remove:hover {
  color: var(--danger);
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text-secondary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.tag-pill:hover {
  color: var(--text-primary);
}

.tag-pill__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.tag-picker__create {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 14px;
  border-radius: var(--r-pill);
}

.tag-picker__input {
  width: 100%;
  height: 100%;
  font-size: 14px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
}

.tag-picker__input::placeholder {
  color: var(--text-tertiary);
}

.tag-picker__input:focus-visible {
  outline: none;
}
</style>
