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
  <div class="resume-page">
    <header class="resume-page__bar">
      <h1 class="resume-page__title">我的简历</h1>
      <el-button type="primary" @click="router.push('/resumes/new')">新建简历</el-button>
    </header>

    <!-- 三态：空态（el-empty）/ 加载态（v-loading）/ 错误态（拦截器统一 toast） -->
    <div v-loading="listLoading" class="resume-page__body">
      <el-empty v-if="!listLoading && total === 0" description="还没有简历，先创建一份">
        <el-button type="primary" @click="router.push('/resumes/new')">创建第一份简历</el-button>
      </el-empty>
      <template v-else>
        <el-table :data="list">
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="targetJob" label="目标岗位" min-width="160" />
          <el-table-column label="技能" min-width="220">
            <template #default="{ row }">
              <!-- F3 渲染纪律：技能标签全部文本插值，禁 v-html -->
              <el-tag
                v-for="skill in row.skills"
                :key="skill"
                class="resume-page__skill"
              >
                {{ skill }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/resumes/${row.id}/edit`)">
                编辑
              </el-button>
              <el-button link type="danger" @click="onDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          class="resume-page__pager"
          @change="load"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官简历页 UI 稿到位后整体接管（完成环第 5 步） */
.resume-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}
.resume-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.resume-page__title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
}
.resume-page__skill {
  margin-right: 6px;
}
.resume-page__pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
