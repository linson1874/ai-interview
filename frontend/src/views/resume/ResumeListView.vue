<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteResume, pageResumes } from '@/api/resume'
import type { ResumeListItemVO } from '@/types/api'

// —— 页面卡 D2-2 列表页（03 合同 §3/§4 D2/D4/D7；拍板四：删除确认 + 删空回退）——
const router = useRouter()

const list = ref<ResumeListItemVO[]>([])
const total = ref(0)
const listLoading = ref(false)
// F5：size 上限 50 镜像后端（D2 越界 code=2），选择器不给出 >50 的选项
const query = reactive({ page: 1, size: 10 })

async function load(): Promise<void> {
  listLoading.value = true
  try {
    const pageVO = await pageResumes(query.page, query.size)
    list.value = pageVO.list
    total.value = pageVO.total
  } catch {
    // 失败文案由拦截器统一弹出（三态之"错误态"），页面只复位 loading
  } finally {
    listLoading.value = false
  }
}

async function onDelete(row: ResumeListItemVO): Promise<void> {
  const action = await ElMessageBox.confirm(
    `确定删除简历「${row.realName}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).catch(() => null)
  if (action !== 'confirm') return // 用户取消，静默
  await deleteResume(row.id) // D7：幂等，怎么删都 code=0，没有"删除失败"态
  ElMessage.success('已删除')
  // 拍板四：当前页删空则回退上一页；第一页删空停留显示空态
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await load()
}

onMounted(load)
</script>

<template>
  <section class="roster" aria-labelledby="roster-title">
    <header class="roster__head">
      <div class="roster__head-copy">
        <h1 id="roster-title" class="roster__title">我的简历</h1>
        <p class="roster__count">
          {{ listLoading && total === 0 ? '正在读取…' : `共 ${total} 份` }}
        </p>
      </div>
      <button
        v-if="listLoading || total > 0"
        class="roster__cta"
        type="button"
        @click="router.push('/resumes/new')"
      >
        新建简历
      </button>
    </header>

    <div v-if="!listLoading && total === 0" class="roster__empty">
      <p class="roster__empty-title">这片地还没有展位</p>
      <p class="roster__empty-note">你的简历会作为名录条目排在这里。</p>
      <button class="roster__cta" type="button" @click="router.push('/resumes/new')">
        创建第一份
      </button>
    </div>

    <table v-else class="roster-table">
      <thead>
        <tr>
          <th scope="col">姓名</th>
          <th scope="col">目标岗位</th>
          <th scope="col">技能</th>
          <th scope="col">创建时间</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <template v-if="listLoading && list.length === 0">
          <tr v-for="n in 3" :key="'sk-' + n" class="roster-table__row roster-table__row--skeleton">
            <td><span class="roster-bar roster-bar--w60"></span></td>
            <td><span class="roster-bar roster-bar--w80"></span></td>
            <td><span class="roster-bar roster-bar--w70"></span></td>
            <td><span class="roster-bar roster-bar--w50"></span></td>
            <td><span class="roster-bar roster-bar--w40"></span></td>
          </tr>
        </template>
        <template v-else>
          <tr v-for="row in list" :key="row.id" class="roster-table__row">
            <td>{{ row.realName }}</td>
            <td>{{ row.targetJob }}</td>
            <td>
              <span v-for="skill in row.skills" :key="skill" class="roster-chip">{{ skill }}</span>
            </td>
            <td class="roster-table__time">{{ row.createTime }}</td>
            <td class="roster-table__ops">
              <button
                type="button"
                class="roster-op"
                @click="router.push(`/resumes/${row.id}/edit`)"
              >
                编辑
              </button>
              <button type="button" class="roster-op" @click="onDelete(row)">删除</button>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <nav v-if="total > 0" class="roster-pager" aria-label="分页">
      <span class="roster-pager__count">
        共 {{ total }} 份 · 第 {{ query.page }} / {{ Math.ceil(total / query.size) }} 页
      </span>
      <span v-if="total > query.size" class="roster-pager__ctrl">
        <button
          type="button"
          class="roster-pager__btn"
          :disabled="query.page <= 1 || listLoading"
          @click="() => { query.page -= 1; load() }"
        >
          上一页
        </button>
        <button
          type="button"
          class="roster-pager__btn"
          :disabled="query.page >= Math.ceil(total / query.size) || listLoading"
          @click="() => { query.page += 1; load() }"
        >
          下一页
        </button>
      </span>
    </nav>
  </section>
</template>

<style scoped>
.roster {
  max-width: 1000px;
}

.roster__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 18px;
  border-bottom: 2px solid var(--zoo-ink);
}

.roster__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.roster__count {
  margin: 8px 0 0;
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--zoo-ink-soft);
}

.roster__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 20px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: var(--zoo-sun);
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
}

.roster__cta:hover {
  background: var(--zoo-sun-deep);
}

.roster__cta:active {
  background: var(--zoo-sun-press);
}

.roster__cta:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.roster-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.roster-table th {
  padding: 14px 16px 12px 0;
  border-bottom: 1px solid var(--zoo-ink);
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.14em;
  white-space: nowrap;
  color: var(--zoo-ink-soft);
}

.roster-table td {
  padding: 18px 16px 18px 0;
  border-bottom: 1px solid var(--zoo-paper-line);
  font-size: 14px;
  line-height: 1.5;
  vertical-align: middle;
}

.roster-table tbody tr:hover {
  background: rgba(44, 46, 42, 0.05);
}

.roster-table__time {
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 12px;
  letter-spacing: 0.04em;
  white-space: nowrap;
  color: var(--zoo-ink-soft);
}

.roster-table__ops {
  white-space: nowrap;
}

.roster-chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  margin: 0 6px 6px 0;
  padding: 0 10px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  font-size: 11px;
  line-height: 1;
}

.roster-op {
  padding: 0;
  margin-right: 16px;
  border: 0;
  background: none;
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 13px;
  line-height: 1;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.roster-op:hover {
  text-decoration-thickness: 2px;
}

.roster-op:last-child {
  margin-right: 0;
}

.roster-op:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.roster__empty {
  margin-top: 26px;
  padding: 60px 32px;
  border: 1px dashed var(--zoo-ink);
  text-align: center;
}

.roster__empty-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.roster__empty-note {
  margin: 10px 0 22px;
  font-size: 13px;
  color: var(--zoo-ink-soft);
}

.roster-table__row--skeleton:hover {
  background: none;
}

.roster-table__row--skeleton td {
  padding-top: 26px;
  padding-bottom: 26px;
}

.roster-bar {
  display: block;
  height: 12px;
  background: rgba(44, 46, 42, 0.14);
}

.roster-bar--w40 {
  width: 40%;
}

.roster-bar--w50 {
  width: 50%;
}

.roster-bar--w60 {
  width: 60%;
}

.roster-bar--w70 {
  width: 70%;
}

.roster-bar--w80 {
  width: 80%;
}

.roster-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
}

.roster-pager__count {
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--zoo-ink-soft);
}

.roster-pager__ctrl {
  display: inline-flex;
  gap: 10px;
}

.roster-pager__btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: transparent;
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.roster-pager__btn:hover:not(:disabled) {
  background: rgba(44, 46, 42, 0.1);
}

.roster-pager__btn:disabled {
  border-color: rgba(44, 46, 42, 0.45);
  background: rgba(44, 46, 42, 0.06);
  color: var(--zoo-ink);
  cursor: not-allowed;
}

.roster-pager__btn:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}
</style>
