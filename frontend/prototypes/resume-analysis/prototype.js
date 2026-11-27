const findings = {
  fit: {
    title: '基础技术栈对得上目标岗位',
    detail: '项目已经覆盖 Web 接口和数据库操作。补一个技术取舍的例子，比如商品检索为什么这样建索引，更能体现后端能力。',
    reference: '示例岗位要求包含 Java、Web 接口开发和关系数据库。判断依据是原句里出现的 Spring Boot、MySQL 和具体业务模块。',
  },
  contribution: {
    title: '把「团队做了什么」写成「你负责了什么」',
    detail: '补充你负责的工作，例如缓存键设计、失效策略或压测。只保留自己确实做过并能解释的部分。',
    reference: '简历原句把执行主体写成「团队」，能看出项目用了 Redis，个人承担的具体任务仍需补充。',
  },
  evidence: {
    title: '说明 40% 是怎么测出来的',
    detail: '补上优化前后用的是哪种延迟指标，以及测试的并发量和数据量。面试时准备好解释缓存贡献了多少。',
    reference: '知识库引用示例《接口性能优化的验证口径》：比较前后延迟时，应记录指标口径、流量、数据量及其他测试条件。',
  },
  clarity: {
    title: '用一处项目经历说明你如何使用技术',
    detail: '从最熟悉的技术挑一两项，补上使用场景。比如说明 MySQL 负责订单事务，Redis 负责缓存哪些读请求。',
    reference: '原句列出了四项技术。确认熟练程度需要相关的项目动作、使用场景或具体结果。',
  },
}

const roles = {
  software: ['Java 后端开发实习生', '前端开发实习生', '软件测试实习生'],
  business: ['财务分析实习生', '市场运营实习生', '供应链实习生'],
  media: ['新媒体运营实习生', '视觉设计实习生', '内容策划实习生'],
}

const roundTypes = ['项目深挖', '岗位能力', '情境问答', 'HR 面']
const primaryRole = roles.software[0]
const byId = (id) => document.getElementById(id)
const views = [...document.querySelectorAll('[data-view]')]
const ratings = [...document.querySelectorAll('[data-rating]')]
const sources = [...document.querySelectorAll('[data-source]')]
const navLinks = [...document.querySelectorAll('.main-nav a')]
const setup = byId('interview-setup')
const domain = byId('domain')
const role = byId('role')
const customRole = byId('custom-role')
const customFocus = byId('custom-focus')
const roundCount = byId('round-count')
const importDialog = byId('import-dialog')
const answer = byId('sample-answer')
const roundSelections = []

let activeFinding = 'evidence'
let session = null

function showFinding(key, animate = true) {
  activeFinding = key
  const { title, detail, reference } = findings[key]
  for (const button of ratings) {
    button.setAttribute('aria-pressed', String(button.dataset.rating === key))
  }
  for (const button of sources) {
    const isSelected = button.dataset.source === key
    button.setAttribute('aria-pressed', String(isSelected))
    button.classList.toggle('flash', isSelected && animate)
  }
  byId('finding-title').textContent = title
  byId('finding-body').textContent = detail
  byId('reference-body').textContent = reference
}

for (const rating of ratings) {
  rating.addEventListener('click', () => showFinding(rating.dataset.rating))
}
for (const source of sources) {
  source.addEventListener('click', () => {
    showFinding(source.dataset.source)
    const finding = byId('finding-title')
    if (finding.getBoundingClientRect().bottom > window.innerHeight) {
      finding.scrollIntoView({ block: 'nearest' })
    }
  })
}
byId('locate-source').addEventListener('click', () => {
  const selected = sources.find((source) => source.dataset.source === activeFinding)
  selected.focus({ preventScroll: true })
  selected.scrollIntoView({ block: 'nearest' })
})

