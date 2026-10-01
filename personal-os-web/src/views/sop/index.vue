<script setup lang="ts">
import { computed, ref } from 'vue'
import SopList from './SopList.vue'
import SopEditor from './SopEditor.vue'
import SopDetail from './SopDetail.vue'
import SopRunner from './SopRunner.vue'
import type { SopEditorSeed } from './SopEditor.vue'

/**
 * SOP 库容器：用组件内状态切换四视图态（列表 / 编辑 / 详情 / 执行）。
 * 不改路由 —— `/sop` 始终指向本组件。
 */
type ViewMode = 'list' | 'editor' | 'detail' | 'runner'

const view = ref<ViewMode>('list')
const activeId = ref<number | null>(null)
const runnerRunId = ref<number | null>(null)
const editorSeed = ref<SopEditorSeed | null>(null)

const sopId = computed(() => activeId.value ?? 0)

function goList(): void {
  view.value = 'list'
  activeId.value = null
  runnerRunId.value = null
  editorSeed.value = null
}

function openCreate(seed: SopEditorSeed | null = null): void {
  activeId.value = null
  editorSeed.value = seed
  view.value = 'editor'
}

function openEdit(id: number, seed: SopEditorSeed | null = null): void {
  activeId.value = id
  editorSeed.value = seed
  view.value = 'editor'
}

function openDetail(id: number): void {
  activeId.value = id
  view.value = 'detail'
}

function openRunner(id: number, runId: number): void {
  activeId.value = id
  runnerRunId.value = runId
  view.value = 'runner'
}

/** 编辑保存成功 → 回到详情（新创建则无 id，回列表） */
function onEditorSaved(id: number | null): void {
  if (id) {
    openDetail(id)
  } else {
    goList()
  }
}

/** 详情里点"编辑" → 用详情数据预填编辑器（不额外请求，避免后端未就绪时报错） */
function onEditFromDetail(seed: SopEditorSeed): void {
  openEdit(seed.id, seed)
}

/** 执行结束 → 回列表，列表会自行刷新使用次数 */
function onRunnerFinished(): void {
  goList()
}
</script>

<template>
  <div class="sop">
    <SopList
      v-if="view === 'list'"
      @create="openCreate()"
      @open="openDetail"
      @edit="openEdit"
    />

    <SopEditor
      v-else-if="view === 'editor'"
      :sop-id="activeId"
      :seed="editorSeed"
      @saved="onEditorSaved"
      @cancel="activeId ? openDetail(activeId) : goList()"
    />

    <SopDetail
      v-else-if="view === 'detail'"
      :sop-id="sopId"
      @back="goList"
      @edit="onEditFromDetail"
      @run="openRunner"
      @removed="goList"
    />

    <SopRunner
      v-else
      :sop-id="sopId"
      :run-id="runnerRunId"
      @back="openDetail(sopId)"
      @finished="onRunnerFinished"
    />
  </div>
</template>

<style lang="scss">
/* 模块级警示 token（避坑黄条的明暗两套变量），见 sop-theme.scss */
@use './sop-theme';

.sop {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  padding-bottom: 32px;
}

@media (max-width: 480px) {
  .sop {
    padding-bottom: 24px;
  }
}
</style>
