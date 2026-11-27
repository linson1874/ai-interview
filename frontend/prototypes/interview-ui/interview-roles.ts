import { Box, Cellphone, ChatDotSquare, Coin, Connection, Cpu, DataAnalysis, DocumentChecked, EditPen, Monitor, Picture, Tickets, TrendCharts, Van, VideoCamera } from '@element-plus/icons-vue'
import type { Component } from 'vue'

type RoleGroup = {
  id: string
  label: string
  options: { title: string; focus: string[]; icon: Component }[]
}

export const interviewRoleGroups: RoleGroup[] = [
  {
    id: 'software',
    label: '计算机与软件',
    options: [
      { title: 'Agent 开发实习生', focus: ['工具调用', 'RAG', 'Agent 评估'], icon: ChatDotSquare },
      { title: 'Java 后端开发实习生', focus: ['Spring Boot', '事务', 'Redis'], icon: Cpu },
      { title: 'Python 后端开发实习生', focus: ['FastAPI', '异步 I/O', 'API 设计'], icon: Connection },
      { title: '前端开发实习生', focus: ['浏览器', '组件设计', '性能优化'], icon: Monitor },
      { title: '软件测试实习生', focus: ['用例设计', '接口测试', '缺陷定位'], icon: DocumentChecked },
      { title: '数据工程实习生', focus: ['SQL', '数据管道', '数据质量'], icon: Coin },
      { title: '机器学习工程实习生', focus: ['特征工程', '模型评估', '模型部署'], icon: DataAnalysis },
      { title: '移动端开发实习生', focus: ['界面状态', '网络请求', '性能'], icon: Cellphone },
      { title: '云平台与 DevOps 实习生', focus: ['容器', 'CI/CD', '故障排查'], icon: Box },
    ],
  },
  {
    id: 'business',
    label: '经管与商业',
    options: [
      { title: '财务分析实习生', focus: ['财务报表', '预算差异', '成本分析'], icon: Tickets },
      { title: '市场运营实习生', focus: ['用户画像', '渠道策略', '活动复盘'], icon: TrendCharts },
      { title: '供应链实习生', focus: ['库存周转', '需求预测', '交付协调'], icon: Van },
    ],
  },
  {
    id: 'design',
    label: '传媒与设计',
    options: [
      { title: '新媒体运营实习生', focus: ['平台内容', '账号增长', '数据复盘'], icon: VideoCamera },
      { title: '视觉设计实习生', focus: ['信息层级', '品牌规范', '作品集'], icon: Picture },
      { title: '内容策划实习生', focus: ['选题', '受众洞察', '内容评估'], icon: EditPen },
    ],
  },
]