function setDomain(selectedRole) {
  const custom = domain.value === 'custom'
  byId('role-field').hidden = custom
  byId('custom-role-field').hidden = !custom
  byId('custom-focus-field').hidden = !custom
  role.disabled = custom
  customRole.disabled = !custom
  customRole.required = custom
  customFocus.disabled = !custom
  if (custom) return

  role.replaceChildren(...roles[domain.value].map((name) => new Option(name)))
  if (roles[domain.value].includes(selectedRole)) role.value = selectedRole
}

function getBasis() {
  return setup.elements.basis.value
}

function renderRounds() {
  const defaults = getBasis() === 'resume'
    ? ['项目深挖', '岗位能力', 'HR 面']
    : ['岗位能力', '情境问答', 'HR 面']

  const fields = Array.from({ length: Number(roundCount.value) }, (_, index) => {
    const label = document.createElement('label')
    label.className = 'field'
    label.textContent = `第 ${index + 1} 轮`

    const select = document.createElement('select')
    select.name = 'round'
    select.replaceChildren(...roundTypes.map((name) => new Option(name)))
    select.value = roundSelections[index] ?? defaults[index]
    select.addEventListener('change', () => {
      roundSelections[index] = select.value
    })
    label.append(select)
    return label
  })
  byId('round-fields').replaceChildren(...fields)
}

function openingQuestion({ basis, domain, targetRole, focus, rounds }) {
  if (rounds[0] === 'HR 面') {
    return `你好，欢迎参加${targetRole}的模拟面试。请先做个简短的自我介绍，说说你为什么对这个岗位感兴趣。`
  }
  if (basis === 'resume') {
    if (rounds[0] === '项目深挖') {
      return '你在二手交易项目里提到接口响应时间降低了约 40%。请介绍你负责的部分，以及你们是怎么测出这个结果的。'
    }
    return `你简历里的二手交易项目有哪些经验能用在${targetRole}这个岗位？请选一项说说你做了什么。`
  }
  if (focus) return `今天我们先聊你选的方向「${focus}」。如果要从一项具体任务入手，你会怎么做？`
  if (rounds[0] === '情境问答') {
    return `作为${targetRole}，如果同事给了你一项需求不明确的新任务，你会怎么确认目标并推进？`
  }
  if (domain === 'software' && targetRole === primaryRole) {
    return '如果一个原本很快的列表接口突然变慢，你会用哪些信息判断问题出在服务、缓存还是数据库？'
  }
  return `针对${targetRole}这个岗位，请介绍一项你认为重要的能力，并用一次课程、社团或个人练习说明你如何应用它。`
}

function readConfiguration() {
  const data = new FormData(setup)
  return {
    basis: getBasis(),
    domain: domain.value,
    targetRole: domain.value === 'custom' ? customRole.value.trim() : role.value,
    focus: (data.get('customFocus') ?? '').trim(),
    difficulty: data.get('difficulty'),
    format: data.get('format'),
    rounds: data.getAll('round'),
  }
}

function updateSetupPreview() {
  const config = readConfiguration()
  const hasResume = config.basis === 'resume'
  byId('basis-help').textContent = hasResume
    ? '已带入陈嘉宁的示例简历。题目会结合其中的经历和你选的岗位。'
    : '可以使用课程、社团或个人经历来回答岗位问题。'
  byId('setup-aside-body').textContent = hasResume
    ? '先确认你在项目里做过什么，再结合目标岗位深入追问。'
    : '从岗位常见任务出发，了解你如何思考和处理问题。'
  byId('setup-source').hidden = !hasResume
  byId('setup-example').textContent = openingQuestion({
    ...config,
    targetRole: config.targetRole || '自定义方向',
  })
}

function beginSetup(basis) {
  setup.querySelector(`[name="basis"][value="${basis}"]`).checked = true
  if (basis === 'resume') {
    domain.value = 'software'
    setDomain(primaryRole)
  }
  roundSelections.length = 0
  renderRounds()
  updateSetupPreview()
  location.hash = 'setup'
}

byId('practice-resume').addEventListener('click', () => beginSetup('resume'))
byId('practice-general').addEventListener('click', () => beginSetup('general'))
setup.addEventListener('change', (event) => {
  const field = event.target
  if (field === domain) setDomain()
  if (field === roundCount || field.name === 'basis') renderRounds()
  updateSetupPreview()
})
customRole.addEventListener('input', () => {
  customRole.setCustomValidity('')
  updateSetupPreview()
})
customFocus.addEventListener('input', updateSetupPreview)

setup.addEventListener('submit', (event) => {
  event.preventDefault()
  const config = readConfiguration()
  if (!config.targetRole) {
    customRole.setCustomValidity('请填写一个目标岗位。')
    customRole.reportValidity()
    return
  }

  const question = openingQuestion(config)
  const basisLabel = config.basis === 'resume' ? '示例简历与岗位' : '按岗位练习'
  const formatLabel = config.format === '语音' ? '语音预览' : '文字'
  const rounds = config.rounds.map((name) => {
    const item = document.createElement('li')
    item.textContent = name
    return item
  })
  session = config
  byId('ready-role').textContent = config.targetRole
  byId('ready-basis').textContent = basisLabel
  byId('ready-difficulty').textContent = config.difficulty
  byId('ready-format').textContent = formatLabel
  byId('ready-rounds').replaceChildren(...rounds)
  byId('ready-format-note').hidden = config.format !== '语音'
  byId('interview-subtitle').textContent = `${config.targetRole} · 第 1 轮 · ${config.rounds[0]}`
  byId('interview-question').textContent = question
  answer.value = ''
  answer.setCustomValidity('')
  byId('answer-receipt').hidden = true
  location.hash = 'ready'
})

answer.addEventListener('input', () => answer.setCustomValidity(''))
byId('answer-form').addEventListener('submit', (event) => {
  event.preventDefault()
  if (!answer.value.trim()) {
    answer.setCustomValidity('请先输入回答。')
    answer.reportValidity()
    return
  }
  byId('answer-receipt').hidden = false
})

for (const button of document.querySelectorAll('[data-open-import]')) {
  button.addEventListener('click', () => importDialog.showModal())
}
byId('resume-file').addEventListener('change', (event) => {
  const file = event.target.files[0]
  const feedback = byId('file-feedback')
  if (!file) {
    feedback.textContent = ''
  } else if (!file.name.toLowerCase().endsWith('.pdf')) {
    event.target.value = ''
    feedback.textContent = '请选择 PDF 文件。'
  } else {
    feedback.textContent = `已选择 ${file.name}。文件保留在本机。`
  }
})
importDialog.addEventListener('close', () => {
  byId('resume-file').value = ''
  byId('file-feedback').textContent = ''
})

function showView(moveFocus = false) {
  let viewId = location.hash.slice(1)
  if (!views.some((view) => view.id === viewId)) viewId = 'resume'
  if (!session && (viewId === 'ready' || viewId === 'interview')) {
    viewId = 'setup'
    history.replaceState(null, '', '#setup')
  }
  for (const view of views) view.hidden = view.id !== viewId
  const navId = viewId === 'resume' ? '#resume' : '#setup'
  for (const link of navLinks) {
    if (link.hash === navId) link.setAttribute('aria-current', 'page')
    else link.removeAttribute('aria-current')
  }
  document.title = `${byId(`${viewId}-title`).textContent} · AI 面试助手`
  if (moveFocus) {
    window.scrollTo(0, 0)
    byId(`${viewId}-title`).focus({ preventScroll: true })
  }
}

document.querySelector('.skip-link').addEventListener('click', (event) => {
  event.preventDefault()
  byId('main').focus()
})
window.addEventListener('hashchange', () => showView(true))
showFinding(activeFinding, false)
setDomain(primaryRole)
renderRounds()
updateSetupPreview()
showView()
